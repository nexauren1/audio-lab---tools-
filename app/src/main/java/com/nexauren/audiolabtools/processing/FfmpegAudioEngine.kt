package com.nexauren.audiolabtools.processing

import android.os.Build
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StatisticsCallback
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject

object FfmpegAudioEngine {
    private val supportedAbis = setOf("arm64-v8a", "x86_64")

    fun isSupportedOnThisDevice(): Boolean =
        Build.SUPPORTED_ABIS.any { it in supportedAbis }

    /**
     * Runs FFmpeg asynchronously so WorkManager cancellation can stop the native session.
     * Statistics are reported as processed output time in milliseconds.
     */
    suspend fun execute(
        arguments: List<String>,
        onStatistics: (Long) -> Unit = {}
    ): String {
        check(isSupportedOnThisDevice()) {
            "The multiformat audio engine requires a 64-bit ARM or x86_64 device."
        }

        return suspendCancellableCoroutine { continuation ->
            val sessionId = AtomicLong(-1L)

            continuation.invokeOnCancellation {
                val activeSessionId = sessionId.get()
                if (activeSessionId > 0L) {
                    FFmpegKit.cancel(activeSessionId)
                }
            }

            try {
                val completeCallback = FFmpegSessionCompleteCallback { session ->
                    if (continuation.isActive) {
                        val returnCode = session.returnCode
                        if (ReturnCode.isSuccess(returnCode)) {
                            continuation.resume(session.output.orEmpty())
                        } else {
                            val details = session.failStackTrace
                                ?.takeIf { it.isNotBlank() }
                                ?: session.output?.takeLast(1600)
                                ?: "FFmpeg could not complete this audio operation."
                            continuation.resumeWithException(IllegalStateException(details))
                        }
                    }
                }
                val statisticsCallback = StatisticsCallback { statistics ->
                    if (continuation.isActive) {
                        onStatistics(statistics.time.coerceAtLeast(0.0).toLong())
                    }
                }

                val session = FFmpegKit.executeWithArgumentsAsync(
                    arguments.toTypedArray(),
                    completeCallback,
                    null,
                    statisticsCallback
                )
                sessionId.set(session.sessionId)
                if (!continuation.isActive) {
                    FFmpegKit.cancel(session.sessionId)
                }
            } catch (error: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
        }
    }

    fun probe(filePath: String): JSONObject {
        check(isSupportedOnThisDevice()) {
            "The multiformat audio inspector requires a 64-bit ARM or x86_64 device."
        }
        val session = FFprobeKit.executeWithArguments(
            arrayOf(
                "-v", "error",
                "-show_entries",
                "format=duration,size,bit_rate,format_name:stream=codec_name,sample_rate,channels,bits_per_sample,bits_per_raw_sample",
                "-of", "json",
                filePath
            )
        )
        val returnCode = session.returnCode
        if (!ReturnCode.isSuccess(returnCode)) {
            throw IllegalArgumentException(
                session.output?.takeLast(1000)
                    ?: "This file could not be decoded by the audio engine."
            )
        }
        return JSONObject(session.output.orEmpty())
    }

    fun durationMs(filePath: String): Long {
        val format = probe(filePath).optJSONObject("format") ?: JSONObject()
        return (format.optDouble("duration", 0.0) * 1000.0)
            .toLong()
            .coerceAtLeast(0L)
    }
}
