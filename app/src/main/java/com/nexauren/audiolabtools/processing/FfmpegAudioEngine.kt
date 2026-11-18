package com.nexauren.audiolabtools.processing

import android.os.Build
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import org.json.JSONObject

object FfmpegAudioEngine {
    private val supportedAbis = setOf("arm64-v8a", "x86_64")

    fun isSupportedOnThisDevice(): Boolean =
        Build.SUPPORTED_ABIS.any { it in supportedAbis }

    fun execute(arguments: List<String>): String {
        check(isSupportedOnThisDevice()) {
            "The multiformat audio engine requires a 64-bit ARM or x86_64 device."
        }
        val session = FFmpegKit.executeWithArguments(arguments.toTypedArray())
        val returnCode = session.getReturnCode()
        if (!ReturnCode.isSuccess(returnCode)) {
            val details = session.getFailStackTrace()
                ?.takeIf { it.isNotBlank() }
                ?: session.getOutput()?.takeLast(1200)
                ?: "FFmpeg could not complete this audio operation."
            throw IllegalStateException(details)
        }
        return session.getOutput().orEmpty()
    }

    fun probe(filePath: String): JSONObject {
        check(isSupportedOnThisDevice()) {
            "The multiformat audio inspector requires a 64-bit ARM or x86_64 device."
        }
        val session = FFprobeKit.executeWithArguments(
            arrayOf(
                "-v", "error",
                "-show_entries",
                "format=duration,size,bit_rate,format_name:stream=codec_name,sample_rate,channels,bits_per_sample",
                "-of", "json",
                filePath
            )
        )
        val returnCode = session.getReturnCode()
        if (!ReturnCode.isSuccess(returnCode)) {
            throw IllegalArgumentException(
                session.getOutput()?.takeLast(1000)
                    ?: "This file could not be decoded by the audio engine."
            )
        }
        return JSONObject(session.getOutput().orEmpty())
    }
}
