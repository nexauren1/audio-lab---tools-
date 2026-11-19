package com.nexauren.audiolabtools.processing

import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.security.MessageDigest

object AudioWorkScheduler {
    fun enqueueInspection(
        context: Context,
        uri: Uri,
        sourceName: String
    ): String {
        val toolId = "audio-info"
        val uniqueName = uniqueName(toolId, uri)
        val request = OneTimeWorkRequestBuilder<AudioInspectionWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uri.toString(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId
                )
            )
            .addTag(ProcessingContract.TAG_PROCESSING)
            .addTag(ProcessingContract.toolTag(toolId))
            .addTag(ProcessingContract.sourceTag(sourceName))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request
        )
        return uniqueName
    }

    fun enqueueWavOperation(
        context: Context,
        uri: Uri,
        sourceName: String,
        toolId: String,
        normalize: Boolean,
        gainDb: Double
    ): String {
        val uniqueName = uniqueName(toolId, uri)
        val request = OneTimeWorkRequestBuilder<WavProcessingWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uri.toString(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId,
                    ProcessingContract.KEY_NORMALIZE to normalize,
                    ProcessingContract.KEY_GAIN_DB to gainDb
                )
            )
            .addTag(ProcessingContract.TAG_PROCESSING)
            .addTag(ProcessingContract.toolTag(toolId))
            .addTag(ProcessingContract.sourceTag(sourceName))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request
        )
        return uniqueName
    }

    fun enqueueFfmpegOperation(
        context: Context,
        uri: Uri,
        sourceName: String,
        toolId: String,
        outputFormat: String = "",
        targetBitrate: String = "",
        startSeconds: Double = 0.0,
        durationSeconds: Double = 15.0,
        gainDb: Double = 3.0,
        noiseReductionDb: Double = 12.0,
        noiseFloorDb: Double = -50.0,
        compressorRatio: Double = 3.0,
        compressorThresholdDb: Double = -18.0,
        speed: Double = 1.25,
        pitch: Double = 2.0,
        title: String = "",
        artist: String = ""
    ): String {
        val uniqueName = uniqueName(toolId, uri)
        val input = workDataOf(
            ProcessingContract.KEY_INPUT_URI to uri.toString(),
            ProcessingContract.KEY_SOURCE_NAME to sourceName,
            ProcessingContract.KEY_TOOL_ID to toolId,
            ProcessingContract.KEY_OUTPUT_FORMAT to outputFormat,
            ProcessingContract.KEY_TARGET_BITRATE to targetBitrate,
            ProcessingContract.KEY_START_SECONDS to startSeconds,
            ProcessingContract.KEY_DURATION_SECONDS to durationSeconds,
            ProcessingContract.KEY_GAIN_DB to gainDb,
            ProcessingContract.KEY_NOISE_REDUCTION to noiseReductionDb,
            ProcessingContract.KEY_COMPRESSOR_RATIO to compressorRatio,
            ProcessingContract.KEY_COMPRESSOR_THRESHOLD_DB to compressorThresholdDb,
            "noise_floor_db" to noiseFloorDb,
            ProcessingContract.KEY_SPEED to speed,
            ProcessingContract.KEY_PITCH to pitch,
            ProcessingContract.KEY_METADATA_TITLE to title,
            ProcessingContract.KEY_METADATA_ARTIST to artist
        )
        val request = OneTimeWorkRequestBuilder<FfmpegAudioWorker>()
            .setInputData(input)
            .addTag(ProcessingContract.TAG_PROCESSING)
            .addTag(ProcessingContract.toolTag(toolId))
            .addTag(ProcessingContract.sourceTag(sourceName))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request
        )
        return uniqueName
    }

    fun enqueueFfmpegJoin(
        context: Context,
        files: List<Pair<Uri, String>>,
        outputFormat: String = "m4a"
    ): String {
        require(files.size in 2..8) { "Choose between 2 and 8 audio files to join." }
        val uris = files.map { it.first.toString() }
        val names = files.map { it.second }
        val sourceLabel = "${files.size} audio files"
        val digestInput = uris.joinToString("|")
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toByteArray(Charsets.UTF_8))
        val key = bytes.take(10).joinToString("") { "%02x".format(it) }
        val uniqueName = "audio_lab_join_audio_" + key

        val request = OneTimeWorkRequestBuilder<FfmpegAudioWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uris.first(),
                    ProcessingContract.KEY_INPUT_URIS to uris.toTypedArray(),
                    ProcessingContract.KEY_INPUT_NAMES to names.toTypedArray(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceLabel,
                    ProcessingContract.KEY_TOOL_ID to "join-audio",
                    ProcessingContract.KEY_OUTPUT_FORMAT to outputFormat
                )
            )
            .addTag(ProcessingContract.TAG_PROCESSING)
            .addTag(ProcessingContract.toolTag("join-audio"))
            .addTag(ProcessingContract.sourceTag(sourceLabel))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request
        )
        return uniqueName
    }

    private fun uniqueName(toolId: String, uri: Uri): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray(Charsets.UTF_8))
        val key = bytes.take(10).joinToString("") { "%02x".format(it) }
        return "audio_lab_" + toolId + "_" + key
    }
}
