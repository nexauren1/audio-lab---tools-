package com.nexauren.audiolabtools.processing

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioInspectionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uri = inputData.getString(ProcessingContract.KEY_INPUT_URI)?.let(Uri::parse)
            ?: return@withContext Result.failure(
                workDataOf(ProcessingContract.KEY_ERROR to "No audio file was selected.")
            )
        val temp = File.createTempFile("inspect_", ".audio", applicationContext.cacheDir)

        try {
            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 8,
                ProcessingContract.KEY_STAGE to "Importing audio file"
            ))
            val details = queryFileDetails(applicationContext, uri)
            copyUriToFile(uri, temp)

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 35,
                ProcessingContract.KEY_STAGE to "Reading audio properties"
            ))

            val mime = applicationContext.contentResolver.getType(uri)
                ?: mimeForName(details.first)
            val result = if (FfmpegAudioEngine.isSupportedOnThisDevice()) {
                try {
                    inspectWithFfprobe(temp, details.first, details.second, mime)
                } catch (_: Exception) {
                    inspectWithAndroid(uri, details.first, details.second, mime)
                }
            } else {
                inspectWithAndroid(uri, details.first, details.second, mime)
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 100,
                ProcessingContract.KEY_STAGE to "Analysis complete"
            ))
            Result.success(result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(
                workDataOf(
                    ProcessingContract.KEY_ERROR to
                        (error.message ?: "The selected file could not be inspected.")
                )
            )
        } finally {
            temp.delete()
        }
    }

    private fun inspectWithFfprobe(
        file: File,
        displayName: String,
        fileSize: Long,
        mime: String
    ): androidx.work.Data {
        val probe = FfmpegAudioEngine.probe(file.absolutePath)
        val format = probe.optJSONObject("format")
            ?: throw IllegalArgumentException("The audio format could not be identified.")
        val stream = probe.optJSONArray("streams")?.optJSONObject(0)
            ?: throw IllegalArgumentException("No audio stream was found in this file.")

        val formatName = format.optString("format_name", "Unknown")
        val codec = stream.optString("codec_name", "Unknown")
        val sampleRate = stream.optString("sample_rate").toIntOrNull() ?: 0
        val channels = stream.optInt("channels", 0)
        val bitDepth = stream.optInt("bits_per_sample", 0)
            .takeIf { it > 0 }
            ?: stream.optInt("bits_per_raw_sample", 0)
        val durationMs = (format.optDouble("duration", 0.0) * 1000.0).toLong()
            .coerceAtLeast(0L)
        val bitrate = format.optLong("bit_rate", 0L)

        val summary = buildString {
            append("Format: ").append(formatName)
            append("\nCodec: ").append(codec)
            if (sampleRate > 0) append("\nSample rate: ").append(sampleRate).append(" Hz")
            if (channels > 0) append("\nChannels: ").append(channels)
            if (bitDepth > 0) append("\nBit depth: ").append(bitDepth).append(" bit")
            if (durationMs > 0L) append("\nDuration: ").append(formatDuration(durationMs))
            if (bitrate > 0L) append("\nBitrate: ").append(bitrate / 1000L).append(" kbps")
        }

        return workDataOf(
            ProcessingContract.KEY_SOURCE_NAME to displayName,
            ProcessingContract.KEY_SIZE to fileSize,
            ProcessingContract.KEY_MIME to mime,
            ProcessingContract.KEY_DURATION to durationMs,
            ProcessingContract.KEY_BITRATE to bitrate,
            ProcessingContract.KEY_SAMPLE_RATE to sampleRate,
            ProcessingContract.KEY_CHANNELS to channels,
            ProcessingContract.KEY_BITS to bitDepth,
            ProcessingContract.KEY_ANALYSIS_SUMMARY to summary
        )
    }

    private fun inspectWithAndroid(
        uri: Uri,
        displayName: String,
        fileSize: Long,
        mime: String
    ): androidx.work.Data {
        val retriever = MediaMetadataRetriever()
        val metadata = try {
            retriever.setDataSource(applicationContext, uri)
            listOf(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
            )
        } finally {
            retriever.release()
        }

        val duration = metadata[0]?.toLongOrNull() ?: 0L
        val bitrate = metadata[1]?.toLongOrNull() ?: 0L
        val sampleRate = metadata[2]?.toIntOrNull() ?: 0
        val tracks = metadata[3]?.toIntOrNull() ?: 0
        val summary = buildString {
            append("Format: ").append(mime)
            if (duration > 0L) append("\nDuration: ").append(formatDuration(duration))
            if (bitrate > 0L) append("\nBitrate: ").append(bitrate / 1000L).append(" kbps")
            if (sampleRate > 0) append("\nSample rate: ").append(sampleRate).append(" Hz")
            if (tracks > 0) append("\nTracks: ").append(tracks)
            append("\nAnalysis: Android media decoder")
        }

        return workDataOf(
            ProcessingContract.KEY_SOURCE_NAME to displayName,
            ProcessingContract.KEY_SIZE to fileSize,
            ProcessingContract.KEY_MIME to mime,
            ProcessingContract.KEY_DURATION to duration,
            ProcessingContract.KEY_BITRATE to bitrate,
            ProcessingContract.KEY_SAMPLE_RATE to sampleRate,
            ProcessingContract.KEY_ANALYSIS_SUMMARY to summary
        )
    }

    private suspend fun copyUriToFile(uri: Uri, destination: File) {
        val source = applicationContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Android could not open the selected audio file.")
        source.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(96 * 1024)
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

    private fun queryFileDetails(context: Context, uri: Uri): Pair<String, Long> {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Audio file"
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                return (name ?: "Audio file") to size
            }
        }
        return "Audio file" to 0L
    }

    private fun mimeForName(name: String): String = when (
        name.substringAfterLast('.', "").lowercase()
    ) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        else -> "audio/*"
    }

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000L
        return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }
}

class WavProcessingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString(ProcessingContract.KEY_INPUT_URI)?.let(Uri::parse)
            ?: return Result.failure(workDataOf(ProcessingContract.KEY_ERROR to "No audio file was selected."))

        val normalize = inputData.getBoolean(ProcessingContract.KEY_NORMALIZE, false)
        val gainDb = inputData.getDouble(ProcessingContract.KEY_GAIN_DB, 0.0)
        val sourceName = inputData.getString(ProcessingContract.KEY_SOURCE_NAME) ?: "audio.wav"
        val safeBase = sourceName.substringBeforeLast('.', sourceName)
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(48)
            .ifBlank { "audio" }

        val cacheFolder = File(applicationContext.cacheDir, "audio")
        val exportFolder = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: applicationContext.filesDir,
            "AudioLab"
        )
        cacheFolder.mkdirs()
        exportFolder.mkdirs()

        val sourceCopy = File.createTempFile("source_", ".wav", cacheFolder)
        val suffix = if (normalize) "_normalized" else "_gain"
        val outputFile = File(exportFolder, safeBase + suffix + "_" + System.currentTimeMillis() + ".wav")

        return try {
            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 2,
                ProcessingContract.KEY_STAGE to "Importing source file"
            ))

            withContext(Dispatchers.IO) {
                val source = applicationContext.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Android could not open this audio file.")
                source.use { input ->
                    FileOutputStream(sourceCopy).use { output ->
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
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 10,
                ProcessingContract.KEY_STAGE to "Checking WAV format"
            ))

            val summary = WavPcmProcessor.process(
                source = sourceCopy,
                destination = outputFile,
                normalize = normalize,
                gainDb = gainDb
            ) { progress ->
                setProgress(workDataOf(
                    ProcessingContract.KEY_PROGRESS to progress,
                    ProcessingContract.KEY_STAGE to if (progress < 25) "Analysing audio peak" else "Processing PCM samples"
                ))
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 97,
                ProcessingContract.KEY_STAGE to "Saving result"
            ))
            val outputSize = outputFile.length()
            val outputUri = withContext(Dispatchers.IO) {
                publishOutput(applicationContext, outputFile)
            }

            Result.success(
                workDataOf(
                    ProcessingContract.KEY_OUTPUT_URI to outputUri.toString(),
                    ProcessingContract.KEY_OUTPUT_NAME to outputFile.name,
                    ProcessingContract.KEY_SIZE to outputSize,
                    ProcessingContract.KEY_SAMPLE_RATE to summary.sampleRate,
                    ProcessingContract.KEY_CHANNELS to summary.channels,
                    ProcessingContract.KEY_BITS to summary.bitsPerSample,
                    ProcessingContract.KEY_APPLIED_GAIN to summary.appliedGain,
                    ProcessingContract.KEY_PROGRESS to 100
                )
            )
        } catch (cancelled: CancellationException) {
            outputFile.delete()
            throw cancelled
        } catch (error: Exception) {
            outputFile.delete()
            Result.failure(
                workDataOf(ProcessingContract.KEY_ERROR to
                    (error.message ?: "Audio processing failed."))
            )
        } finally {
            sourceCopy.delete()
        }
    }

    private fun publishOutput(context: Context, file: File): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
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
}
