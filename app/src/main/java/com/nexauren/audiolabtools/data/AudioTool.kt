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
    TRIM_WAV,
    NORMALIZE_WAV,
    GAIN_WAV,
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
            "Definir início e fim e exportar apenas o trecho escolhido.", "Set start and end points and export only the selected section.",
            "✂", ToolOperation.TRIM_WAV),
        AudioTool("split-audio", ToolCategory.EDIT, "Dividir áudio", "Split audio",
            "Separar um ficheiro em várias partes.", "Split one file into separate parts.",
            "⋈", ToolOperation.NOT_READY),
        AudioTool("join-audio", ToolCategory.EDIT, "Unir áudios", "Join audio files",
            "Juntar vários ficheiros numa faixa.", "Combine multiple files into one track.",
            "＋", ToolOperation.NOT_READY),
        AudioTool("convert-audio", ToolCategory.CONVERT, "Converter formato", "Convert format",
            "Converter entre formatos de áudio.", "Convert between supported audio formats.",
            "⇄", ToolOperation.NOT_READY),
        AudioTool("sample-rate", ToolCategory.CONVERT, "Taxa de amostragem", "Sample rate",
            "Alterar a frequência de amostragem.", "Change the audio sample rate.",
            "〰", ToolOperation.NOT_READY),
        AudioTool("normalize-wav", ToolCategory.CLEAN, "Normalizar volume", "Normalize volume",
            "Ajustar o pico do áudio WAV PCM sem clipping.", "Bring a PCM WAV peak to a consistent level.",
            "↗", ToolOperation.NORMALIZE_WAV),
        AudioTool("amplify-wav", ToolCategory.CLEAN, "Aumentar / reduzir volume", "Amplify / reduce",
            "Aplicar ganho em decibéis a áudio WAV PCM.", "Apply gain in decibels to PCM WAV audio.",
            "◖", ToolOperation.GAIN_WAV),
        AudioTool("remove-silence", ToolCategory.CLEAN, "Remover silêncio", "Remove silence",
            "Detetar e remover pausas longas.", "Detect and remove long silent sections.",
            "∿", ToolOperation.NOT_READY),
        AudioTool("audio-info", ToolCategory.ANALYZE, "Informações do áudio", "Audio information",
            "Ver duração, formato, tamanho e dados técnicos.", "Inspect duration, format, size and technical details.",
            "ⓘ", ToolOperation.INSPECT_AUDIO),
        AudioTool("compare-audio", ToolCategory.ANALYZE, "Comparar ficheiros", "Compare files",
            "Comparar propriedades de dois ficheiros.", "Compare the properties of two audio files.",
            "⇆", ToolOperation.NOT_READY),
        AudioTool("edit-metadata", ToolCategory.METADATA, "Editor de metadados", "Metadata editor",
            "Consultar e editar etiquetas do ficheiro.", "View and edit file tags.",
            "▤", ToolOperation.NOT_READY),
        AudioTool("batch-process", ToolCategory.BATCH, "Processamento em lote", "Batch processing",
            "Aplicar uma operação a vários ficheiros.", "Apply one operation to multiple files.",
            "▦", ToolOperation.NOT_READY),
        AudioTool("audio-compress", ToolCategory.CONVERT, "Comprimir áudio", "Compress audio",
            "Reduzir o tamanho com controlo de qualidade.", "Reduce file size with quality controls.",
            "⇣", ToolOperation.NOT_READY)
    )

    fun find(id: String): AudioTool? = tools.firstOrNull { it.id == id }

    fun toolsIn(category: ToolCategory): List<AudioTool> =
        tools.filter { it.category == category }
}

fun isPortuguese(locale: Locale = Locale.getDefault()): Boolean =
    locale.language.equals("pt", ignoreCase = true)

fun localized(pt: String, en: String, isPt: Boolean = isPortuguese()): String =
    if (isPt) pt else en
