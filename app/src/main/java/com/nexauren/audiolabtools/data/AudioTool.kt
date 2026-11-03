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
    INSPECT_AUDIO, TRIM_WAV, SPLIT_WAV, JOIN_WAV, NORMALIZE_WAV, GAIN_WAV, NOT_READY
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
            "Selecionar o início e o fim de um trecho.", "Select the start and end of an audio section.", "✂", ToolOperation.TRIM_WAV),
        AudioTool("split-audio", ToolCategory.EDIT, "Dividir áudio", "Split audio",
            "Separar uma faixa em várias partes.", "Separate one track into multiple parts.", "⋈", ToolOperation.SPLIT_WAV),
        AudioTool("join-audio", ToolCategory.EDIT, "Unir áudios", "Join audio files",
            "Juntar várias faixas numa só.", "Combine multiple tracks into one.", "＋", ToolOperation.JOIN_WAV),
        AudioTool("reverse-audio", ToolCategory.EDIT, "Inverter áudio", "Reverse audio",
            "Reproduzir o áudio do fim para o início.", "Reverse the direction of the audio.", "↶", ToolOperation.NOT_READY),
        AudioTool("fade-in", ToolCategory.EDIT, "Entrada gradual", "Fade in",
            "Aumentar o volume gradualmente no início.", "Gradually raise the volume at the beginning.", "◜", ToolOperation.NOT_READY),
        AudioTool("fade-out", ToolCategory.EDIT, "Saída gradual", "Fade out",
            "Reduzir o volume gradualmente no fim.", "Gradually lower the volume at the end.", "◝", ToolOperation.NOT_READY),
        AudioTool("change-speed", ToolCategory.EDIT, "Alterar velocidade", "Change speed",
            "Acelerar ou desacelerar uma faixa.", "Speed up or slow down a track.", "⏩", ToolOperation.NOT_READY),
        AudioTool("extract-channel", ToolCategory.EDIT, "Separar canais", "Extract channels",
            "Exportar os canais esquerdo e direito.", "Export left and right channels separately.", "⇆", ToolOperation.NOT_READY),

        AudioTool("convert-audio", ToolCategory.CONVERT, "Conversor de áudio", "Audio converter",
            "Converter ficheiros entre formatos suportados.", "Convert files between supported formats.", "⇄", ToolOperation.NOT_READY),
        AudioTool("mp3-to-wav", ToolCategory.CONVERT, "MP3 para WAV", "MP3 to WAV",
            "Converter MP3 para WAV PCM.", "Convert MP3 files to PCM WAV.", "M→W", ToolOperation.NOT_READY),
        AudioTool("wav-to-mp3", ToolCategory.CONVERT, "WAV para MP3", "WAV to MP3",
            "Converter WAV para MP3 com qualidade configurável.", "Convert WAV to MP3 with adjustable quality.", "W→M", ToolOperation.NOT_READY),
        AudioTool("flac-converter", ToolCategory.CONVERT, "Conversor FLAC", "FLAC converter",
            "Converter áudio FLAC com opções claras de qualidade.", "Convert FLAC audio with explicit quality choices.", "FL", ToolOperation.NOT_READY),
        AudioTool("sample-rate", ToolCategory.CONVERT, "Taxa de amostragem", "Sample rate",
            "Alterar a frequência de amostragem.", "Change the audio sample rate.", "〰", ToolOperation.NOT_READY),
        AudioTool("bit-depth", ToolCategory.CONVERT, "Profundidade de bits", "Bit depth",
            "Alterar a resolução PCM do áudio.", "Change the PCM audio resolution.", "24b", ToolOperation.NOT_READY),
        AudioTool("stereo-mono", ToolCategory.CONVERT, "Estéreo / mono", "Stereo / mono",
            "Converter entre áudio mono e estéreo.", "Convert between mono and stereo audio.", "◖◗", ToolOperation.NOT_READY),
        AudioTool("extract-video-audio", ToolCategory.CONVERT, "Extrair de vídeo", "Extract from video",
            "Guardar a faixa de áudio de um vídeo local.", "Save the audio track from a local video.", "▶♫", ToolOperation.NOT_READY),

        AudioTool("normalize-wav", ToolCategory.CLEAN, "Normalizar volume", "Normalize volume",
            "Ajustar o pico sem substituir o original.", "Adjust the peak without replacing the original.", "↗", ToolOperation.NORMALIZE_WAV),
        AudioTool("amplify-wav", ToolCategory.CLEAN, "Aumentar / reduzir", "Amplify / reduce",
            "Aplicar ganho em decibéis.", "Apply gain in decibels.", "◖", ToolOperation.GAIN_WAV),
        AudioTool("remove-silence", ToolCategory.CLEAN, "Remover silêncio", "Remove silence",
            "Detetar e remover pausas longas.", "Detect and remove long silent sections.", "∿", ToolOperation.NOT_READY),
        AudioTool("noise-reduction", ToolCategory.CLEAN, "Reduzir ruído", "Noise reduction",
            "Reduzir ruído constante do ambiente.", "Reduce steady background noise.", "⌁", ToolOperation.NOT_READY),
        AudioTool("audio-compressor", ToolCategory.CLEAN, "Compressor dinâmico", "Dynamic compressor",
            "Controlar diferenças entre sons baixos e altos.", "Control the difference between quiet and loud audio.", "▥", ToolOperation.NOT_READY),
        AudioTool("peak-limiter", ToolCategory.CLEAN, "Limitador de pico", "Peak limiter",
            "Impedir picos acima do limite definido.", "Prevent peaks from exceeding a defined ceiling.", "⌃", ToolOperation.NOT_READY),

        AudioTool("audio-info", ToolCategory.ANALYZE, "Informações do áudio", "Audio information",
            "Consultar formato, duração e propriedades técnicas.", "Inspect format, duration and technical properties.", "ⓘ", ToolOperation.INSPECT_AUDIO),
        AudioTool("waveform-viewer", ToolCategory.ANALYZE, "Visualizador waveform", "Waveform viewer",
            "Ver a forma de onda e navegar pela faixa.", "View the waveform and navigate the track.", "∿", ToolOperation.NOT_READY),
        AudioTool("spectrogram", ToolCategory.ANALYZE, "Espectrograma", "Spectrogram",
            "Visualizar frequências ao longo do tempo.", "Visualize frequencies over time.", "▤", ToolOperation.NOT_READY),
        AudioTool("frequency-analyzer", ToolCategory.ANALYZE, "Analisador de frequência", "Frequency analyzer",
            "Medir a distribuição de frequências.", "Measure the distribution of frequencies.", "⌁", ToolOperation.NOT_READY),
        AudioTool("loudness-meter", ToolCategory.ANALYZE, "Medidor LUFS", "LUFS meter",
            "Analisar loudness integrado e momentâneo.", "Measure integrated and momentary loudness.", "dB", ToolOperation.NOT_READY),
        AudioTool("clipping-detector", ToolCategory.ANALYZE, "Detetor de clipping", "Clipping detector",
            "Encontrar picos cortados ou distorcidos.", "Find clipped or distorted peaks.", "⚠", ToolOperation.NOT_READY),
        AudioTool("silence-detector", ToolCategory.ANALYZE, "Detetor de silêncio", "Silence detector",
            "Localizar pausas e medir a sua duração.", "Locate pauses and measure their duration.", "…", ToolOperation.NOT_READY),

        AudioTool("edit-metadata", ToolCategory.METADATA, "Editor de metadados", "Metadata editor",
            "Editar título, artista, álbum e outras etiquetas.", "Edit title, artist, album and other tags.", "▤", ToolOperation.NOT_READY),
        AudioTool("cover-art", ToolCategory.METADATA, "Capa do áudio", "Audio cover art",
            "Consultar, adicionar ou substituir a capa.", "View, add or replace embedded cover art.", "▧", ToolOperation.NOT_READY),
        AudioTool("file-renamer", ToolCategory.METADATA, "Renomear ficheiros", "File renamer",
            "Renomear com padrões e numeração.", "Rename files with patterns and numbering.", "✎", ToolOperation.NOT_READY),
        AudioTool("metadata-cleaner", ToolCategory.METADATA, "Limpar metadados", "Clean metadata",
            "Remover etiquetas desnecessárias ou privadas.", "Remove unnecessary or private tags.", "⌫", ToolOperation.NOT_READY),

        AudioTool("batch-convert", ToolCategory.BATCH, "Conversão em lote", "Batch conversion",
            "Converter vários ficheiros numa única operação.", "Convert multiple files in one operation.", "⇉", ToolOperation.NOT_READY),
        AudioTool("batch-normalize", ToolCategory.BATCH, "Normalização em lote", "Batch normalization",
            "Aplicar o mesmo alvo de volume a vários áudios.", "Apply the same loudness target to several files.", "▥", ToolOperation.NOT_READY),
        AudioTool("batch-rename", ToolCategory.BATCH, "Renomeação em lote", "Batch renaming",
            "Organizar nomes de uma coleção inteira.", "Organize names across an entire collection.", "▦", ToolOperation.NOT_READY),
        AudioTool("batch-export", ToolCategory.BATCH, "Exportação em lote", "Batch export",
            "Preparar vários resultados numa só fila.", "Prepare multiple outputs in one queue.", "⇩", ToolOperation.NOT_READY)
    )

    fun find(id: String): AudioTool? = tools.firstOrNull { it.id == id }
    fun toolsIn(category: ToolCategory): List<AudioTool> = tools.filter { it.category == category }
}

fun isPortuguese(locale: Locale = Locale.getDefault()): Boolean =
    locale.language.equals("pt", ignoreCase = true)

fun localized(pt: String, en: String, isPt: Boolean = isPortuguese()): String =
    if (isPt) pt else en
