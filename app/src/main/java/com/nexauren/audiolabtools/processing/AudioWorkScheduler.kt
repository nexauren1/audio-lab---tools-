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


    fun enqueueWavSplit(
        context: Context,
        uri: Uri,
        sourceName: String,
        partCount: Int
    ): String {
        require(partCount in 2..20) { "Choose between 2 and 20 parts." }
        val toolId = "split-audio"
        val uniqueName = uniqueName(toolId, uri)
        val request = OneTimeWorkRequestBuilder<WavProcessingWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uri.toString(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId,
                    ProcessingContract.KEY_SPLIT_PARTS to partCount,
                    ProcessingContract.KEY_NORMALIZE to false,
                    ProcessingContract.KEY_GAIN_DB to 0.0
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


    fun enqueueWavFade(
        context: Context,
        uri: Uri,
        sourceName: String,
        fadeIn: Boolean,
        fadeDurationMs: Long
    ): String {
        require(fadeDurationMs in 1L..60_000L) { "Fade duration must be between 1 ms and 60 seconds." }
        val toolId = if (fadeIn) "fade-in" else "fade-out"
        val uniqueName = uniqueName(toolId, uri)
        val request = OneTimeWorkRequestBuilder<WavProcessingWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uri.toString(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId,
                    ProcessingContract.KEY_FADE_MS to fadeDurationMs,
                    ProcessingContract.KEY_NORMALIZE to false,
                    ProcessingContract.KEY_GAIN_DB to 0.0
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

    fun enqueueWavEcho(
        context: Context,
        uri: Uri,
        sourceName: String,
        delayMs: Long,
        feedback: Double,
        mix: Double
    ): String {
        require(delayMs in 40L..1000L) { "Echo delay must be between 40 and 1000 ms." }
        require(feedback.isFinite() && feedback in 0.0..0.7) { "Echo feedback must be between 0 and 0.7." }
        require(mix.isFinite() && mix in 0.0..1.0) { "Echo mix must be between 0 and 1." }
        val toolId = "echo"
        val uniqueName = uniqueName(toolId, uri)
        val request = OneTimeWorkRequestBuilder<WavProcessingWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uri.toString(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId,
                    ProcessingContract.KEY_DELAY_MS to delayMs,
                    ProcessingContract.KEY_FEEDBACK to feedback,
                    ProcessingContract.KEY_MIX to mix,
                    ProcessingContract.KEY_NORMALIZE to false,
                    ProcessingContract.KEY_GAIN_DB to 0.0
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

    fun enqueueWavReverb(
        context: Context,
        uri: Uri,
        sourceName: String,
        decay: Double,
        mix: Double
    ): String {
        require(decay.isFinite() && decay in 0.1..0.9) { "Reverb decay must be between 0.1 and 0.9." }
        require(mix.isFinite() && mix in 0.0..1.0) { "Reverb mix must be between 0 and 1." }
        val toolId = "reverb"
        val uniqueName = uniqueName(toolId, uri)
        val request = OneTimeWorkRequestBuilder<WavProcessingWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URI to uri.toString(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId,
                    ProcessingContract.KEY_DECAY to decay,
                    ProcessingContract.KEY_MIX to mix,
                    ProcessingContract.KEY_NORMALIZE to false,
                    ProcessingContract.KEY_GAIN_DB to 0.0
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

    fun enqueueWavJoin(
        context: Context,
        uris: List<Uri>,
        sourceName: String
    ): String {
        require(uris.size in 2..20) { "Choose between 2 and 20 WAV files." }
        val toolId = "join-audio"
        val sourceKey = uris.joinToString("\n") { it.toString() }
        val uniqueName = uniqueName(toolId, sourceKey)
        val request = OneTimeWorkRequestBuilder<WavProcessingWorker>()
            .setInputData(
                workDataOf(
                    ProcessingContract.KEY_INPUT_URIS to uris.map { it.toString() }.toTypedArray(),
                    ProcessingContract.KEY_SOURCE_NAME to sourceName,
                    ProcessingContract.KEY_TOOL_ID to toolId,
                    ProcessingContract.KEY_NORMALIZE to false,
                    ProcessingContract.KEY_GAIN_DB to 0.0
                )
            )
            .addTag(ProcessingContract.TAG_PROCESSING)
            .addTag(ProcessingContract.toolTag(toolId))
            .addTag(ProcessingContract.sourceTag(sourceKey))
            .addTag(ProcessingContract.sourceNameTag(sourceName))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request
        )
        return uniqueName
    }

    private fun uniqueName(toolId: String, uri: Uri): String = uniqueName(toolId, uri.toString())

    private fun uniqueName(toolId: String, sourceKey: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(sourceKey.toByteArray(Charsets.UTF_8))
        val key = bytes.take(10).joinToString("") { "%02x".format(it) }
        return "audio_lab_" + toolId + "_" + key
    }
}
