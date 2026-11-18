package com.nexauren.audiolabtools.data

import java.util.Locale

enum class ToolCategory(val titlePt: String, val titleEn: String, val symbol: String) {
    EDIT("Edição", "Editing", "✂"),
    CONVERT("Conversão", "Conversion", "⇄"),
    CLEAN("Limpeza", "Cleaning", "⌁"),
    ANALYZE("Análise", "Analysis", "◉"),
    METADATA("Metadados", "Metadata", "ⓘ"),
    BATCH("Em lote", "Batch", "▦")
}

enum class ToolOperation {
    INSPECT_AUDIO,
    NORMALIZE_WAV,
    GAIN_WAV,
    FFMPEG_CONVERT,
    FFMPEG_COMPRESS,
    FFMPEG_TRIM,
    FFMPEG_JOIN,
    FFMPEG_NOISE_REDUCTION,
    FFMPEG_SPEED,
    FFMPEG_PITCH,
    FFMPEG_METADATA,
    FFMPEG_ANALYZE,
    NOT_READY
}

data class AudioTool(
    val id: String,
    val category: ToolCategory,
    val titlePt: String,
    val titleEn: String,
    val descriptionPt: String,
    val descriptionEn: String,
    val symbol: String,
    val operation: ToolOperation
) {
    val isAvailable: Boolean get() = operation != ToolOperation.NOT_READY

    fun title(isPt: Boolean) = if (isPt) titlePt else titleEn
    fun description(isPt: Boolean) = if (isPt) descriptionPt else descriptionEn
}

object AudioToolCatalog {
    val tools = listOf(
        AudioTool("trim-audio", ToolCategory.EDIT, "Cortar áudio", "Trim audio",
            "Definir início e fim e guardar um novo ficheiro.", "Set start and end points and save a new file.",
            "✂", ToolOperation.FFMPEG_TRIM),
        AudioTool("split-audio", ToolCategory.EDIT, "Dividir áudio", "Split audio",
            "Separar um ficheiro em várias partes.", "Split one file into separate parts.",
            "⋈", ToolOperation.NOT_READY),
        AudioTool("join-audio", ToolCategory.EDIT, "Unir áudios", "Join audio files",
            "Juntar de 2 a 8 ficheiros numa faixa.", "Combine 2 to 8 audio files into one track.",
            "＋", ToolOperation.FFMPEG_JOIN),
        AudioTool("convert-audio", ToolCategory.CONVERT, "Converter formato", "Convert format",
            "Converter entre MP3, WAV, FLAC, AAC, M4A, OGG e OPUS.", "Convert between MP3, WAV, FLAC, AAC, M4A, OGG and OPUS.",
            "⇄", ToolOperation.FFMPEG_CONVERT),
        AudioTool("sample-rate", ToolCategory.CONVERT, "Taxa de amostragem", "Sample rate",
            "Alterar a frequência de amostragem.", "Change the audio sample rate.",
            "〰", ToolOperation.NOT_READY),
        AudioTool("normalize-wav", ToolCategory.CLEAN, "Normalizar volume", "Normalize volume",
            "Normalização de loudness para vários formatos de áudio.", "Loudness normalization for multiple audio formats.",
            "↗", ToolOperation.NORMALIZE_WAV),
        AudioTool("amplify-wav", ToolCategory.CLEAN, "Aumentar / reduzir volume", "Amplify / reduce",
            "Aplicar ganho em decibéis a vários formatos.", "Apply gain in decibels to multiple audio formats.",
            "◖", ToolOperation.GAIN_WAV),
        AudioTool("remove-silence", ToolCategory.CLEAN, "Reduzir ruído", "Noise reduction",
            "Reduzir ruído de fundo com filtro de áudio.", "Reduce background noise with an audio filter.",
            "∿", ToolOperation.FFMPEG_NOISE_REDUCTION),
        AudioTool("audio-info", ToolCategory.ANALYZE, "Informações do áudio", "Audio information",
            "Ver duração, formato, tamanho e dados técnicos.", "Inspect duration, format, size and technical details.",
            "ⓘ", ToolOperation.INSPECT_AUDIO),
        AudioTool("compare-audio", ToolCategory.ANALYZE, "Analisar frequências", "Frequency analysis",
            "Analisar níveis, dinâmica e características do áudio.", "Analyse levels, dynamics and audio characteristics.",
            "⇆", ToolOperation.FFMPEG_ANALYZE),
        AudioTool("edit-metadata", ToolCategory.METADATA, "Editor de metadados", "Metadata editor",
            "Editar título e artista em ficheiros compatíveis.", "Edit title and artist tags in supported files.",
            "▤", ToolOperation.FFMPEG_METADATA),
        AudioTool("batch-process", ToolCategory.BATCH, "Processamento em lote", "Batch processing",
            "Aplicar uma operação a vários ficheiros.", "Apply one operation to multiple files.",
            "▦", ToolOperation.NOT_READY),
        AudioTool("audio-compress", ToolCategory.CONVERT, "Comprimir áudio", "Compress audio",
            "Reduzir o tamanho em AAC/M4A com controlo de bitrate.", "Reduce file size to AAC/M4A with bitrate control.",
            "⇣", ToolOperation.FFMPEG_COMPRESS),
        AudioTool("speed-audio", ToolCategory.EDIT, "Alterar velocidade", "Change speed",
            "Acelerar ou abrandar sem mudar o tom.", "Speed up or slow down without changing pitch.",
            "◴", ToolOperation.FFMPEG_SPEED),
        AudioTool("pitch-audio", ToolCategory.EDIT, "Alterar tonalidade", "Change pitch",
            "Subir ou baixar o tom do áudio.", "Raise or lower the audio pitch.",
            "♫", ToolOperation.FFMPEG_PITCH)
    )

    fun find(id: String): AudioTool? = tools.firstOrNull { it.id == id }

    fun toolsIn(category: ToolCategory): List<AudioTool> =
        tools.filter { it.category == category }
}

fun isPortuguese(locale: Locale = Locale.getDefault()): Boolean =
    locale.language.equals("pt", ignoreCase = true)

fun localized(pt: String, en: String, isPt: Boolean = isPortuguese()): String =
    if (isPt) pt else en
