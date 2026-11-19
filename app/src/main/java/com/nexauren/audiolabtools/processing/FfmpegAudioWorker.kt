package com.nexauren.audiolabtools.processing

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class FfmpegAudioWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sourceUri = inputData.getString(ProcessingContract.KEY_INPUT_URI)?.let(Uri::parse)
            ?: return@withContext Result.failure(
                workDataOf(ProcessingContract.KEY_ERROR to "No audio file was selected.")
            )
        val toolId = inputData.getString(ProcessingContract.KEY_TOOL_ID).orEmpty()
        val sourceUris = inputData.getStringArray(ProcessingContract.KEY_INPUT_URIS)
            ?.mapNotNull(Uri::parse)
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(sourceUri)
        val sourceNames = inputData.getStringArray(ProcessingContract.KEY_INPUT_NAMES)
            ?.toList()
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(inputData.getString(ProcessingContract.KEY_SOURCE_NAME).orEmpty())
        val sourceName = inputData.getString(ProcessingContract.KEY_SOURCE_NAME)
            .orEmpty().ifBlank { sourceNames.firstOrNull().orEmpty() }
        if (toolId == "join-audio" && sourceUris.size !in 2..8) {
            return@withContext Result.failure(
                workDataOf(ProcessingContract.KEY_ERROR to "Choose between 2 and 8 audio files to join.")
            )
        }
        val sourceExtension = sourceNames.firstOrNull().orEmpty()
            .substringAfterLast('.', "audio")
            .lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }
            .ifBlank { "audio" }

        val cacheFolder = File(applicationContext.cacheDir, "audio-engine")
        val exportFolder = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: applicationContext.filesDir,
            "AudioLab"
        )
        cacheFolder.mkdirs()
        exportFolder.mkdirs()

        val sourceCopies = sourceUris.mapIndexed { index, _ ->
            val extension = sourceNames.getOrNull(index).orEmpty()
                .substringAfterLast('.', sourceExtension)
                .lowercase(Locale.ROOT)
                .filter { it.isLetterOrDigit() }
                .ifBlank { sourceExtension }
            File.createTempFile("source_${index}_", ".$extension", cacheFolder)
        }
        val sourceCopy = sourceCopies.first()
        var outputFile: File? = null

        try {
            if (!FfmpegAudioEngine.isSupportedOnThisDevice()) {
                throw IllegalStateException(
                    "The advanced audio engine currently requires a 64-bit ARM or x86_64 device. WAV PCM tools remain available on other devices."
                )
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 8,
                ProcessingContract.KEY_STAGE to "Importing audio file"
            ))
            sourceUris.forEachIndexed { index, uri ->
                copySource(uri, sourceCopies[index])
            }

            val outputFormat = inputData.getString(ProcessingContract.KEY_OUTPUT_FORMAT)
                ?.lowercase(Locale.ROOT)
                ?.takeIf { it in SUPPORTED_FORMATS }
                ?: when (toolId) {
                    "audio-compress" -> "m4a"
                    else -> sourceExtension.takeIf { it in SUPPORTED_FORMATS } ?: "m4a"
                }

            if (toolId == "compare-audio") {
                val summary = analyseAudio(sourceCopy)
                setProgress(workDataOf(
                    ProcessingContract.KEY_PROGRESS to 100,
                    ProcessingContract.KEY_STAGE to "Analysis complete"
                ))
                return@withContext Result.success(
                    workDataOf(
                        ProcessingContract.KEY_ANALYSIS_SUMMARY to summary,
                        ProcessingContract.KEY_SOURCE_NAME to sourceName,
                        ProcessingContract.KEY_SIZE to sourceCopy.length(),
                        ProcessingContract.KEY_PROGRESS to 100
                    )
                )
            }

            val outputExtension = when (toolId) {
                "audio-compress" -> "m4a"
                "convert-audio" -> outputFormat
                "join-audio" -> outputFormat
                else -> sourceExtension.takeIf { it in SUPPORTED_FORMATS }
                    ?: throw IllegalArgumentException(
                        "This operation needs an audio file in MP3, WAV, FLAC, AAC, M4A, OGG or OPUS format."
                    )
            }
            val safeBase = sourceName.substringBeforeLast('.', sourceName)
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(48)
                .ifBlank { "audio" }
            val suffix = when (toolId) {
                "normalize-wav" -> "_normalized"
                "amplify-wav" -> "_gain"
                "trim-audio" -> "_trimmed"
                "convert-audio" -> "_converted"
                "audio-compress" -> "_compressed"
                "remove-silence" -> "_cleaned"
                "dynamic-compressor" -> "_compressed_dynamics"
                "speed-audio" -> "_speed"
                "pitch-audio" -> "_pitch"
                "edit-metadata" -> "_tagged"
                "join-audio" -> "_joined"
                else -> "_processed"
            }
            outputFile = File(
                exportFolder,
                safeBase + suffix + "_" + System.currentTimeMillis() + "." + outputExtension
            )

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 22,
                ProcessingContract.KEY_STAGE to "Preparing audio processing"
            ))
            val arguments = if (toolId == "join-audio") {
                buildJoinArguments(sourceCopies, outputFile, outputFormat)
            } else {
                buildArguments(toolId, sourceCopy, outputFile, outputFormat)
            }
            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 48,
                ProcessingContract.KEY_STAGE to "Processing audio locally"
            ))

            val inputDurationMs = sourceCopies.sumOf { source ->
                currentCoroutineContext().ensureActive()
                FfmpegAudioEngine.durationMs(source.absolutePath)
            }
            val expectedOutputDurationMs = when (toolId) {
                "trim-audio" -> (inputData.getDouble(
                    ProcessingContract.KEY_DURATION_SECONDS, 15.0
                ) * 1000.0).toLong().coerceAtLeast(1L)
                "speed-audio" -> {
                    val speed = inputData.getDouble(ProcessingContract.KEY_SPEED, 1.25)
                    if (speed > 0.0) (inputDurationMs / speed).toLong() else inputDurationMs
                }
                else -> inputDurationMs
            }.coerceAtLeast(0L)

            val latestStatistics = AtomicLong(0L)
            val lastProgress = AtomicInteger(47)
            val progressScope = CoroutineScope(currentCoroutineContext())
            val progressJob = progressScope.launch {
                while (isActive) {
                    val processedMs = latestStatistics.get()
                    if (expectedOutputDurationMs > 0L && processedMs > 0L) {
                        val computed = (
                            48L + processedMs.coerceAtMost(expectedOutputDurationMs) * 39L /
                                expectedOutputDurationMs
                            ).toInt().coerceIn(48, 87)
                        while (true) {
                            val previous = lastProgress.get()
                            if (computed <= previous || lastProgress.compareAndSet(previous, computed)) {
                                if (computed > previous) {
                                    setProgress(workDataOf(
                                        ProcessingContract.KEY_PROGRESS to computed,
                                        ProcessingContract.KEY_STAGE to "Processing audio locally"
                                    ))
                                }
                                break
                            }
                        }
                    }
                    delay(350L)
                }
            }

            try {
                FfmpegAudioEngine.execute(arguments) { processedMs ->
                    latestStatistics.set(processedMs)
                }
            } finally {
                withContext(NonCancellable) { progressJob.cancelAndJoin() }
            }

            currentCoroutineContext().ensureActive()
            if (!outputFile.isFile || outputFile.length() <= 0L) {
                throw IllegalStateException("The audio engine did not create a valid output file.")
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 91,
                ProcessingContract.KEY_STAGE to "Saving processed audio"
            ))
            val outputSize = outputFile.length()
            val outputUri = publishOutput(applicationContext, outputFile, outputExtension)

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 100,
                ProcessingContract.KEY_STAGE to "Completed"
            ))
            Result.success(
                workDataOf(
                    ProcessingContract.KEY_OUTPUT_URI to outputUri.toString(),
                    ProcessingContract.KEY_OUTPUT_NAME to outputFile.name,
                    ProcessingContract.KEY_SIZE to outputSize,
                    ProcessingContract.KEY_PROGRESS to 100,
                    ProcessingContract.KEY_MIME to mimeFor(outputExtension)
                )
            )
        } catch (cancelled: CancellationException) {
            outputFile?.delete()
            throw cancelled
        } catch (error: Exception) {
            outputFile?.delete()
            Result.failure(
                workDataOf(
                    ProcessingContract.KEY_ERROR to (
                        error.message?.take(1200)
                            ?: "The selected audio file could not be processed."
                        )
                )
            )
        } finally {
            sourceCopies.forEach(File::delete)
        }
    }

    private fun buildJoinArguments(
        sources: List<File>,
        output: File,
        outputFormat: String
    ): List<String> {
        require(sources.size in 2..8) { "Choose between 2 and 8 audio files to join." }
        val args = mutableListOf("-hide_banner", "-nostdin", "-y")
        sources.forEach { source ->
            args += listOf("-i", source.absolutePath)
        }
        val filters = buildString {
            sources.indices.forEach { index ->
                append("[").append(index).append(":a:0]")
                append("aresample=44100,aformat=sample_fmts=fltp:channel_layouts=stereo")
                append("[a").append(index).append("];")
            }
            sources.indices.forEach { index ->
                append("[a").append(index).append("]")
            }
            append("concat=n=").append(sources.size)
                .append(":v=0:a=1[outa]")
        }
        args += listOf("-filter_complex", filters, "-map", "[outa]", "-vn")
        args += codecOptions(outputFormat, "192k")
        args += output.absolutePath
        return args
    }

    private suspend fun copySource(uri: Uri, destination: File) {
        val source = applicationContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Android could not open the selected audio file.")
        source.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
        require(destination.length() > 0L) { "The selected audio file is empty." }
    }

    private fun buildArguments(
        toolId: String,
        source: File,
        output: File,
        outputFormat: String
    ): List<String> {
        val args = mutableListOf("-hide_banner", "-nostdin", "-y")

        if (toolId == "trim-audio") {
            val start = inputData.getDouble(ProcessingContract.KEY_START_SECONDS, 0.0)
            val duration = inputData.getDouble(ProcessingContract.KEY_DURATION_SECONDS, 15.0)
            require(start.isFinite() && start >= 0.0) { "Start time must be zero or greater." }
            require(duration.isFinite() && duration > 0.0 && duration <= 21600.0) {
                "Trim duration must be between 0 and 6 hours."
            }
            args += listOf("-ss", decimal(start))
        }

        args += listOf("-i", source.absolutePath, "-map", "0:a:0", "-vn")

        when (toolId) {
            "convert-audio" -> {
                val bitrate = safeBitrate(inputData.getString(ProcessingContract.KEY_TARGET_BITRATE), "192k")
                args += codecOptions(outputFormat, bitrate)
            }
            "audio-compress" -> {
                val bitrate = safeBitrate(inputData.getString(ProcessingContract.KEY_TARGET_BITRATE), "128k")
                args += listOf("-c:a", "aac", "-b:a", bitrate, "-movflags", "+faststart")
            }
            "trim-audio" -> {
                val duration = inputData.getDouble(ProcessingContract.KEY_DURATION_SECONDS, 15.0)
                args += listOf("-t", decimal(duration))
                args += codecOptions(output.extension.lowercase(Locale.ROOT), "192k")
            }
            "normalize-wav" -> {
                args += listOf("-af", "loudnorm=I=-16:TP=-1.5:LRA=11")
                args += codecOptions(output.extension.lowercase(Locale.ROOT), "192k")
            }
            "amplify-wav" -> {
                val gain = inputData.getDouble(ProcessingContract.KEY_GAIN_DB, 3.0)
                require(gain.isFinite() && gain in -24.0..18.0) {
                    "Gain must be between -24 dB and +18 dB."
                }
                args += listOf("-af", "volume=${decimal(gain)}dB")
                args += codecOptions(output.extension.lowercase(Locale.ROOT), "192k")
            }
            "remove-silence" -> {
                val reduction = inputData.getDouble(ProcessingContract.KEY_NOISE_REDUCTION, 12.0)
                    .coerceIn(0.01, 40.0)
                val floor = inputData.getDouble(ProcessingContract.KEY_NOISE_FLOOR_DB, -50.0)
                    .coerceIn(-80.0, -20.0)
                args += listOf("-af", "afftdn=nr=${decimal(reduction)}:nf=${decimal(floor)}:tn=1")
                args += codecOptions(output.extension.lowercase(Locale.ROOT), "192k")
            }
            "dynamic-compressor" -> {
                val ratio = inputData.getDouble(ProcessingContract.KEY_COMPRESSOR_RATIO, 3.0)
                    .coerceIn(1.0, 12.0)
                val thresholdDb = inputData.getDouble(
                    ProcessingContract.KEY_COMPRESSOR_THRESHOLD_DB, -18.0
                ).coerceIn(-36.0, -6.0)
                val threshold = Math.pow(10.0, thresholdDb / 20.0)
                args += listOf(
                    "-af",
                    "acompressor=threshold=${decimal(threshold)}:ratio=${decimal(ratio)}:attack=20:release=250:makeup=1.2:knee=2.8:mix=1"
                )
                args += codecOptions(output.extension.lowercase(Locale.ROOT), "192k")
            }
            "speed-audio" -> {
                val speed = inputData.getDouble(ProcessingContract.KEY_SPEED, 1.25)
                require(speed.isFinite() && speed in 0.5..2.0) {
                    "Speed must be between 0.5x and 2.0x."
                }
                args += listOf("-af", "atempo=${decimal(speed)}")
                args += codecOptions(output.extension.lowercase(Locale.ROOT), "192k")
            }
            "pitch-audio" -> {
                val semitones = inputData.getDouble(ProcessingContract.KEY_PITCH, 2.0)
                require(semitones.isFinite() && semitones in -6.0..6.0) {
                    "Pitch must be between -6 and +6 semitones."
                }
                val probe = FfmpegAudioEngine.probe(source.absolutePath)
                val sampleRate = probe.optJSONArray("streams")
                    ?.optJSONObject(0)?.optInt("sample_rate", 44100) ?: 44100
                val factor = Math.pow(2.0, semitones / 12.0)
                val returnTempo = 1.0 / factor
                val filter = "asetrate=${decimal(sampleRate * factor)},aresample=$sampleRate,atempo=${decimal(returnTempo)}"
                args += listOf("-af", filter)
                args += codecOptions(output.extension.lowercase(Locale.ROOT), "192k")
            }
            "edit-metadata" -> {
                val title = inputData.getString(ProcessingContract.KEY_METADATA_TITLE)
                    .orEmpty().filterNot { it == '\n' || it == '\r' }.take(120)
                val artist = inputData.getString(ProcessingContract.KEY_METADATA_ARTIST)
                    .orEmpty().filterNot { it == '\n' || it == '\r' }.take(120)
                args += listOf("-c:a", "copy")
                if (title.isNotBlank()) args += listOf("-metadata", "title=$title")
                if (artist.isNotBlank()) args += listOf("-metadata", "artist=$artist")
            }
            else -> throw IllegalArgumentException("This audio operation is not available yet.")
        }

        args += output.absolutePath
        return args
    }

    private suspend fun analyseAudio(source: File): String {
        val probe = FfmpegAudioEngine.probe(source.absolutePath)
        val format = probe.optJSONObject("format") ?: JSONObject()
        val stream = probe.optJSONArray("streams")?.optJSONObject(0) ?: JSONObject()
        val formatName = format.optString("format_name", "Unknown")
        val codec = stream.optString("codec_name", "Unknown")
        val sampleRate = stream.optString("sample_rate", "Unknown")
        val channels = stream.optInt("channels", 0)
        val duration = format.optDouble("duration", 0.0)
        val size = format.optLong("size", source.length())
        val bitrate = format.optLong("bit_rate", 0L)

        val stats = FfmpegAudioEngine.execute(
            listOf(
                "-hide_banner", "-nostats", "-i", source.absolutePath,
                "-map", "0:a:0", "-af", "astats=metadata=1:reset=0",
                "-f", "null", "-"
            )
        )
        val spectral = try {
            FfmpegAudioEngine.execute(
                listOf(
                    "-hide_banner", "-nostats", "-t", "30", "-i", source.absolutePath,
                    "-map", "0:a:0",
                    "-af", "aspectralstats=measure=centroid+spread+entropy+flatness+rolloff:win_size=2048,ametadata=mode=print:file=-",
                    "-f", "null", "-"
                )
            )
        } catch (_: Exception) {
            ""
        }
        val centroid = averageSpectralMetric(spectral, "centroid")
        val rolloff = averageSpectralMetric(spectral, "rolloff")
        val flatness = averageSpectralMetric(spectral, "flatness")

        val summary = buildString {
            append("Format: ").append(formatName)
            append("\nCodec: ").append(codec)
            append("\nDuration: ").append(String.format(Locale.US, "%.2f s", duration))
            append("\nSample rate: ").append(sampleRate).append(" Hz")
            append("\nChannels: ").append(channels)
            append("\nSize: ").append(formatBytes(size))
            if (bitrate > 0L) append("\nBitrate: ").append(bitrate / 1000L).append(" kbps")
            val peak = Regex("Peak level dB:\\s*(-?[0-9.]+)", RegexOption.IGNORE_CASE)
                .find(stats)?.groupValues?.getOrNull(1)
            val rms = Regex("RMS level dB:\\s*(-?[0-9.]+)", RegexOption.IGNORE_CASE)
                .find(stats)?.groupValues?.getOrNull(1)
            if (peak != null) append("\nPeak level: ").append(peak).append(" dB")
            if (rms != null) append("\nRMS level: ").append(rms).append(" dB")
            if (centroid != null) append("\nSpectral centroid: ").append(String.format(Locale.US, "%.1f Hz", centroid))
            if (rolloff != null) append("\nSpectral rolloff: ").append(String.format(Locale.US, "%.1f Hz", rolloff))
            if (flatness != null) append("\nSpectral flatness: ").append(String.format(Locale.US, "%.4f", flatness))
        }
        return summary
    }

    private fun averageSpectralMetric(output: String, metric: String): Double? {
        val regex = Regex(
            "lavfi\\.aspectralstats\\.\\d+\\." + Regex.escape(metric) + "=([0-9.eE+-]+)",
            RegexOption.IGNORE_CASE
        )
        val values = regex.findAll(output)
            .mapNotNull { it.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .filter { it.isFinite() }
            .take(2000)
            .toList()
        return values.takeIf { it.isNotEmpty() }?.average()
    }

    private fun safeBitrate(value: String?, fallback: String): String {
        val parsed = value?.trim()?.lowercase(Locale.ROOT)
            ?.removeSuffix("k")
            ?.toIntOrNull()
            ?: return fallback
        return parsed.coerceIn(32, 320).toString() + "k"
    }

    private fun codecOptions(format: String, bitrate: String): List<String> = when (format) {
        "mp3" -> listOf("-c:a", "libmp3lame", "-b:a", bitrate)
        "wav" -> listOf("-c:a", "pcm_s16le")
        "flac" -> listOf("-c:a", "flac")
        "aac" -> listOf("-c:a", "aac", "-b:a", bitrate, "-f", "adts")
        "m4a" -> listOf("-c:a", "aac", "-b:a", bitrate, "-movflags", "+faststart")
        "ogg" -> listOf("-c:a", "libvorbis", "-q:a", "4")
        "opus" -> listOf("-c:a", "libopus", "-b:a", bitrate)
        else -> throw IllegalArgumentException("Choose MP3, WAV, FLAC, AAC, M4A, OGG or OPUS.")
    }

    private fun publishOutput(context: Context, file: File, extension: String): Uri {
        val mime = mimeFor(extension)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + "/Audio Lab"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IllegalStateException("Android could not create the output file.")
            try {
                context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IllegalStateException("Android could not write the output file.")
                val completed = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, completed, null, null)
                file.delete()
                return uri
            } catch (error: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
        }
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
    }

    private fun mimeFor(extension: String): String = when (extension) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        else -> "audio/*"
    }

    private fun decimal(value: Double): String =
        String.format(Locale.US, "%.5f", value).trimEnd('0').trimEnd('.')

    private fun formatBytes(size: Long): String {
        if (size < 1024L) return "$size B"
        val kb = size / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        return String.format(Locale.US, "%.1f MB", kb / 1024.0)
    }

    companion object {
        private val SUPPORTED_FORMATS = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus")
    }
}
