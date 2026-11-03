package com.nexauren.audiolabtools.processing

import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WavPcmProcessorTest {
    @Test
    fun normalize16BitPcmKeepsValidWavAndTargetsPeak() = runBlocking {
        val folder = createTempDir(prefix = "audio-lab-test-")
        try {
            val input = File(folder, "input.wav")
            val output = File(folder, "normalized.wav")
            writePcm16Wav(input, shortArrayOf(1000, -4000, 12000, -8000, 2000))

            val summary = WavPcmProcessor.process(
                source = input,
                destination = output,
                normalize = true,
                onProgress = {}
            )

            RandomAccessFile(output, "r").use { wav ->
                val header = ByteArray(4)
                wav.readFully(header)
                assertEquals("RIFF", String(header, Charsets.US_ASCII))
                wav.seek(8)
                wav.readFully(header)
                assertEquals("WAVE", String(header, Charsets.US_ASCII))
                wav.seek(44)
                val samples = ShortArray(5) { wav.readShortLE() }
                val peak = samples.maxOf { abs(it.toInt()) } / 32768.0
                assertTrue("Normalized peak should be near -0.18 dBFS", peak in 0.979..0.981)
            }
            assertEquals(48000, summary.sampleRate)
            assertEquals(1, summary.channels)
            assertEquals(16, summary.bitsPerSample)
            assertEquals(5L, summary.frames)
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun gainDbIsAppliedToPcmSamples() = runBlocking {
        val folder = createTempDir(prefix = "audio-lab-test-")
        try {
            val input = File(folder, "input.wav")
            val output = File(folder, "gain.wav")
            writePcm16Wav(input, shortArrayOf(10000, -10000, 5000, -5000))

            val summary = WavPcmProcessor.process(
                source = input,
                destination = output,
                normalize = false,
                gainDb = -6.0,
                onProgress = {}
            )

            RandomAccessFile(output, "r").use { wav ->
                wav.seek(44)
                val first = wav.readShortLE().toInt()
                assertTrue(abs(first - 5012) < 8)
            }
            assertTrue(summary.appliedGain in 0.500..0.502)
        } finally {
            folder.deleteRecursively()
        }
    }


    @Test
    fun trimExportsOnlyRequestedFrames() = runBlocking {
        val folder = createTempDir(prefix = "audio-lab-test-")
        try {
            val input = File(folder, "input.wav")
            val output = File(folder, "trimmed.wav")
            writePcm16Wav(input, ShortArray(80) { it.toShort() }, sampleRate = 8000)

            val summary = WavPcmProcessor.trim(
                source = input,
                destination = output,
                startMs = 2L,
                endMs = 8L,
                onProgress = {}
            )

            RandomAccessFile(output, "r").use { wav ->
                wav.seek(44)
                assertEquals(16, wav.readShortLE().toInt())
                var last = 0
                repeat(47) { last = wav.readShortLE().toInt() }
                assertEquals(63, last)
            }
            assertEquals(48L, summary.frames)
            assertEquals(8000, summary.sampleRate)
            assertTrue(output.length() < input.length())
        } finally {
            folder.deleteRecursively()
        }
    }

    private fun writePcm16Wav(file: File, samples: ShortArray, sampleRate: Int = 48000) {
        RandomAccessFile(file, "rw").use { wav ->
            val dataSize = samples.size * 2
            wav.writeBytes("RIFF")
            wav.writeUInt32(36L + dataSize)
            wav.writeBytes("WAVEfmt ")
            wav.writeUInt32(16)
            wav.writeUInt16(1)
            wav.writeUInt16(1)
            wav.writeUInt32(sampleRate.toLong())
            wav.writeUInt32(sampleRate.toLong() * 2L)
            wav.writeUInt16(2)
            wav.writeUInt16(16)
            wav.writeBytes("data")
            wav.writeUInt32(dataSize.toLong())
            samples.forEach { wav.writeUInt16(it.toInt() and 0xFFFF) }
        }
    }

    private fun RandomAccessFile.readShortLE(): Short {
        val low = readUnsignedByte()
        val high = readUnsignedByte()
        return ((high shl 8) or low).toShort()
    }

    private fun RandomAccessFile.writeUInt16(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private fun RandomAccessFile.writeUInt32(value: Long) {
        write(value.toInt() and 0xFF)
        write((value shr 8).toInt() and 0xFF)
        write((value shr 16).toInt() and 0xFF)
        write((value shr 24).toInt() and 0xFF)
    }
}
