package com.nexauren.audiolabtools.processing

object ProcessingContract {
    const val TAG_PROCESSING = "audio_lab_processing"

    const val KEY_INPUT_URI = "input_uri"
    const val KEY_TOOL_ID = "tool_id"
    const val KEY_SOURCE_NAME = "source_name"
    const val KEY_GAIN_DB = "gain_db"
    const val KEY_NORMALIZE = "normalize"
    const val KEY_PROGRESS = "progress"
    const val KEY_STAGE = "stage"

    const val KEY_OUTPUT_URI = "output_uri"
    const val KEY_OUTPUT_NAME = "output_name"
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
}
