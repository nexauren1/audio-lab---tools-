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
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AudioInspectionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uri = inputData.getString(ProcessingContract.KEY_INPUT_URI)?.let(Uri::parse)
            ?: return@withContext Result.failure(
                workDataOf(ProcessingContract.KEY_ERROR to "No audio file was selected.")
            )

        try {
            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 15,
                ProcessingContract.KEY_STAGE to "Reading file details"
            ))

            val details = queryFileDetails(applicationContext, uri)
            val retriever = MediaMetadataRetriever()
            val metadata = try {
                retriever.setDataSource(applicationContext, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                val tracks = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
                listOf(duration, bitrate, sampleRate, tracks)
            } finally {
                retriever.release()
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 100,
                ProcessingContract.KEY_STAGE to "Analysis complete"
            ))

            Result.success(
                workDataOf(
                    ProcessingContract.KEY_SOURCE_NAME to details.first,
                    ProcessingContract.KEY_SIZE to details.second,
                    ProcessingContract.KEY_MIME to (applicationContext.contentResolver.getType(uri) ?: "Unknown"),
                    ProcessingContract.KEY_DURATION to (metadata[0]?.toLongOrNull() ?: 0L),
                    ProcessingContract.KEY_BITRATE to (metadata[1]?.toLongOrNull() ?: 0L),
                    ProcessingContract.KEY_SAMPLE_RATE to (metadata[2]?.toIntOrNull() ?: 0),
                    "track_count" to (metadata[3]?.toIntOrNull() ?: 0)
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(
                workDataOf(ProcessingContract.KEY_ERROR to
                    (error.message ?: "The selected file could not be inspected."))
            )
        }
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
}

class WavProcessingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val toolId = inputData.getString(ProcessingContract.KEY_TOOL_ID).orEmpty()
        val sourceUris = if (toolId == "join-audio") {
            inputData.getStringArray(ProcessingContract.KEY_INPUT_URIS)
                ?.map { Uri.parse(it) }.orEmpty()
        } else {
            inputData.getString(ProcessingContract.KEY_INPUT_URI)
                ?.let { listOf(Uri.parse(it)) }.orEmpty()
        }
        if (sourceUris.isEmpty()) {
            return Result.failure(workDataOf(ProcessingContract.KEY_ERROR to "No audio file was selected."))
        }

        val normalize = inputData.getBoolean(ProcessingContract.KEY_NORMALIZE, false)
        val gainDb = inputData.getDouble(ProcessingContract.KEY_GAIN_DB, 0.0)
        val startMs = inputData.getLong(ProcessingContract.KEY_START_MS, 0L)
        val endMs = inputData.getLong(ProcessingContract.KEY_END_MS, 0L)
        val fadeDurationMs = inputData.getLong(ProcessingContract.KEY_FADE_MS, 1000L)
        val splitParts = inputData.getInt(ProcessingContract.KEY_SPLIT_PARTS, 2)
        val isArchive = toolId == "split-audio"
        val sourceName = inputData.getString(ProcessingContract.KEY_SOURCE_NAME) ?: "audio.wav"
        val safeBase = sourceName.substringBeforeLast('.', sourceName)
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(48)
            .ifBlank { "audio" }

        val cacheFolder = File(applicationContext.cacheDir, "audio")
        val exportFolder = File(
            applicationContext.getExternalFilesDir(
                if (isArchive) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_MUSIC
            ) ?: applicationContext.filesDir,
            "AudioLab"
        )
        cacheFolder.mkdirs()
        exportFolder.mkdirs()

        val sourceCopies = mutableListOf<File>()
        val suffix = when {
            toolId == "trim-audio" -> "_trimmed"
            toolId == "split-audio" -> "_parts"
            toolId == "join-audio" -> "_joined"
            toolId == "reverse-audio" -> "_reversed"
            toolId == "fade-in" -> "_fade_in"
            toolId == "fade-out" -> "_fade_out"
            normalize -> "_normalized"
            else -> "_gain"
        }
        val outputFile = File(
            exportFolder,
            safeBase + suffix + "_" + System.currentTimeMillis() + if (isArchive) ".zip" else ".wav"
        )
        val splitFolder = File(cacheFolder, "split_" + System.currentTimeMillis())

        return try {
            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 2,
                ProcessingContract.KEY_STAGE to "Importing source file"
            ))

            withContext(Dispatchers.IO) {
                val buffer = ByteArray(128 * 1024)
                sourceUris.forEachIndexed { index, sourceUri ->
                    currentCoroutineContext().ensureActive()
                    val sourceCopy = File.createTempFile("source_", ".wav", cacheFolder)
                    sourceCopies += sourceCopy
                    val source = applicationContext.contentResolver.openInputStream(sourceUri)
                        ?: throw IllegalArgumentException("Android could not open selected audio file ${index + 1}.")
                    source.use { input ->
                        FileOutputStream(sourceCopy).use { output ->
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
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 10,
                ProcessingContract.KEY_STAGE to "Checking WAV format"
            ))

            val reportProgress: suspend (Int) -> Unit = { progress ->
                val visibleProgress = (10 + progress * 0.85).toInt().coerceIn(10, 95)
                setProgress(workDataOf(
                    ProcessingContract.KEY_PROGRESS to visibleProgress,
                    ProcessingContract.KEY_STAGE to when {
                        progress < 10 -> "Preparing audio"
                        progress < 96 && toolId == "trim-audio" -> "Extracting selected audio range"
                        toolId == "join-audio" -> "Joining WAV files"
                        toolId == "reverse-audio" -> "Reversing audio frames"
                        toolId == "fade-in" -> "Applying fade in"
                        toolId == "fade-out" -> "Applying fade out"
                        progress < 25 -> "Analysing audio peak"
                        else -> "Processing PCM samples"
                    }
                ))
            }
            val summary = when {
                toolId == "trim-audio" -> WavPcmProcessor.trim(
                    source = sourceCopies.first(),
                    destination = outputFile,
                    startMs = startMs,
                    endMs = endMs,
                    onProgress = reportProgress
                )
                toolId == "join-audio" -> WavPcmProcessor.join(
                    sources = sourceCopies,
                    destination = outputFile,
                    onProgress = reportProgress
                )
                toolId == "reverse-audio" -> WavPcmProcessor.reverse(
                    source = sourceCopies.first(),
                    destination = outputFile,
                    onProgress = reportProgress
                )
                toolId == "fade-in" || toolId == "fade-out" -> WavPcmProcessor.fade(
                    source = sourceCopies.first(),
                    destination = outputFile,
                    fadeIn = toolId == "fade-in",
                    fadeDurationMs = fadeDurationMs,
                    onProgress = reportProgress
                )
                isArchive -> {
                    val parts = WavPcmProcessor.splitIntoParts(
                        source = sourceCopies.first(),
                        outputFolder = splitFolder,
                        baseName = safeBase,
                        partCount = splitParts
                    ) { progress ->
                        setProgress(workDataOf(
                            ProcessingContract.KEY_PROGRESS to (10 + progress * 75 / 100).coerceAtMost(85),
                            ProcessingContract.KEY_STAGE to "Splitting WAV into part " +
                                ((progress * splitParts / 100) + 1).coerceAtMost(splitParts) +
                                " of " + splitParts
                        ))
                    }
                    withContext(Dispatchers.IO) {
                        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zip ->
                            val buffer = ByteArray(64 * 1024)
                            parts.forEach { part ->
                                currentCoroutineContext().ensureActive()
                                zip.putNextEntry(ZipEntry(part.name))
                                part.inputStream().use { input ->
                                    while (true) {
                                        currentCoroutineContext().ensureActive()
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        zip.write(buffer, 0, count)
                                    }
                                }
                                zip.closeEntry()
                            }
                        }
                    }
                    parts.forEach(File::delete)
                    null
                }
                else -> WavPcmProcessor.process(
                    source = sourceCopies.first(),
                    destination = outputFile,
                    normalize = normalize,
                    gainDb = gainDb,
                    onProgress = reportProgress
                )
            }

            setProgress(workDataOf(
                ProcessingContract.KEY_PROGRESS to 97,
                ProcessingContract.KEY_STAGE to "Saving result"
            ))
            val outputSize = outputFile.length()
            val outputMime = if (isArchive) "application/zip" else "audio/wav"
            val outputUri = withContext(Dispatchers.IO) {
                publishOutput(applicationContext, outputFile, outputMime, isArchive)
            }

            Result.success(
                workDataOf(
                    ProcessingContract.KEY_OUTPUT_URI to outputUri.toString(),
                    ProcessingContract.KEY_OUTPUT_NAME to outputFile.name,
                    ProcessingContract.KEY_OUTPUT_MIME to outputMime,
                    ProcessingContract.KEY_SIZE to outputSize,
                    ProcessingContract.KEY_SAMPLE_RATE to (summary?.sampleRate ?: 0),
                    ProcessingContract.KEY_CHANNELS to (summary?.channels ?: 0),
                    ProcessingContract.KEY_BITS to (summary?.bitsPerSample ?: 0),
                    ProcessingContract.KEY_APPLIED_GAIN to (summary?.appliedGain ?: 1.0),
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
            sourceCopies.forEach { it.delete() }
            splitFolder.deleteRecursively()
        }
    }

    private fun publishOutput(
        context: Context,
        file: File,
        mimeType: String,
        isArchive: Boolean
    ): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    (if (isArchive) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_MUSIC) +
                        "/Audio Lab"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = if (isArchive) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val uri = context.contentResolver.insert(
                collection,
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
