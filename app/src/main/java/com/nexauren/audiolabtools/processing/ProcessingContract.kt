package com.nexauren.audiolabtools.processing

object ProcessingContract {
    const val TAG_PROCESSING = "audio_lab_processing"

    const val KEY_INPUT_URI = "input_uri"
    const val KEY_INPUT_URIS = "input_uris"
    const val KEY_TOOL_ID = "tool_id"
    const val KEY_SOURCE_NAME = "source_name"
    const val KEY_GAIN_DB = "gain_db"
    const val KEY_START_MS = "start_ms"
    const val KEY_END_MS = "end_ms"
    const val KEY_SPLIT_PARTS = "split_parts"
    const val KEY_FADE_MS = "fade_ms"
    const val KEY_DELAY_MS = "delay_ms"
    const val KEY_FEEDBACK = "feedback"
    const val KEY_MIX = "mix"
    const val KEY_DECAY = "decay"
    const val KEY_NORMALIZE = "normalize"
    const val KEY_PROGRESS = "progress"
    const val KEY_STAGE = "stage"

    const val KEY_OUTPUT_URI = "output_uri"
    const val KEY_OUTPUT_NAME = "output_name"
    const val KEY_OUTPUT_MIME = "output_mime"
    const val KEY_MIME = "mime"
    const val KEY_SIZE = "size"
    const val KEY_DURATION = "duration"
    const val KEY_BITRATE = "bitrate"
    const val KEY_SAMPLE_RATE = "sample_rate"
    const val KEY_CHANNELS = "channels"
    const val KEY_BITS = "bits"
    const val KEY_APPLIED_GAIN = "applied_gain"
    const val KEY_ERROR = "error"

    fun toolTag(toolId: String) = "audio_lab_tool_$toolId"

    fun sourceTag(uri: String): String {
        val key = java.security.MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(Charsets.UTF_8))
            .take(10)
            .joinToString("") { "%02x".format(it) }
        return "audio_lab_source_$key"
    }

    fun sourceNameTag(name: String): String {
        val safeName = name.substringBeforeLast('.', name)
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(60)
            .ifBlank { "audio" }
        return "audio_lab_source_name_$safeName"
    }
}
