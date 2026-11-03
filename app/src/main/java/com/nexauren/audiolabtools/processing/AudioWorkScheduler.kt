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
            .addTag(ProcessingContract.sourceTag(uri.toString()))
            .addTag(ProcessingContract.sourceNameTag(sourceName))
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
            .addTag(ProcessingContract.sourceTag(uri.toString()))
            .addTag(ProcessingContract.sourceNameTag(sourceName))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request
        )
        return uniqueName
    }


    fun enqueueWavTrim(
        context: Context,
        uri: Uri,
        sourceName: String,
        startMs: Long,
        endMs: Long
    ): String {
        require(startMs >= 0L && endMs > startMs) { "Invalid audio trim range." }
        val toolId = "trim-audio"
        val uniqueName = uniqueName(toolId, uri)
        val request = OneTimeWorkRequestBuilder<WavProcessingWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uri.toString(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId,
                    ProcessingContract.KEY_NORMALIZE to false,
                    ProcessingContract.KEY_GAIN_DB to 0.0,
                    ProcessingContract.KEY_START_MS to startMs,
                    ProcessingContract.KEY_END_MS to endMs
                )
            )
            .addTag(ProcessingContract.TAG_PROCESSING)
            .addTag(ProcessingContract.toolTag(toolId))
            .addTag(ProcessingContract.sourceTag(uri.toString()))
            .addTag(ProcessingContract.sourceNameTag(sourceName))
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
