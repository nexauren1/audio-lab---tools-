package com.nexauren.audiolabtools.processing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

data class WavProcessSummary(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val frames: Long,
    val appliedGain: Double
)

/**
 * Streaming processor for uncompressed integer PCM WAV files.
 * Files are processed in bounded buffers and are never loaded fully into RAM.
 */
object WavPcmProcessor {
    private data class WavInfo(
        val channels: Int,
        val sampleRate: Int,
        val bitsPerSample: Int,
        val blockAlign: Int,
        val dataOffset: Long,
        val dataSize: Long
    )

    suspend fun process(
        source: File,
        destination: File,
        normalize: Boolean,
        gainDb: Double = 0.0,
        onProgress: suspend (Int) -> Unit
    ): WavProcessSummary = withContext(Dispatchers.IO) {
        require(source.isFile && source.length() > 44L) { "The source file is not a valid WAV file." }
        require(gainDb.isFinite() && gainDb in -60.0..24.0) { "Gain must be between -60 dB and +24 dB." }

        val info = readInfo(source)
        val bytesPerSample = info.bitsPerSample / 8
        require(info.dataSize % info.blockAlign == 0L) { "The WAV data chunk is incomplete." }
        require(info.dataSize <= 0xFFFF_FFFFL - 37L) { "This WAV file is too large for standard RIFF output." }

        val peak = if (normalize) scanPeak(source, info, bytesPerSample, onProgress) else 1.0
        val appliedGain = if (normalize) {
            if (peak <= 0.0) 1.0 else 0.98 / peak
        } else {
            10.0.pow(gainDb / 20.0)
        }
        require(appliedGain.isFinite() && appliedGain > 0.0) { "Unable to calculate a safe gain value." }

        destination.parentFile?.mkdirs()
        RandomAccessFile(source, "r").use { input ->
            RandomAccessFile(destination, "rw").use { output ->
                output.setLength(0L)
                writeHeader(output, info)
                input.seek(info.dataOffset)

                val bytesPerBlock = max(info.blockAlign, (64 * 1024 / info.blockAlign) * info.blockAlign)
                val buffer = ByteArray(bytesPerBlock)
                var remaining = info.dataSize
                var processed = 0L
                var lastProgress = -1

                while (remaining > 0L) {
                    currentCoroutineContext().ensureActive()
                    val count = minOf(buffer.size.toLong(), remaining).toInt()
                    input.readFully(buffer, 0, count)
                    var offset = 0
                    while (offset + bytesPerSample <= count) {
                        applyGain(buffer, offset, info.bitsPerSample, appliedGain)
                        offset += bytesPerSample
                    }
                    output.write(buffer, 0, count)
                    processed += count
                    remaining -= count
                    val progress = (20 + (processed * 75L / info.dataSize)).toInt().coerceAtMost(95)
                    if (progress != lastProgress) {
                        onProgress(progress)
                        lastProgress = progress
                    }
                }
                if (info.dataSize % 2L == 1L) output.write(0)
                output.fd.sync()
            }
        }

        onProgress(100)
        WavProcessSummary(
            sampleRate = info.sampleRate,
            channels = info.channels,
            bitsPerSample = info.bitsPerSample,
            frames = info.dataSize / info.blockAlign,
            appliedGain = appliedGain
        )
    }


    /**
     * Exports a time range from an integer-PCM WAV file without decoding the
     * entire file into memory. Boundaries are aligned to complete PCM frames.
     */
    suspend fun trim(
        source: File,
        destination: File,
        startMs: Long,
        endMs: Long,
        onProgress: suspend (Int) -> Unit
    ): WavProcessSummary = withContext(Dispatchers.IO) {
        require(source.isFile && source.length() > 44L) { "The source file is not a valid WAV file." }
        require(startMs >= 0L && endMs > startMs) { "The end time must be greater than the start time." }

        val info = readInfo(source)
        require(info.dataSize % info.blockAlign == 0L) { "The WAV data chunk is incomplete." }
        val totalFrames = info.dataSize / info.blockAlign
        val durationMs = totalFrames * 1000L / info.sampleRate
        require(endMs <= durationMs) { "The selected end time is beyond the audio duration." }

        val startFrame = (startMs * info.sampleRate / 1000L).coerceIn(0L, totalFrames)
        val endFrame = (endMs * info.sampleRate / 1000L).coerceIn(0L, totalFrames)
        require(endFrame > startFrame) { "The selected range is shorter than one audio frame." }

        val selectedFrames = endFrame - startFrame
        val selectedBytes = selectedFrames * info.blockAlign
        require(selectedBytes <= 0xFFFF_FFFFL - 37L) {
            "The selected range is too large for standard RIFF/WAV output."
        }

        destination.parentFile?.mkdirs()
        RandomAccessFile(source, "r").use { input ->
            RandomAccessFile(destination, "rw").use { output ->
                output.setLength(0L)
                writeHeader(output, info.copy(dataSize = selectedBytes))
                input.seek(info.dataOffset + startFrame * info.blockAlign)

                val bytesPerBlock = max(info.blockAlign, (64 * 1024 / info.blockAlign) * info.blockAlign)
                val buffer = ByteArray(bytesPerBlock)
                var remaining = selectedBytes
                var copied = 0L
                var lastProgress = -1

                onProgress(5)
                while (remaining > 0L) {
                    currentCoroutineContext().ensureActive()
                    val count = minOf(buffer.size.toLong(), remaining).toInt()
                    input.readFully(buffer, 0, count)
                    output.write(buffer, 0, count)
                    copied += count
                    remaining -= count
                    val progress = (5 + copied * 90L / selectedBytes).toInt().coerceAtMost(95)
                    if (progress != lastProgress) {
                        onProgress(progress)
                        lastProgress = progress
                    }
                }
                if (selectedBytes % 2L == 1L) output.write(0)
                output.fd.sync()
            }
        }

        onProgress(100)
        WavProcessSummary(
            sampleRate = info.sampleRate,
            channels = info.channels,
            bitsPerSample = info.bitsPerSample,
            frames = selectedFrames,
            appliedGain = 1.0
        )
    }


    /**
     * Splits integer-PCM WAV into equal, frame-aligned parts.
     * Audio is copied in small buffers; no full-file PCM allocation is made.
     */
    suspend fun splitIntoParts(
        source: File,
        outputFolder: File,
        baseName: String,
        partCount: Int,
        onProgress: suspend (Int) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        require(source.isFile && source.length() > 44L) { "The source file is not a valid WAV file." }
        require(partCount in 2..20) { "Choose between 2 and 20 parts." }

        val info = readInfo(source)
        require(info.dataSize % info.blockAlign == 0L) { "The WAV data chunk is incomplete." }
        val totalFrames = info.dataSize / info.blockAlign
        require(totalFrames >= partCount.toLong()) { "The audio is too short to create that many parts." }

        outputFolder.mkdirs()
        val safeBase = baseName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(48).ifBlank { "audio" }
        val outputs = mutableListOf<File>()

        try {
            RandomAccessFile(source, "r").use { input ->
                for (index in 0 until partCount) {
                    currentCoroutineContext().ensureActive()
                    val startFrame = totalFrames * index / partCount
                    val endFrame = totalFrames * (index + 1) / partCount
                    val partBytes = (endFrame - startFrame) * info.blockAlign
                    require(partBytes <= 0xFFFF_FFFFL - 37L) {
                        "A split part is too large for standard RIFF/WAV output."
                    }

                    val partFile = File(
                        outputFolder,
                        safeBase + "_part_" + (index + 1).toString().padStart(2, '0') + ".wav"
                    )
                    RandomAccessFile(partFile, "rw").use { output ->
                        output.setLength(0L)
                        writeHeader(output, info.copy(dataSize = partBytes))
                        input.seek(info.dataOffset + startFrame * info.blockAlign)

                        val bytesPerBlock = max(info.blockAlign, (64 * 1024 / info.blockAlign) * info.blockAlign)
                        val buffer = ByteArray(bytesPerBlock)
                        var remaining = partBytes
                        var copied = 0L
                        while (remaining > 0L) {
                            currentCoroutineContext().ensureActive()
                            val count = minOf(buffer.size.toLong(), remaining).toInt()
                            input.readFully(buffer, 0, count)
                            output.write(buffer, 0, count)
                            copied += count
                            remaining -= count
                            val partProgress = (copied * 100L / partBytes).toInt()
                            val overall = ((index * 100L + partProgress) / partCount).toInt()
                            onProgress(overall.coerceIn(0, 99))
                        }
                        if (partBytes % 2L == 1L) output.write(0)
                        output.fd.sync()
                    }
                    outputs += partFile
                }
            }
            onProgress(100)
            outputs
        } catch (error: Exception) {
            outputs.forEach(File::delete)
            throw error
        }
    }

    private suspend fun scanPeak(
        source: File,
        info: WavInfo,
        bytesPerSample: Int,
        onProgress: suspend (Int) -> Unit
    ): Double {
        var peak = 0.0
        RandomAccessFile(source, "r").use { input ->
            input.seek(info.dataOffset)
            val bytesPerBlock = max(info.blockAlign, (64 * 1024 / info.blockAlign) * info.blockAlign)
            val buffer = ByteArray(bytesPerBlock)
            var remaining = info.dataSize
            var scanned = 0L
            var lastProgress = -1

            while (remaining > 0L) {
                currentCoroutineContext().ensureActive()
                val count = minOf(buffer.size.toLong(), remaining).toInt()
                input.readFully(buffer, 0, count)
                var offset = 0
                while (offset + bytesPerSample <= count) {
                    peak = max(peak, abs(readSample(buffer, offset, info.bitsPerSample)))
                    offset += bytesPerSample
                }
                scanned += count
                remaining -= count
                val progress = (5 + scanned * 15L / info.dataSize).toInt().coerceAtMost(20)
                if (progress != lastProgress) {
                    onProgress(progress)
                    lastProgress = progress
                }
            }
        }
        return peak
    }

    private fun readInfo(file: File): WavInfo {
        RandomAccessFile(file, "r").use { input ->
            require(readAscii(input, 4) == "RIFF") { "Only little-endian RIFF/WAVE files are supported." }
            readUInt32(input)
            require(readAscii(input, 4) == "WAVE") { "The selected file is not a WAV file." }

            var channels = 0
            var sampleRate = 0
            var bitsPerSample = 0
            var blockAlign = 0
            var audioFormat = 0
            var dataOffset = -1L
            var dataSize = -1L

            while (input.filePointer + 8L <= input.length()) {
                val chunkId = readAscii(input, 4)
                val chunkSize = readUInt32(input)
                val chunkStart = input.filePointer
                require(chunkSize <= input.length() - chunkStart) { "A WAV chunk extends beyond the end of the file." }

                when (chunkId) {
                    "fmt " -> {
                        require(chunkSize in 16L..65536L) { "The WAV format header is invalid." }
                        val bytes = ByteArray(chunkSize.toInt())
                        input.readFully(bytes)
                        audioFormat = u16(bytes, 0)
                        channels = u16(bytes, 2)
                        sampleRate = u32(bytes, 4).toInt()
                        blockAlign = u16(bytes, 12)
                        bitsPerSample = u16(bytes, 14)
                        input.seek(chunkStart + chunkSize + (chunkSize % 2L))
                    }
                    "data" -> {
                        dataOffset = chunkStart
                        dataSize = chunkSize
                        break
                    }
                    else -> input.seek(chunkStart + chunkSize + (chunkSize % 2L))
                }
            }

            require(audioFormat == 1) { "Only integer PCM WAV is supported in this version. Floating-point WAV is not supported yet." }
            require(channels in 1..8) { "The WAV channel count is not supported." }
            require(sampleRate in 8000..384000) { "The WAV sample rate is outside the supported range." }
            require(bitsPerSample in setOf(8, 16, 24, 32)) { "Supported PCM depths are 8, 16, 24 and 32 bit." }
            require(blockAlign == channels * (bitsPerSample / 8)) { "The WAV block alignment is invalid." }
            require(dataOffset >= 0L && dataSize >= 0L) { "The WAV data chunk was not found." }
            require(dataOffset + dataSize <= file.length()) { "The WAV data is truncated." }

            return WavInfo(channels, sampleRate, bitsPerSample, blockAlign, dataOffset, dataSize)
        }
    }

    private fun writeHeader(output: RandomAccessFile, info: WavInfo) {
        val pad = info.dataSize % 2L
        output.writeBytes("RIFF")
        writeUInt32(output, 36L + info.dataSize + pad)
        output.writeBytes("WAVE")
        output.writeBytes("fmt ")
        writeUInt32(output, 16L)
        writeUInt16(output, 1)
        writeUInt16(output, info.channels)
        writeUInt32(output, info.sampleRate.toLong())
        writeUInt32(output, info.sampleRate.toLong() * info.blockAlign)
        writeUInt16(output, info.blockAlign)
        writeUInt16(output, info.bitsPerSample)
        output.writeBytes("data")
        writeUInt32(output, info.dataSize)
    }

    private fun readSample(bytes: ByteArray, offset: Int, bits: Int): Double = when (bits) {
        8 -> ((bytes[offset].toInt() and 0xFF) - 128) / 128.0
        16 -> {
            val raw = (bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)
            raw.toShort().toInt() / 32768.0
        }
        24 -> {
            var raw = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16)
            if ((raw and 0x800000) != 0) raw = raw or -0x1000000
            raw / 8388608.0
        }
        32 -> {
            val raw = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                (bytes[offset + 3].toInt() shl 24)
            raw / 2147483648.0
        }
        else -> error("Unsupported PCM depth.")
    }

    private fun applyGain(bytes: ByteArray, offset: Int, bits: Int, gain: Double) {
        val sample = readSample(bytes, offset, bits)
        when (bits) {
            8 -> {
                val value = (128.0 + sample * gain * 128.0).roundToLong().coerceIn(0L, 255L).toInt()
                bytes[offset] = value.toByte()
            }
            16 -> {
                val value = (sample * gain * 32768.0).roundToLong().coerceIn(-32768L, 32767L).toInt()
                bytes[offset] = value.toByte()
                bytes[offset + 1] = (value shr 8).toByte()
            }
            24 -> {
                val value = (sample * gain * 8388608.0).roundToLong().coerceIn(-8388608L, 8388607L).toInt()
                bytes[offset] = value.toByte()
                bytes[offset + 1] = (value shr 8).toByte()
                bytes[offset + 2] = (value shr 16).toByte()
            }
            32 -> {
                val value = (sample * gain * 2147483648.0).roundToLong()
                    .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
                bytes[offset] = value.toByte()
                bytes[offset + 1] = (value shr 8).toByte()
                bytes[offset + 2] = (value shr 16).toByte()
                bytes[offset + 3] = (value shr 24).toByte()
            }
        }
    }

    private fun readAscii(input: RandomAccessFile, length: Int): String {
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private fun readUInt32(input: RandomAccessFile): Long {
        val b0 = input.readUnsignedByte().toLong()
        val b1 = input.readUnsignedByte().toLong()
        val b2 = input.readUnsignedByte().toLong()
        val b3 = input.readUnsignedByte().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun writeUInt32(output: RandomAccessFile, value: Long) {
        output.write(value.toInt() and 0xFF)
        output.write((value shr 8).toInt() and 0xFF)
        output.write((value shr 16).toInt() and 0xFF)
        output.write((value shr 24).toInt() and 0xFF)
    }

    private fun writeUInt16(output: RandomAccessFile, value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun u32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFFL) or
            ((bytes[offset + 1].toLong() and 0xFFL) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFFL) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFFL) shl 24)
}
