package com.nexauren.audiolabtools.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.livedata.observeAsState
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nexauren.audiolabtools.data.AudioTool
import com.nexauren.audiolabtools.data.ToolOperation
import com.nexauren.audiolabtools.data.isPortuguese
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.processing.AudioWorkScheduler
import com.nexauren.audiolabtools.processing.FfmpegAudioEngine
import com.nexauren.audiolabtools.processing.ProcessingContract
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette
import java.util.Locale

@Composable
fun ToolDetailScreen(
    tool: AudioTool,
    initialUri: String?,
    initialName: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isPt = isPortuguese()
    val workManager = WorkManager.getInstance(context)

    var selectedUri by rememberSaveable(tool.id, initialUri) { mutableStateOf(initialUri) }
    var sourceName by rememberSaveable(tool.id, initialName) { mutableStateOf(initialName.orEmpty()) }
    var selectedJoinUris by rememberSaveable(tool.id) { mutableStateOf(emptyList<String>()) }
    var selectedJoinNames by rememberSaveable(tool.id) { mutableStateOf(emptyList<String>()) }
    var gainDb by rememberSaveable(tool.id) { mutableStateOf(3f) }
    var outputFormat by rememberSaveable(tool.id) {
        mutableStateOf(
            initialName?.substringAfterLast('.', "mp3")?.lowercase(Locale.ROOT)
                ?.takeIf { it in setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus") }
                ?: "mp3"
        )
    }
    var targetBitrate by rememberSaveable(tool.id) { mutableStateOf(128f) }
    var conversionBitrate by rememberSaveable(tool.id) { mutableStateOf(192f) }
    var noiseReductionDb by rememberSaveable(tool.id) { mutableStateOf(12f) }
    var noiseFloorDb by rememberSaveable(tool.id) { mutableStateOf(-50f) }
    var compressorRatio by rememberSaveable(tool.id) { mutableStateOf(3f) }
    var compressorThresholdDb by rememberSaveable(tool.id) { mutableStateOf(-18f) }
    var trimStart by rememberSaveable(tool.id) { mutableStateOf("0") }
    var trimDuration by rememberSaveable(tool.id) { mutableStateOf("15") }
    var speed by rememberSaveable(tool.id) { mutableStateOf(1.25f) }
    var pitch by rememberSaveable(tool.id) { mutableStateOf(2f) }
    var effectAmount by rememberSaveable(tool.id) { mutableStateOf(4f) }
    var sampleRate by rememberSaveable(tool.id) { mutableStateOf(48000) }
    var metadataTitle by rememberSaveable(tool.id) { mutableStateOf("") }
    var metadataArtist by rememberSaveable(tool.id) { mutableStateOf("") }
    val ffmpegSupported = FfmpegAudioEngine.isSupportedOnThisDevice()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not offer persistable grants; the current URI can still be used.
            }
            selectedUri = uri.toString()
            sourceName = queryDisplayName(context, uri)
        }
    }

    val multiplePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { selected: List<Uri> ->
        val limited = selected.take(8)
        limited.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not offer persistable grants; the current URI can still be used.
            }
        }
        selectedJoinUris = limited.map(Uri::toString)
        selectedJoinNames = limited.map { queryDisplayName(context, it) }
        selectedUri = limited.firstOrNull()?.toString()
        sourceName = if (limited.isEmpty()) "" else
            limited.size.toString() + localized(" ficheiros selecionados", " files selected", isPt)
    }

    val workInfosLiveData = workManager.getWorkInfosByTagLiveData(
        ProcessingContract.toolTag(tool.id)
    )
    val workInfos by workInfosLiveData.observeAsState(initial = emptyList())
    val workInfo = workInfos.firstOrNull {
        it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.BLOCKED
    } ?: workInfos.lastOrNull()
    val isProcessing = workInfos.any {
        it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.BLOCKED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AudioLabPalette.SurfaceRaised,
                    contentColor = AudioLabPalette.Cyan
                )
            ) {
                Text("←  " + localized("Voltar", "Back", isPt))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                localized("FERRAMENTA", "TOOL", isPt),
                color = AudioLabPalette.Muted,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(AudioLabPalette.Blue, AudioLabPalette.Violet))),
                contentAlignment = Alignment.Center
            ) {
                Text(tool.symbol, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(tool.title(isPt), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isPt) tool.category.titlePt else tool.category.titleEn,
                    color = AudioLabPalette.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(tool.description(isPt), color = AudioLabPalette.Muted, fontSize = 13.sp)

        Spacer(Modifier.height(17.dp))
        AudioToolBanner(tool = tool, isPt = isPt)
        Spacer(Modifier.height(19.dp))
        if (!tool.isAvailable) {
            NotReadyPanel(isPt)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11192C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303B61))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        localized("Ficheiro de origem", "Source file", isPt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (tool.operation == ToolOperation.FFMPEG_JOIN) {
                            localized(
                                "Seleciona entre 2 e 8 ficheiros. A ordem da seleção será mantida.",
                                "Select 2 to 8 files. The selection order will be preserved.",
                                isPt
                            )
                        } else {
                            localized(
                                "Escolhe um ficheiro do dispositivo. O original nunca será alterado.",
                                "Choose a file from your device. The original will never be changed.",
                                isPt
                            )
                        },
                        color = AudioLabPalette.Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (tool.operation == ToolOperation.FFMPEG_JOIN) {
                                multiplePicker.launch(arrayOf("audio/*"))
                            } else {
                                picker.launch(arrayOf("audio/*"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF172A47),
                            contentColor = AudioLabPalette.Cyan
                        )
                    ) {
                        Text(
                            "＋  " + if (tool.operation == ToolOperation.FFMPEG_JOIN)
                                localized("Selecionar vários áudios", "Choose multiple audio files", isPt)
                            else localized("Selecionar áudio", "Choose audio", isPt)
                        )
                    }

                    if (selectedUri != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0B1425))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("♫", color = AudioLabPalette.Cyan, fontSize = 23.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sourceName.ifBlank { "Audio file" },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                Text(
                                    selectedUri!!.substringBefore("?").take(54),
                                    color = AudioLabPalette.Muted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                            TextButtonLike(
                                text = localized("Trocar", "Change", isPt),
                                onClick = {
                                    if (tool.operation == ToolOperation.FFMPEG_JOIN) {
                                        multiplePicker.launch(arrayOf("audio/*"))
                                    } else {
                                        picker.launch(arrayOf("audio/*"))
                                    }
                                }
                            )
                        }
                        if (tool.operation == ToolOperation.FFMPEG_JOIN && selectedJoinNames.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                selectedJoinNames.joinToString(separator = "\n"),
                                color = AudioLabPalette.Muted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                maxLines = 8
                            )
                        }
                    }
                }
            }

            if (tool.operation == ToolOperation.GAIN_WAV) {
                Spacer(Modifier.height(15.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                localized("Ganho de volume", "Gain amount", isPt),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                String.format(Locale.US, "%+.1f dB", gainDb),
                                color = AudioLabPalette.Cyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = gainDb,
                            onValueChange = { gainDb = it },
                            valueRange = -12f..12f,
                            steps = 23
                        )
                        Text(
                            localized(
                                "Valores positivos aumentam o volume; negativos reduzem. O pico é limitado para evitar clipping.",
                                "Positive values increase volume; negative values reduce it. Peaks are clamped to avoid clipping.",
                                isPt
                            ),
                            color = AudioLabPalette.Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            when (tool.operation) {
                ToolOperation.FFMPEG_CONVERT -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Formato de saída", "Output format", isPt)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            listOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus").forEach { format ->
                                FormatChoice(
                                    label = format.uppercase(Locale.ROOT),
                                    selected = outputFormat == format,
                                    onClick = { outputFormat = format }
                                )
                            }
                        }
                        if (outputFormat in setOf("mp3", "aac", "m4a", "opus")) {
                            Spacer(Modifier.height(13.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    localized("Bitrate", "Bitrate", isPt),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${conversionBitrate.toInt()} kbps",
                                    color = AudioLabPalette.Cyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = conversionBitrate,
                                onValueChange = { conversionBitrate = it },
                                valueRange = 64f..320f,
                                steps = 7
                            )
                            Text(
                                localized("Bitrate mais alto costuma preservar mais detalhe, mas cria ficheiros maiores.", "Higher bitrates usually preserve more detail but create larger files.", isPt),
                                color = AudioLabPalette.Muted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                ToolOperation.FFMPEG_COMPRESS -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Qualidade de compressão", "Compression quality", isPt)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                localized("Bitrate AAC", "AAC bitrate", isPt),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${targetBitrate.toInt()} kbps",
                                color = AudioLabPalette.Cyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = targetBitrate,
                            onValueChange = { targetBitrate = it },
                            valueRange = 64f..256f,
                            steps = 5
                        )
                        Text(
                            localized("Valores menores geram ficheiros menores e podem reduzir a qualidade.", "Lower values create smaller files and may reduce quality.", isPt),
                            color = AudioLabPalette.Muted,
                            fontSize = 10.sp
                        )
                    }
                }
                ToolOperation.FFMPEG_TRIM -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Intervalo de corte", "Trim range", isPt)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = trimStart,
                                onValueChange = { trimStart = it.filter { char -> char.isDigit() || char == '.' } },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text(localized("Início (s)", "Start (s)", isPt)) },
                                colors = audioTextFieldColors()
                            )
                            OutlinedTextField(
                                value = trimDuration,
                                onValueChange = { trimDuration = it.filter { char -> char.isDigit() || char == '.' } },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text(localized("Duração (s)", "Duration (s)", isPt)) },
                                colors = audioTextFieldColors()
                            )
                        }
                        Text(
                            localized("O original permanece intacto. O corte é guardado num novo ficheiro.", "The original stays intact. The trimmed audio is saved as a new file.", isPt),
                            color = AudioLabPalette.Muted,
                            fontSize = 10.sp
                        )
                    }
                }
                ToolOperation.FFMPEG_NOISE_REDUCTION -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Redução de ruído", "Noise reduction", isPt)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Intensidade", "Reduction strength", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text("${noiseReductionDb.toInt()} dB", color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = noiseReductionDb,
                            onValueChange = { noiseReductionDb = it },
                            valueRange = 6f..30f,
                            steps = 11
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Piso de ruído", "Noise floor", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text("${noiseFloorDb.toInt()} dB", color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = noiseFloorDb,
                            onValueChange = { noiseFloorDb = it },
                            valueRange = -80f..-20f,
                            steps = 11
                        )
                        Text(
                            localized("Uma redução muito forte pode retirar detalhes da voz ou da música. Começa com valores moderados.", "Very strong reduction can remove detail from speech or music. Start with moderate settings.", isPt),
                            color = AudioLabPalette.Muted,
                            fontSize = 10.sp
                        )
                    }
                }
                ToolOperation.FFMPEG_DYNAMICS -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Compressor dinâmico", "Dynamic compressor", isPt)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Ratio", "Ratio", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text(String.format(Locale.US, "%.1f:1", compressorRatio), color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = compressorRatio,
                            onValueChange = { compressorRatio = it },
                            valueRange = 1f..8f,
                            steps = 13
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Threshold", "Threshold", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text("${compressorThresholdDb.toInt()} dB", color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = compressorThresholdDb,
                            onValueChange = { compressorThresholdDb = it },
                            valueRange = -36f..-6f,
                            steps = 14
                        )
                        Text(
                            localized("O compressor reduz a dinâmica acima do threshold. Valores extremos podem deixar o áudio demasiado comprimido.", "The compressor reduces dynamics above the threshold. Extreme settings can make audio sound over-compressed.", isPt),
                            color = AudioLabPalette.Muted,
                            fontSize = 10.sp
                        )
                    }
                }
                ToolOperation.FFMPEG_SPEED -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Velocidade de reprodução", "Playback speed", isPt)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Velocidade", "Speed", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text(String.format(Locale.US, "%.2fx", speed), color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2f, steps = 14)
                    }
                }
                ToolOperation.FFMPEG_NIGHTCORE -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Nightcore effect", "Nightcore effect", isPt)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Velocidade", "Speed", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text(String.format(Locale.US, "%.2fx", speed), color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            valueRange = 1.05f..1.5f,
                            steps = 8
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Subida de tonalidade", "Pitch increase", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text("+${pitch.toInt()} st", color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = pitch,
                            onValueChange = { pitch = it },
                            valueRange = 1f..6f,
                            steps = 4
                        )
                        Text(
                            localized(
                                "A velocidade e a tonalidade são aplicadas em conjunto; o ficheiro original é preservado.",
                                "Speed and pitch are applied together; the original file is preserved.",
                                isPt
                            ),
                            color = AudioLabPalette.Muted,
                            fontSize = 10.sp
                        )
                    }
                }
                ToolOperation.FFMPEG_PITCH -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Tonalidade", "Pitch", isPt)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Semitons", "Semitones", isPt), color = Color.White, modifier = Modifier.weight(1f))
                            Text(String.format(Locale.US, "%+.1f", pitch), color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = pitch, onValueChange = { pitch = it }, valueRange = -6f..6f, steps = 23)
                    }
                }
                ToolOperation.FFMPEG_SAMPLE_RATE -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Frequência de saída", "Output sample rate", isPt)) {
                        Text(
                            localized("Escolhe a frequência de amostragem do ficheiro exportado.", "Choose the sample rate for the exported file.", isPt),
                            color = AudioLabPalette.Muted,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(44100, 48000, 96000).forEach { rate ->
                                FormatChoice(
                                    label = if (rate == 44100) "44.1 kHz" else "${rate / 1000} kHz",
                                    selected = sampleRate == rate,
                                    onClick = { sampleRate = rate }
                                )
                            }
                        }
                    }
                }
                ToolOperation.FFMPEG_EFFECT -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Controlos do efeito", "Effect controls", isPt)) {
                        val adjustable = tool.id in setOf(
                            "audio-eq", "audio-8d", "add-reverb", "bass-boost",
                            "treble-boost", "echo-audio", "chorus-audio",
                            "flanger-audio", "phaser-audio", "tremolo-audio",
                            "vibrato-audio", "distortion-audio"
                        )
                        if (tool.id == "voice-changer") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(localized("Deslocamento de tom", "Pitch shift", isPt), color = Color.White, modifier = Modifier.weight(1f))
                                Text(String.format(Locale.US, "%+.1f st", pitch), color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                            }
                            Slider(value = pitch, onValueChange = { pitch = it }, valueRange = -6f..6f, steps = 23)
                        } else if (adjustable) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(localized("Intensidade", "Intensity", isPt), color = Color.White, modifier = Modifier.weight(1f))
                                Text("${effectAmount.toInt()}/10", color = AudioLabPalette.Cyan, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = effectAmount,
                                onValueChange = { effectAmount = it },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                        } else {
                            Text(
                                localized(
                                    "Este efeito utiliza um preset de processamento equilibrado. O original nunca é substituído.",
                                    "This effect uses a balanced processing preset. The original file is never overwritten.",
                                    isPt
                                ),
                                color = AudioLabPalette.Muted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                ToolOperation.FFMPEG_METADATA -> {
                    Spacer(Modifier.height(15.dp))
                    AudioOptionCard(title = localized("Etiquetas do ficheiro", "File tags", isPt)) {
                        OutlinedTextField(
                            value = metadataTitle,
                            onValueChange = { metadataTitle = it.take(120) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(localized("Título", "Title", isPt)) },
                            colors = audioTextFieldColors()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = metadataArtist,
                            onValueChange = { metadataArtist = it.take(120) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(localized("Artista", "Artist", isPt)) },
                            colors = audioTextFieldColors()
                        )
                    }
                }
                else -> Unit
            }

            Spacer(Modifier.height(17.dp))
            val supported = when (tool.operation) {
                ToolOperation.INSPECT_AUDIO -> localized(
                    "Inspeção de ficheiros de áudio compatíveis com o dispositivo.",
                    "Inspection of audio formats supported by the device.",
                    isPt
                )
                ToolOperation.NORMALIZE_WAV,
                ToolOperation.GAIN_WAV,
                ToolOperation.FFMPEG_CONVERT,
                ToolOperation.FFMPEG_COMPRESS,
                ToolOperation.FFMPEG_TRIM,
                ToolOperation.FFMPEG_NOISE_REDUCTION,
                ToolOperation.FFMPEG_SPEED,
                ToolOperation.FFMPEG_PITCH,
                ToolOperation.FFMPEG_NIGHTCORE,
                ToolOperation.FFMPEG_METADATA,
                ToolOperation.FFMPEG_ANALYZE,
                ToolOperation.FFMPEG_JOIN,
                ToolOperation.FFMPEG_DYNAMICS,
                ToolOperation.FFMPEG_SAMPLE_RATE,
                ToolOperation.FFMPEG_EFFECT -> if (ffmpegSupported) {
                    localized(
                        "Motor local multiformato: MP3, WAV, FLAC, AAC, M4A, OGG e OPUS. O ficheiro original não é substituído.",
                        "Local multiformat engine: MP3, WAV, FLAC, AAC, M4A, OGG and OPUS. The source file is never overwritten.",
                        isPt
                    )
                } else {
                    localized(
                        "O motor multiformato requer Android de 64 bits. Neste dispositivo, apenas operações WAV compatíveis continuam disponíveis.",
                        "The multiformat engine requires 64-bit Android. On this device, only compatible WAV operations remain available.",
                        isPt
                    )
                }
                ToolOperation.NOT_READY -> ""
            }
            InfoLine(text = supported)

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val uri = selectedUri?.let(Uri::parse) ?: return@Button
                    when (tool.operation) {
                        ToolOperation.INSPECT_AUDIO ->
                            AudioWorkScheduler.enqueueInspection(context, uri, sourceName)
                        ToolOperation.NORMALIZE_WAV,
                        ToolOperation.GAIN_WAV,
                        ToolOperation.FFMPEG_CONVERT,
                        ToolOperation.FFMPEG_COMPRESS,
                        ToolOperation.FFMPEG_TRIM,
                        ToolOperation.FFMPEG_NOISE_REDUCTION,
                        ToolOperation.FFMPEG_SPEED,
                        ToolOperation.FFMPEG_PITCH,
                        ToolOperation.FFMPEG_NIGHTCORE,
                        ToolOperation.FFMPEG_METADATA,
                        ToolOperation.FFMPEG_ANALYZE,
                        ToolOperation.FFMPEG_DYNAMICS,
                        ToolOperation.FFMPEG_SAMPLE_RATE,
                        ToolOperation.FFMPEG_EFFECT -> {
                            if (!ffmpegSupported && sourceName.endsWith(".wav", ignoreCase = true) &&
                                tool.operation in setOf(ToolOperation.NORMALIZE_WAV, ToolOperation.GAIN_WAV)
                            ) {
                                AudioWorkScheduler.enqueueWavOperation(
                                    context, uri, sourceName, tool.id,
                                    normalize = tool.operation == ToolOperation.NORMALIZE_WAV,
                                    gainDb = gainDb.toDouble()
                                )
                            } else {
                                AudioWorkScheduler.enqueueFfmpegOperation(
                                    context = context,
                                    uri = uri,
                                    sourceName = sourceName,
                                    toolId = tool.id,
                                    outputFormat = outputFormat,
                                    targetBitrate = if (tool.operation == ToolOperation.FFMPEG_COMPRESS) "${targetBitrate.toInt()}k" else "${conversionBitrate.toInt()}k",
                                    startSeconds = trimStart.toDoubleOrNull() ?: 0.0,
                                    durationSeconds = trimDuration.toDoubleOrNull() ?: 15.0,
                                    gainDb = if (tool.operation == ToolOperation.FFMPEG_EFFECT) effectAmount.toDouble() else gainDb.toDouble(),
                                    noiseReductionDb = noiseReductionDb.toDouble(),
                                    noiseFloorDb = noiseFloorDb.toDouble(),
                                    compressorRatio = compressorRatio.toDouble(),
                                    compressorThresholdDb = compressorThresholdDb.toDouble(),
                                    speed = speed.toDouble(),
                                    pitch = pitch.toDouble(),
                                    sampleRate = sampleRate,
                                    title = metadataTitle,
                                    artist = metadataArtist
                                )
                            }
                        }
                        ToolOperation.FFMPEG_JOIN -> {
                            val files = selectedJoinUris.zip(selectedJoinNames).map { (value, name) ->
                                Uri.parse(value) to name
                            }
                            AudioWorkScheduler.enqueueFfmpegJoin(context, files, outputFormat)
                        }
                        ToolOperation.NOT_READY -> Unit
                    }
                },
                enabled = (if (tool.operation == ToolOperation.FFMPEG_JOIN)
                    selectedJoinUris.size in 2..8 else selectedUri != null) && !isProcessing &&
                    (tool.operation == ToolOperation.INSPECT_AUDIO ||
                        ffmpegSupported ||
                        (sourceName.endsWith(".wav", ignoreCase = true) &&
                            tool.operation in setOf(ToolOperation.NORMALIZE_WAV, ToolOperation.GAIN_WAV))) &&
                    (tool.operation != ToolOperation.FFMPEG_TRIM ||
                        ((trimStart.toDoubleOrNull() ?: -1.0) >= 0.0 &&
                            (trimDuration.toDoubleOrNull() ?: 0.0) > 0.0)),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1769D2),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF26334B),
                    disabledContentColor = AudioLabPalette.Muted
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    when (tool.operation) {
                        ToolOperation.INSPECT_AUDIO -> localized("Analisar ficheiro", "Inspect file", isPt)
                        ToolOperation.NORMALIZE_WAV -> localized("Normalizar áudio", "Normalize audio", isPt)
                        ToolOperation.GAIN_WAV -> localized("Aplicar ganho", "Apply gain", isPt)
                        ToolOperation.FFMPEG_CONVERT -> localized("Converter áudio", "Convert audio", isPt)
                        ToolOperation.FFMPEG_COMPRESS -> localized("Comprimir áudio", "Compress audio", isPt)
                        ToolOperation.FFMPEG_TRIM -> localized("Cortar e exportar", "Trim and export", isPt)
                        ToolOperation.FFMPEG_NOISE_REDUCTION -> localized("Reduzir ruído", "Reduce noise", isPt)
                        ToolOperation.FFMPEG_SPEED -> localized("Aplicar velocidade", "Apply speed", isPt)
                        ToolOperation.FFMPEG_PITCH -> localized("Aplicar tonalidade", "Apply pitch", isPt)
                        ToolOperation.FFMPEG_NIGHTCORE -> localized("Criar Nightcore", "Create Nightcore", isPt)
                        ToolOperation.FFMPEG_METADATA -> localized("Guardar metadados", "Save metadata", isPt)
                        ToolOperation.FFMPEG_ANALYZE -> localized("Analisar frequências", "Analyse frequencies", isPt)
                        ToolOperation.FFMPEG_DYNAMICS -> localized("Comprimir dinâmica", "Compress dynamics", isPt)
                        ToolOperation.FFMPEG_SAMPLE_RATE -> localized("Converter frequência", "Convert sample rate", isPt)
                        ToolOperation.FFMPEG_EFFECT -> localized("Aplicar efeito", "Apply effect", isPt)
                        ToolOperation.FFMPEG_JOIN -> localized("Unir ficheiros", "Join audio files", isPt)
                        ToolOperation.NOT_READY -> ""
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            if (!ffmpegSupported && selectedUri != null &&
                tool.operation in setOf(
                    ToolOperation.FFMPEG_CONVERT,
                    ToolOperation.FFMPEG_COMPRESS,
                    ToolOperation.FFMPEG_TRIM,
                    ToolOperation.FFMPEG_NOISE_REDUCTION,
                    ToolOperation.FFMPEG_SPEED,
                    ToolOperation.FFMPEG_PITCH,
                    ToolOperation.FFMPEG_NIGHTCORE,
                    ToolOperation.FFMPEG_METADATA,
                    ToolOperation.FFMPEG_ANALYZE,
                    ToolOperation.FFMPEG_JOIN,
                    ToolOperation.FFMPEG_DYNAMICS,
                    ToolOperation.FFMPEG_SAMPLE_RATE,
                    ToolOperation.FFMPEG_EFFECT
                )
            ) {
                Spacer(Modifier.height(6.dp))
                Text(
                    localized(
                        "Este dispositivo não tem uma ABI de 64 bits suportada pelo motor multiformato.",
                        "This device does not have a 64-bit ABI supported by the multiformat engine.",
                        isPt
                    ),
                    color = Color(0xFFFFC06B),
                    fontSize = 11.sp
                )
            }
        }

        if (workInfo != null) {
            Spacer(Modifier.height(19.dp))
            JobStatusPanel(
                info = workInfo,
                isPt = isPt,
                onCancel = { workManager.cancelWorkById(workInfo.id) },
                onOpenOutput = { output ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(output), "audio/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // The file remains in Music/Audio Lab and in the processing history.
                    }
                }
            )
        }
    }
}

@Composable
private fun AudioToolBanner(tool: AudioTool, isPt: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10172B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF343064))
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(51.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(AudioLabPalette.Violet, AudioLabPalette.Blue, AudioLabPalette.Cyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tool.symbol, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        localized("ESPAÇO DE TRABALHO", "TOOL WORKSPACE", isPt),
                        color = AudioLabPalette.Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        tool.title(isPt),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (tool.isAvailable) Color(0xFF103A3B) else Color(0xFF28223F))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (tool.isAvailable) localized("ATIVA", "READY", isPt)
                        else localized("PLANEADA", "PLANNED", isPt),
                        color = if (tool.isAvailable) AudioLabPalette.Teal else Color(0xFFB9A7F5),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(37.dp)) {
                val bars = listOf(0.22f, 0.42f, 0.67f, 0.36f, 0.82f, 0.54f, 0.29f, 0.73f, 0.95f, 0.48f, 0.31f, 0.65f, 0.86f, 0.44f, 0.72f, 0.28f, 0.58f, 0.91f, 0.38f, 0.63f, 0.82f, 0.33f, 0.57f, 0.76f, 0.4f, 0.68f, 0.29f, 0.52f, 0.87f, 0.45f, 0.71f)
                val gap = size.width / bars.size
                bars.forEachIndexed { index, amount ->
                    val barHeight = size.height * amount
                    drawRoundRect(
                        color = if (index % 3 == 0) AudioLabPalette.Violet else AudioLabPalette.Cyan,
                        topLeft = Offset(index * gap + 1.dp.toPx(), (size.height - barHeight) / 2f),
                        size = Size(3.dp.toPx(), barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
            Text(
                localized(
                    "Interface de controlo • processamento local",
                    "Control workspace • local processing",
                    isPt
                ),
                color = AudioLabPalette.Muted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun AudioOptionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11192C)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(11.dp))
            content()
        }
    }
}

@Composable
private fun FormatChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF4C22A9) else Color(0xFF1A2439))
            .then(
                Modifier.clickable(onClick = onClick)
            )
            .padding(horizontal = 13.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else AudioLabPalette.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun audioTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AudioLabPalette.Violet,
    unfocusedBorderColor = AudioLabPalette.Border,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = AudioLabPalette.Cyan,
    unfocusedLabelColor = AudioLabPalette.Muted,
    cursorColor = AudioLabPalette.Cyan,
    focusedContainerColor = Color(0xFF0D1526),
    unfocusedContainerColor = Color(0xFF0D1526)
)

@Composable
private fun NotReadyPanel(isPt: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("◌", color = AudioLabPalette.Cyan, fontSize = 33.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                localized("Motor em desenvolvimento", "Processing engine in development", isPt),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(7.dp))
            Text(
                localized(
                    "Esta ferramenta já faz parte da organização do Audio Lab, mas ainda não executa operações. Será ativada quando o motor e os testes estiverem prontos.",
                    "This tool is already part of Audio Lab's structure, but it does not run operations yet. It will be enabled when its engine and tests are ready.",
                    isPt
                ),
                color = AudioLabPalette.Muted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1A2D))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓", color = AudioLabPalette.Teal, fontSize = 17.sp)
        Spacer(Modifier.width(9.dp))
        Text(text, color = Color(0xFFB8C7DE), fontSize = 11.sp)
    }
}

@Composable
private fun JobStatusPanel(
    info: WorkInfo,
    isPt: Boolean,
    onCancel: () -> Unit,
    onOpenOutput: (String) -> Unit
) {
    val progress = if (info.state == WorkInfo.State.SUCCEEDED) 100
        else info.progress.getInt(ProcessingContract.KEY_PROGRESS, 0)
    val stage = info.progress.getString(ProcessingContract.KEY_STAGE)
    val status = when (info.state) {
        WorkInfo.State.ENQUEUED -> localized("Na fila", "Queued", isPt)
        WorkInfo.State.RUNNING -> localized("A processar", "Processing", isPt)
        WorkInfo.State.BLOCKED -> localized("A aguardar", "Waiting", isPt)
        WorkInfo.State.SUCCEEDED -> localized("Concluído", "Completed", isPt)
        WorkInfo.State.FAILED -> localized("Falhou", "Failed", isPt)
        WorkInfo.State.CANCELLED -> localized("Cancelado", "Cancelled", isPt)
    }
    val outputUri = info.outputData.getString(ProcessingContract.KEY_OUTPUT_URI)
    val outputName = info.outputData.getString(ProcessingContract.KEY_OUTPUT_NAME)
    val error = info.outputData.getString(ProcessingContract.KEY_ERROR)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101C30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    localized("Estado do processamento", "Processing status", isPt),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    status,
                    color = if (info.state == WorkInfo.State.FAILED) Color(0xFFFF7188) else AudioLabPalette.Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stage ?: localized("A processar o ficheiro", "Processing audio file", isPt),
                        color = Color(0xFFD6E1F5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        localized("A tarefa continua em segundo plano.", "This job can continue in the background.", isPt),
                        color = AudioLabPalette.Muted,
                        fontSize = 10.sp
                    )
                }
                Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = AudioLabPalette.Violet,
                        trackColor = Color(0xFF29334A),
                        strokeWidth = 7.dp
                    )
                    Text(
                        progress.toString() + "%",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
            if (info.state == WorkInfo.State.ENQUEUED ||
                info.state == WorkInfo.State.RUNNING ||
                info.state == WorkInfo.State.BLOCKED
            ) {
                Spacer(Modifier.height(10.dp))
                TextButtonLike(
                    text = localized("Cancelar tarefa", "Cancel job", isPt),
                    onClick = onCancel
                )
            }
            if (info.state == WorkInfo.State.SUCCEEDED) {
                if (outputUri != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        outputName ?: localized("Ficheiro guardado", "File saved", isPt),
                        color = AudioLabPalette.Teal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { onOpenOutput(outputUri) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123A4E))
                    ) {
                        Text(localized("Abrir resultado", "Open result", isPt), color = AudioLabPalette.Cyan)
                    }
                } else {
                    val mime = info.outputData.getString(ProcessingContract.KEY_MIME).orEmpty()
                    val duration = info.outputData.getLong(ProcessingContract.KEY_DURATION, 0L)
                    val bitrate = info.outputData.getLong(ProcessingContract.KEY_BITRATE, 0L)
                    val sampleRate = info.outputData.getInt(ProcessingContract.KEY_SAMPLE_RATE, 0)
                    val size = info.outputData.getLong(ProcessingContract.KEY_SIZE, 0L)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        localized("Análise concluída", "Inspection complete", isPt),
                        color = AudioLabPalette.Teal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    MetadataLine(localized("Formato", "Format", isPt), mime)
                    MetadataLine(localized("Tamanho", "Size", isPt), formatBytes(size))
                    MetadataLine(localized("Duração", "Duration", isPt), formatDuration(duration))
                    if (bitrate > 0L) MetadataLine(localized("Bitrate", "Bitrate", isPt), (bitrate / 1000L).toString() + " kbps")
                    if (sampleRate > 0) MetadataLine(localized("Amostragem", "Sample rate", isPt), sampleRate.toString() + " Hz")
                    val analysisSummary = info.outputData.getString(ProcessingContract.KEY_ANALYSIS_SUMMARY).orEmpty()
                    if (analysisSummary.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            analysisSummary,
                            color = Color(0xFFD0DBEF),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            if (info.state == WorkInfo.State.FAILED) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error ?: localized("Não foi possível concluir o processamento.", "Processing could not be completed.", isPt),
                    color = Color(0xFFFF8797),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AudioLabPalette.Muted, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TextButtonLike(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text, color = AudioLabPalette.Cyan, fontSize = 12.sp)
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index) ?: "Audio file"
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file"
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatBytes(size: Long): String {
    if (size < 1024L) return size.toString() + " B"
    val kb = size / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}
