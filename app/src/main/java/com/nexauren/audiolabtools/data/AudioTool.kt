package com.nexauren.audiolabtools.data

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

enum class ToolCategory(val titlePt: String, val titleEn: String, val symbol: String) {
    EDIT("Edição", "Editing", "✂"),
    CONVERT("Conversão", "Conversion", "⇄"),
    CLEAN("Efeitos e melhoria", "Effects & enhance", "✧"),
    ANALYZE("Análise", "Analysis", "◉"),
    METADATA("Metadados", "Metadata", "ⓘ")
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
    FFMPEG_DYNAMICS,
    FFMPEG_SPEED,
    FFMPEG_PITCH,
    FFMPEG_NIGHTCORE,
    FFMPEG_SAMPLE_RATE,
    FFMPEG_EFFECT,
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
            "Definir início e duração e guardar um novo ficheiro.", "Set a start point and duration, then save a new file.",
            "✂", ToolOperation.FFMPEG_TRIM),
        AudioTool("fade-out-audio", ToolCategory.EDIT, "Saída gradual", "Fade out",
            "Reduzir o volume gradualmente no fim da faixa.", "Fade the volume down gradually at the end of a track.",
            "↘", ToolOperation.FFMPEG_EFFECT),
        AudioTool("join-audio", ToolCategory.EDIT, "Unir áudios", "Join audio files",
            "Juntar de 2 a 8 ficheiros numa faixa.", "Combine 2 to 8 audio files into one track.",
            "＋", ToolOperation.FFMPEG_JOIN),
        AudioTool("convert-audio", ToolCategory.CONVERT, "Converter formato", "Convert format",
            "Converter entre MP3, WAV, FLAC, AAC, M4A, OGG e OPUS.", "Convert between MP3, WAV, FLAC, AAC, M4A, OGG and OPUS.",
            "⇄", ToolOperation.FFMPEG_CONVERT),
        AudioTool("sample-rate", ToolCategory.CONVERT, "Taxa de amostragem", "Sample rate",
            "Converter a frequência de amostragem para 44,1, 48 ou 96 kHz.", "Resample audio to 44.1, 48 or 96 kHz.",
            "〰", ToolOperation.FFMPEG_SAMPLE_RATE),
        AudioTool("normalize-wav", ToolCategory.CLEAN, "Normalizar volume", "Normalize volume",
            "Normalização de loudness para vários formatos de áudio.", "Loudness normalization for multiple audio formats.",
            "↗", ToolOperation.NORMALIZE_WAV),
        AudioTool("amplify-wav", ToolCategory.CLEAN, "Aumentar / reduzir volume", "Amplify / reduce",
            "Aplicar ganho em decibéis a vários formatos.", "Apply gain in decibels to multiple formats.",
            "◖", ToolOperation.GAIN_WAV),
        AudioTool("remove-silence", ToolCategory.CLEAN, "Reduzir ruído", "Noise reduction",
            "Reduzir ruído de fundo com intensidade ajustável.", "Reduce background noise with adjustable strength.",
            "∿", ToolOperation.FFMPEG_NOISE_REDUCTION),
        AudioTool("dynamic-compressor", ToolCategory.CLEAN, "Compressor dinâmico", "Dynamic compressor",
            "Controlar a dinâmica com threshold e ratio ajustáveis.", "Control dynamics with adjustable threshold and ratio.",
            "◒", ToolOperation.FFMPEG_DYNAMICS),
        AudioTool("audio-info", ToolCategory.ANALYZE, "Informações do áudio", "Audio information",
            "Ver duração, formato, tamanho e dados técnicos.", "Inspect duration, format, size and technical details.",
            "ⓘ", ToolOperation.INSPECT_AUDIO),
        AudioTool("compare-audio", ToolCategory.ANALYZE, "Analisar frequências", "Frequency analysis",
            "Analisar níveis, dinâmica e características do áudio.", "Analyse levels, dynamics and audio characteristics.",
            "⇆", ToolOperation.FFMPEG_ANALYZE),
        AudioTool("edit-metadata", ToolCategory.METADATA, "Editor de metadados", "Metadata editor",
            "Editar título e artista em ficheiros compatíveis.", "Edit title and artist tags in supported files.",
            "▤", ToolOperation.FFMPEG_METADATA),
        AudioTool("highpass-audio", ToolCategory.CLEAN, "Filtro passa-alto", "High-pass filter",
            "Reduzir frequências abaixo de 250 Hz.", "Reduce frequencies below 250 Hz.",
            "⌁", ToolOperation.FFMPEG_EFFECT),
        AudioTool("audio-compress", ToolCategory.CONVERT, "Comprimir áudio", "Compress audio",
            "Reduzir o tamanho em AAC/M4A com controlo de bitrate.", "Reduce file size to AAC/M4A with bitrate control.",
            "⇣", ToolOperation.FFMPEG_COMPRESS),
        AudioTool("speed-audio", ToolCategory.EDIT, "Alterar velocidade", "Change speed",
            "Acelerar ou abrandar sem mudar o tom.", "Speed up or slow down without changing pitch.",
            "◴", ToolOperation.FFMPEG_SPEED),
        AudioTool("nightcore", ToolCategory.EDIT, "Criador Nightcore", "Nightcore maker",
            "Acelerar o áudio e elevar a tonalidade num único processamento.", "Speed up audio and raise pitch in one processing pass.",
            "♫", ToolOperation.FFMPEG_NIGHTCORE),
        AudioTool("pitch-audio", ToolCategory.EDIT, "Alterar tonalidade", "Change pitch",
            "Subir ou baixar o tom do áudio.", "Raise or lower the audio pitch.",
            "♫", ToolOperation.FFMPEG_PITCH),

        AudioTool("audio-eq", ToolCategory.CLEAN, "Equalizador de áudio", "Audio equalizer",
            "Ajustar a presença das frequências médias.", "Shape the presence of mid-range frequencies.",
            "▥", ToolOperation.FFMPEG_EFFECT),
        AudioTool("audio-8d", ToolCategory.CLEAN, "Áudio 8D", "8D audio",
            "Criar movimento estéreo espacial com modulação.", "Create a moving stereo field with modulation.",
            "◉", ToolOperation.FFMPEG_EFFECT),
        AudioTool("add-reverb", ToolCategory.CLEAN, "Adicionar reverberação", "Add reverb",
            "Acrescentar espaço e cauda ao som.", "Add space and a decaying room effect.",
            "≋", ToolOperation.FFMPEG_EFFECT),
        AudioTool("bass-boost", ToolCategory.CLEAN, "Reforço de graves", "Bass boost",
            "Dar mais corpo às frequências baixas.", "Give low frequencies more weight.",
            "▂", ToolOperation.FFMPEG_EFFECT),
        AudioTool("treble-boost", ToolCategory.CLEAN, "Reforço de agudos", "Treble boost",
            "Aumentar o brilho das frequências altas.", "Add brightness to high frequencies.",
            "▔", ToolOperation.FFMPEG_EFFECT),
        AudioTool("echo-audio", ToolCategory.CLEAN, "Adicionar eco", "Add echo",
            "Criar repetições audíveis do sinal.", "Create audible repetitions of the signal.",
            "〰", ToolOperation.FFMPEG_EFFECT),
        AudioTool("chorus-audio", ToolCategory.CLEAN, "Efeito chorus", "Chorus effect",
            "Adicionar camadas moduladas para alargar o som.", "Add modulated layers to thicken the sound.",
            "♬", ToolOperation.FFMPEG_EFFECT),
        AudioTool("flanger-audio", ToolCategory.CLEAN, "Efeito flanger", "Flanger effect",
            "Criar varrimentos metálicos e móveis.", "Create moving, metallic sweeps.",
            "∿", ToolOperation.FFMPEG_EFFECT),
        AudioTool("phaser-audio", ToolCategory.CLEAN, "Efeito phaser", "Phaser effect",
            "Aplicar modulação de fase ao áudio.", "Apply phase modulation to audio.",
            "✧", ToolOperation.FFMPEG_EFFECT),
        AudioTool("tremolo-audio", ToolCategory.CLEAN, "Efeito tremolo", "Tremolo effect",
            "Modular o volume de forma rítmica.", "Modulate volume in a rhythmic pattern.",
            "⌁", ToolOperation.FFMPEG_EFFECT),
        AudioTool("vibrato-audio", ToolCategory.CLEAN, "Efeito vibrato", "Vibrato effect",
            "Aplicar uma oscilação suave de afinação.", "Apply a gentle oscillation in pitch.",
            "♒", ToolOperation.FFMPEG_EFFECT),
        AudioTool("distortion-audio", ToolCategory.CLEAN, "Distorção suave", "Soft distortion",
            "Adicionar saturação controlada ao sinal.", "Add controlled saturation to the signal.",
            "⚡", ToolOperation.FFMPEG_EFFECT),
        AudioTool("radio-effect", ToolCategory.CLEAN, "Efeito rádio", "Radio effect",
            "Simular uma banda sonora estreita de rádio.", "Simulate the narrow audio bandwidth of radio.",
            "◖", ToolOperation.FFMPEG_EFFECT),
        AudioTool("telephone-effect", ToolCategory.CLEAN, "Efeito telefone", "Telephone effect",
            "Aplicar filtragem de banda de voz telefónica.", "Apply telephone-style voice band filtering.",
            "☎", ToolOperation.FFMPEG_EFFECT),
        AudioTool("mono-audio", ToolCategory.EDIT, "Converter para mono", "Convert to mono",
            "Misturar os canais num único canal mono.", "Mix the channels down to one mono channel.",
            "◐", ToolOperation.FFMPEG_EFFECT),
        AudioTool("stereo-widen", ToolCategory.CLEAN, "Alargar estéreo", "Stereo widener",
            "Expandir a imagem estéreo em ficheiros compatíveis.", "Widen the stereo image on compatible files.",
            "◑", ToolOperation.FFMPEG_EFFECT),
        AudioTool("reverse-audio", ToolCategory.EDIT, "Inverter áudio", "Reverse audio",
            "Reproduzir o áudio do fim para o início.", "Play the audio from end to beginning.",
            "⇋", ToolOperation.FFMPEG_EFFECT),
        AudioTool("fade-in-audio", ToolCategory.EDIT, "Entrada gradual", "Fade in",
            "Fazer o áudio entrar gradualmente.", "Fade the audio in gradually.",
            "↗", ToolOperation.FFMPEG_EFFECT),
        AudioTool("silence-trim", ToolCategory.EDIT, "Cortar silêncios", "Trim silence",
            "Remover silêncios longos no início e entre segmentos.", "Remove long silences at the start and between sections.",
            "⫿", ToolOperation.FFMPEG_EFFECT),
        AudioTool("limiter-audio", ToolCategory.CLEAN, "Limitador de picos", "Peak limiter",
            "Controlar picos para reduzir clipping.", "Control peaks to reduce clipping.",
            "▰", ToolOperation.FFMPEG_EFFECT),
        AudioTool("voice-changer", ToolCategory.CLEAN, "Alterar voz", "Voice changer",
            "Mudar o carácter vocal com deslocamento de tonalidade.", "Change vocal character with pitch shifting.",
            "♟", ToolOperation.FFMPEG_EFFECT)
    )

    fun find(id: String): AudioTool? = tools.firstOrNull { it.id == id }

    fun toolsIn(category: ToolCategory): List<AudioTool> =
        tools.filter { it.category == category }
}

fun isPortuguese(
    locale: Locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
): Boolean = locale.language.equals("pt", ignoreCase = true)

fun localized(pt: String, en: String, isPt: Boolean = isPortuguese()): String =
    if (isPt) pt else en
