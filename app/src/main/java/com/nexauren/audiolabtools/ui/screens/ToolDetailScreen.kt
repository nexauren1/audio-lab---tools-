package com.nexauren.audiolabtools.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nexauren.audiolabtools.processing.ProcessingContract
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette
import java.util.Locale

@Composable
fun ToolDetailScreen(
    tool: AudioTool,
    initialUri: String?,
    initialName: String?,
    onSourceSelected: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isPt = isPortuguese()
    val workManager = WorkManager.getInstance(context)

    val validInitialUri = initialUri?.takeIf {
        tool.id == "join-audio" || !it.contains("\n")
    }
    var selectedUri by rememberSaveable(tool.id, validInitialUri) { mutableStateOf(validInitialUri) }
    var sourceName by rememberSaveable(tool.id, initialName) {
        mutableStateOf(if (validInitialUri != null) initialName.orEmpty() else "")
    }
    var gainDb by rememberSaveable(tool.id) { mutableStateOf(3f) }
    var trimStartSeconds by rememberSaveable(tool.id) { mutableStateOf("0") }
    var trimEndSeconds by rememberSaveable(tool.id) { mutableStateOf("10") }
    var splitPartCount by rememberSaveable(tool.id) { mutableStateOf("3") }
    var fadeDurationSeconds by rememberSaveable(tool.id) { mutableStateOf("1.0") }

    val trimStartMs = trimStartSeconds.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.times(1000.0)?.toLong()
    val trimEndMs = trimEndSeconds.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.times(1000.0)?.toLong()
    val trimRangeValid = trimStartMs != null && trimEndMs != null && trimEndMs > trimStartMs
    val splitParts = splitPartCount.toIntOrNull()?.takeIf { it in 2..20 }
    val fadeDurationMs = fadeDurationSeconds.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it in 0.001..60.0 }
        ?.times(1000.0)?.toLong()

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
            onSourceSelected(selectedUri!!, sourceName)
        }
    }


    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.size in 2..20) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some providers do not offer persistable grants; current selections remain usable.
                }
            }
            val sourceKey = uris.joinToString("\n") { it.toString() }
            selectedUri = sourceKey
            sourceName = "joined_" + uris.size + "_files.wav"
            onSourceSelected(sourceKey, sourceName)
        }
    }

    val workInfosLiveData = workManager.getWorkInfosByTagLiveData(
        ProcessingContract.toolTag(tool.id)
    )
    val workInfos by workInfosLiveData.observeAsState(initial = emptyList())
    // A tool screen only observes jobs for its currently selected source file.
    // Other files may be processed independently in the background.
    val selectedSourceTag = selectedUri?.let(ProcessingContract::sourceTag)
    val sourceWorkInfos = if (selectedSourceTag == null) emptyList() else {
        workInfos.filter { selectedSourceTag in it.tags }
    }
    val workInfo = sourceWorkInfos.firstOrNull {
        it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.BLOCKED
    } ?: sourceWorkInfos.lastOrNull()
    val isProcessing = sourceWorkInfos.any {
        it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.BLOCKED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState())
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

        Spacer(Modifier.height(22.dp))
        if (!tool.isAvailable) {
            NotReadyPanel(tool, isPt)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
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
                        localized(
                            "Escolhe um ficheiro do dispositivo. O original nunca será alterado.",
                            "Choose a file from your device. The original will never be changed.",
                            isPt
                        ),
                        color = AudioLabPalette.Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (tool.operation == ToolOperation.JOIN_WAV) {
                                multiPicker.launch(arrayOf("audio/*"))
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
                        Text("＋  " + localized("Selecionar áudio", "Choose audio", isPt))
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
                                    if (tool.operation == ToolOperation.JOIN_WAV) {
                                        localized(
                                            (selectedUri?.split("\n")?.size ?: 0).toString() + " ficheiros selecionados",
                                            (selectedUri?.split("\n")?.size ?: 0).toString() + " files selected",
                                            isPt
                                        )
                                    } else sourceName.ifBlank { "Audio file" },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                Text(
                                    if (tool.operation == ToolOperation.JOIN_WAV) {
                                        localized("Os WAV serão unidos pela ordem selecionada.", "WAV files will be joined in the selected order.", isPt)
                                    } else selectedUri!!.substringBefore("?").take(54),
                                    color = AudioLabPalette.Muted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                            TextButtonLike(
                                text = localized("Trocar", "Change", isPt),
                                onClick = {
                                    if (tool.operation == ToolOperation.JOIN_WAV) {
                                        multiPicker.launch(arrayOf("audio/*"))
                                    } else {
                                        picker.launch(arrayOf("audio/*"))
                                    }
                                }
                            )
                        }
                    }
                }
            }


            if (tool.operation == ToolOperation.TRIM_WAV) {
                Spacer(Modifier.height(15.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            localized("Intervalo do corte", "Trim range", isPt),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            localized(
                                "Define o início e o fim em segundos. O ficheiro original fica intacto.",
                                "Set the start and end in seconds. The original file remains untouched.",
                                isPt
                            ),
                            color = AudioLabPalette.Muted,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = trimStartSeconds,
                                onValueChange = { trimStartSeconds = it.filter { ch -> ch.isDigit() || ch == '.' }.take(9) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text(localized("Início (s)", "Start (s)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = AudioLabPalette.Cyan,
                                    unfocusedBorderColor = AudioLabPalette.Border,
                                    focusedLabelColor = AudioLabPalette.Cyan,
                                    unfocusedLabelColor = AudioLabPalette.Muted,
                                    cursorColor = AudioLabPalette.Cyan
                                )
                            )
                            OutlinedTextField(
                                value = trimEndSeconds,
                                onValueChange = { trimEndSeconds = it.filter { ch -> ch.isDigit() || ch == '.' }.take(9) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text(localized("Fim (s)", "End (s)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = AudioLabPalette.Cyan,
                                    unfocusedBorderColor = AudioLabPalette.Border,
                                    focusedLabelColor = AudioLabPalette.Cyan,
                                    unfocusedLabelColor = AudioLabPalette.Muted,
                                    cursorColor = AudioLabPalette.Cyan
                                )
                            )
                        }
                        if (!trimRangeValid) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                localized("O fim deve ser maior que o início.", "End must be greater than start."),
                                color = Color(0xFFFF8797),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }


            if (tool.operation == ToolOperation.JOIN_WAV) {
                Spacer(Modifier.height(15.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            localized("Unir vários ficheiros", "Join multiple files", isPt),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            localized(
                                "Seleciona entre 2 e 20 WAV PCM. Todos devem ter a mesma taxa de amostragem, canais e profundidade de bits.",
                                "Select 2–20 PCM WAV files. All must have the same sample rate, channels and bit depth.",
                                isPt
                            ),
                            color = AudioLabPalette.Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (tool.operation == ToolOperation.SPLIT_WAV) {
                Spacer(Modifier.height(15.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            localized("Número de partes", "Number of parts", isPt),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(
                            value = splitPartCount,
                            onValueChange = { splitPartCount = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(localized("Partes (2–20)", "Parts (2–20)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AudioLabPalette.Cyan,
                                unfocusedBorderColor = AudioLabPalette.Border,
                                focusedLabelColor = AudioLabPalette.Cyan,
                                unfocusedLabelColor = AudioLabPalette.Muted,
                                cursorColor = AudioLabPalette.Cyan
                            )
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            localized(
                                "As partes terão durações praticamente iguais. O resultado será um ZIP com os ficheiros WAV separados.",
                                "Parts will have nearly equal durations. The result is a ZIP containing separate WAV files.",
                                isPt
                            ),
                            color = AudioLabPalette.Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (tool.operation == ToolOperation.FADE_IN_WAV ||
                tool.operation == ToolOperation.FADE_OUT_WAV
            ) {
                Spacer(Modifier.height(15.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            localized("Duração da transição", "Fade duration", isPt),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fadeDurationSeconds,
                            onValueChange = {
                                fadeDurationSeconds = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(localized("Segundos (0,001–60)", "Seconds (0.001–60)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AudioLabPalette.Cyan,
                                unfocusedBorderColor = AudioLabPalette.Border,
                                focusedLabelColor = AudioLabPalette.Cyan,
                                unfocusedLabelColor = AudioLabPalette.Muted,
                                cursorColor = AudioLabPalette.Cyan
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            localized(
                                "A transição é aplicada aos primeiros ou últimos segundos da faixa. O original fica intacto.",
                                "The fade is applied to the first or last seconds of the track. The original remains untouched.",
                                isPt
                            ),
                            color = AudioLabPalette.Muted,
                            fontSize = 11.sp
                        )
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

            Spacer(Modifier.height(17.dp))
            val supported = when (tool.operation) {
                ToolOperation.INSPECT_AUDIO -> localized(
                    "Aceita ficheiros que o Android consiga ler como áudio.",
                    "Accepts audio files that Android can read.",
                    isPt
                )
                ToolOperation.TRIM_WAV -> localized(
                    "Corte WAV PCM inteiro de 8, 16, 24 ou 32 bits. O intervalo é alinhado a frames de áudio completos.",
                    "Trim 8, 16, 24 or 32-bit integer PCM WAV. The range is aligned to complete audio frames.",
                    isPt
                )
                ToolOperation.SPLIT_WAV -> localized(
                    "Divisão WAV PCM inteiro de 8, 16, 24 ou 32 bits. O resultado é um ficheiro ZIP com os segmentos.",
                    "Split 8, 16, 24 or 32-bit integer PCM WAV. The result is a ZIP archive containing the segments.",
                    isPt
                )
                ToolOperation.JOIN_WAV -> localized(
                    "União de 2 a 20 WAV PCM inteiros. Todos os ficheiros têm de ter a mesma taxa de amostragem, canais e profundidade de bits.",
                    "Join 2–20 integer PCM WAV files. Every file must have the same sample rate, channel count and bit depth.",
                    isPt
                )
                ToolOperation.REVERSE_WAV -> localized(
                    "Inverte a ordem dos frames do WAV PCM, mantendo os canais e os samples de cada frame juntos. Aceita PCM inteiro de 8, 16, 24 ou 32 bits.",
                    "Reverses WAV PCM frame order while keeping channels and samples in each frame together. Supports 8, 16, 24 or 32-bit integer PCM.",
                    isPt
                )
                ToolOperation.FADE_IN_WAV, ToolOperation.FADE_OUT_WAV -> localized(
                    "Transição linear de volume por frames, preservando os canais. Aceita WAV PCM inteiro de 8, 16, 24 ou 32 bits.",
                    "Linear frame-based volume fade that preserves channels. Supports 8, 16, 24 or 32-bit integer PCM WAV.",
                    isPt
                )
                ToolOperation.NORMALIZE_WAV, ToolOperation.GAIN_WAV -> localized(
                    "Nesta versão: WAV PCM inteiro de 8, 16, 24 ou 32 bits. WAV float e outros formatos ainda não são aceites por este motor.",
                    "This version: 8, 16, 24 or 32-bit integer PCM WAV. Float WAV and other formats are not supported by this engine yet.",
                    isPt
                )
                else -> ""
            }
            InfoLine(text = supported)

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val sourceKey = selectedUri ?: return@Button
                    val uri = if (tool.operation == ToolOperation.JOIN_WAV) null else Uri.parse(sourceKey)
                    when (tool.operation) {
                        ToolOperation.INSPECT_AUDIO ->
                            AudioWorkScheduler.enqueueInspection(context, uri ?: return@Button, sourceName)
                        ToolOperation.NORMALIZE_WAV ->
                            AudioWorkScheduler.enqueueWavOperation(
                                context, uri ?: return@Button, sourceName, tool.id, normalize = true, gainDb = 0.0
                            )
                        ToolOperation.GAIN_WAV ->
                            AudioWorkScheduler.enqueueWavOperation(
                                context, uri ?: return@Button, sourceName, tool.id, normalize = false, gainDb = gainDb.toDouble()
                            )
                        ToolOperation.TRIM_WAV ->
                            AudioWorkScheduler.enqueueWavTrim(
                                context, uri ?: return@Button, sourceName,
                                startMs = trimStartMs ?: return@Button,
                                endMs = trimEndMs ?: return@Button
                            )
                        ToolOperation.SPLIT_WAV ->
                            AudioWorkScheduler.enqueueWavSplit(
                                context, uri ?: return@Button, sourceName,
                                partCount = splitParts ?: return@Button
                            )
                        ToolOperation.JOIN_WAV -> AudioWorkScheduler.enqueueWavJoin(
                            context,
                            sourceKey.split("\n").map { Uri.parse(it) },
                            sourceName
                        )
                        ToolOperation.REVERSE_WAV -> AudioWorkScheduler.enqueueWavOperation(
                            context, uri ?: return@Button, sourceName,
                            tool.id, normalize = false, gainDb = 0.0
                        )
                        ToolOperation.FADE_IN_WAV, ToolOperation.FADE_OUT_WAV ->
                            AudioWorkScheduler.enqueueWavFade(
                                context, uri ?: return@Button, sourceName,
                                fadeIn = tool.operation == ToolOperation.FADE_IN_WAV,
                                fadeDurationMs = fadeDurationMs ?: return@Button
                            )
                        ToolOperation.NOT_READY -> Unit
                    }
                },
                enabled = selectedUri != null && !isProcessing &&
                    (tool.operation == ToolOperation.INSPECT_AUDIO ||
                        sourceName.endsWith(".wav", ignoreCase = true)) &&
                    (tool.operation != ToolOperation.JOIN_WAV ||
                        sourceKeyCount(selectedUri) in 2..20) &&
                    (tool.operation != ToolOperation.TRIM_WAV || trimRangeValid) &&
                    (tool.operation != ToolOperation.SPLIT_WAV || splitParts != null) &&
                    ((tool.operation != ToolOperation.FADE_IN_WAV &&
                        tool.operation != ToolOperation.FADE_OUT_WAV) || fadeDurationMs != null),
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
                        ToolOperation.INSPECT_AUDIO -> localized("Analisar ficheiro", "Analyse file", isPt)
                        ToolOperation.NORMALIZE_WAV -> localized("Normalizar áudio", "Normalize audio", isPt)
                        ToolOperation.GAIN_WAV -> localized("Aplicar ganho", "Apply gain", isPt)
                        ToolOperation.TRIM_WAV -> localized("Cortar e exportar trecho", "Trim and export section", isPt)
                        ToolOperation.SPLIT_WAV -> localized("Dividir e exportar ZIP", "Split and export ZIP", isPt)
                        ToolOperation.JOIN_WAV -> localized("Unir e exportar WAV", "Join and export WAV", isPt)
                        ToolOperation.REVERSE_WAV -> localized("Inverter e exportar WAV", "Reverse and export WAV", isPt)
                        ToolOperation.FADE_IN_WAV -> localized("Aplicar entrada gradual", "Apply fade in", isPt)
                        ToolOperation.FADE_OUT_WAV -> localized("Aplicar saída gradual", "Apply fade out", isPt)
                        ToolOperation.NOT_READY -> ""
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            if (tool.operation != ToolOperation.INSPECT_AUDIO &&
                selectedUri != null && !sourceName.endsWith(".wav", ignoreCase = true)
            ) {
                Spacer(Modifier.height(6.dp))
                Text(
                    localized("Esta ferramenta exige um ficheiro .wav.", "This tool requires a .wav file.", isPt),
                    color = AudioLabPalette.Muted,
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
                            setDataAndType(
                                Uri.parse(output),
                                workInfo.outputData.getString(ProcessingContract.KEY_OUTPUT_MIME) ?: "audio/*"
                            )
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
private fun NotReadyPanel(tool: AudioTool, isPt: Boolean) {
    val accent = when (tool.category) {
        com.nexauren.audiolabtools.data.ToolCategory.EDIT -> Color(0xFF27A8FF)
        com.nexauren.audiolabtools.data.ToolCategory.CONVERT -> Color(0xFF23D0B5)
        com.nexauren.audiolabtools.data.ToolCategory.CLEAN -> Color(0xFFAE7BFF)
        com.nexauren.audiolabtools.data.ToolCategory.ANALYZE -> Color(0xFF49C9F2)
        com.nexauren.audiolabtools.data.ToolCategory.METADATA -> Color(0xFFFFB15E)
        com.nexauren.audiolabtools.data.ToolCategory.BATCH -> Color(0xFFFF70BA)
    }
    val fileFlow = when (tool.id) {
        "extract-video-audio" -> Pair(localized("Vídeo local", "Local video", isPt), localized("Novo ficheiro de áudio", "New audio file", isPt))
        "waveform-viewer", "spectrogram", "frequency-analyzer", "loudness-meter", "clipping-detector", "silence-detector" ->
            Pair(localized("Ficheiro de áudio", "Audio file", isPt), localized("Relatório / visualização", "Report / visualization", isPt))
        "edit-metadata", "cover-art", "metadata-cleaner" ->
            Pair(localized("Ficheiro com etiquetas", "Tagged audio file", isPt), localized("Cópia com metadados", "Copy with metadata", isPt))
        "batch-convert", "batch-normalize", "batch-rename", "batch-export" ->
            Pair(localized("Vários ficheiros", "Multiple files", isPt), localized("Coleção de resultados", "Output collection", isPt))
        "remove-silence", "noise-reduction", "audio-compressor", "peak-limiter" ->
            Pair(localized("Áudio de origem", "Source audio", isPt), localized("Áudio otimizado", "Optimized audio", isPt))
        "convert-audio", "mp3-to-wav", "wav-to-mp3", "flac-converter", "sample-rate", "bit-depth", "stereo-mono" ->
            Pair(localized("Ficheiro de áudio", "Audio file", isPt), localized("Novo formato / resolução", "New format / resolution", isPt))
        else -> Pair(localized("Ficheiro de áudio", "Audio file", isPt), localized("Novo ficheiro processado", "New processed file", isPt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E192B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(49.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tool.symbol, color = accent, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(localized("ESPECIFICAÇÃO DA FERRAMENTA", "TOOL SPECIFICATION", isPt),
                        color = accent, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(tool.title(isPt), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(13.dp))
            Text(tool.description(isPt), color = Color(0xFFC0CDE1), fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(17.dp))
            Text(localized("FLUXO PREVISTO", "PLANNED WORKFLOW", isPt),
                color = AudioLabPalette.Muted, fontSize = 9.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlannedStep("01", localized("Selecionar", "Select", isPt), Modifier.weight(1f))
                PlannedStep("02", localized("Configurar", "Configure", isPt), Modifier.weight(1f))
                PlannedStep("03", localized("Processar", "Process", isPt), Modifier.weight(1f))
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PlannedFileCard(localized("ENTRADA", "INPUT", isPt), fileFlow.first, Modifier.weight(1f), accent)
                PlannedFileCard(localized("RESULTADO", "OUTPUT", isPt), fileFlow.second, Modifier.weight(1f), accent)
            }
            Spacer(Modifier.height(15.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF2A2030)).padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◌", color = Color(0xFFFFC477), fontSize = 19.sp)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(localized("Motor em desenvolvimento", "Engine in development", isPt),
                        color = Color(0xFFFFD39B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        localized(
                            "Esta página descreve a ferramenta. A execução só será ativada depois de o motor e os testes estarem prontos.",
                            "This page describes the tool. Processing will only be enabled after its engine and tests are ready.",
                            isPt
                        ),
                        color = AudioLabPalette.Muted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedStep(number: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Color(0xFF17243A)).padding(10.dp)) {
        Text(number, color = AudioLabPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlannedFileCard(title: String, value: String, modifier: Modifier, accent: Color) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Color(0xFF111F33)).padding(11.dp)) {
        Text(title, color = accent, fontSize = 8.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(5.dp))
        Text(value, color = Color.White, fontSize = 10.sp, lineHeight = 13.sp)
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
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(7.dp)),
                color = AudioLabPalette.Cyan,
                trackColor = Color(0xFF26334B)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                stage ?: (progress.toString() + "%"),
                color = AudioLabPalette.Muted,
                fontSize = 11.sp
            )
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
                    if (sampleRate > 0) MetadataLine(localized("Amostragem", "Sample rate", isPt), (sampleRate / 1000.0).toString() + " kHz")
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

private fun sourceKeyCount(sourceKey: String?): Int =
    sourceKey?.split("\n")?.count { it.isNotBlank() } ?: 0

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
