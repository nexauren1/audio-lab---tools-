package com.nexauren.audiolabtools.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nexauren.audiolabtools.data.AudioToolCatalog
import com.nexauren.audiolabtools.data.isPortuguese
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.processing.ProcessingContract
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette

@Composable
fun ProcessingScreen() {
    val context = LocalContext.current
    val isPt = isPortuguese()
    val workManager = WorkManager.getInstance(context)
    val workInfos by workManager
        .getWorkInfosByTagLiveData(ProcessingContract.TAG_PROCESSING)
        .observeAsState(initial = emptyList())

    val sortedJobs = workInfos.sortedBy {
        when (it.state) {
            WorkInfo.State.RUNNING -> 0
            WorkInfo.State.ENQUEUED -> 1
            WorkInfo.State.BLOCKED -> 2
            WorkInfo.State.FAILED -> 3
            WorkInfo.State.SUCCEEDED -> 4
            WorkInfo.State.CANCELLED -> 5
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 22.dp, bottom = 24.dp)
    ) {
        Text(
            localized("Processamento", "Processing", isPt),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            localized(
                "Os trabalhos continuam na fila quando sais de uma ferramenta.",
                "Jobs stay in the queue when you leave a tool.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))

        if (sortedJobs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("◷", color = AudioLabPalette.Cyan, fontSize = 35.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        localized("Ainda não há tarefas.", "No jobs yet.", isPt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        localized(
                            "Quando iniciares um processamento, ele aparecerá aqui.",
                            "When you start a job, it will appear here.",
                            isPt
                        ),
                        color = AudioLabPalette.Muted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            sortedJobs.forEach { info ->
                ProcessingJobCard(
                    info = info,
                    isPt = isPt,
                    onCancel = { workManager.cancelWorkById(info.id) },
                    onOpenOutput = { uri ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(uri), "audio/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Keep the result in the system Music/Audio Lab folder.
                        }
                    }
                )
                Spacer(Modifier.height(11.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            localized(
                "Regra: voltar à aplicação mostra o estado real da tarefa. Não volta a iniciar o processamento.",
                "Rule: returning to the app shows the real job state. It does not restart processing.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ProcessingJobCard(
    info: WorkInfo,
    isPt: Boolean,
    onCancel: () -> Unit,
    onOpenOutput: (String) -> Unit
) {
    val toolId = info.tags.firstOrNull { it.startsWith("audio_lab_tool_") }
        ?.removePrefix("audio_lab_tool_")
    val tool = toolId?.let(AudioToolCatalog::find)
    val title = tool?.title(isPt) ?: localized("Processamento de áudio", "Audio processing", isPt)
    val sourceName = info.inputData.getString(ProcessingContract.KEY_SOURCE_NAME).orEmpty()
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
        colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    status,
                    color = when (info.state) {
                        WorkInfo.State.FAILED -> Color(0xFFFF7086)
                        WorkInfo.State.SUCCEEDED -> AudioLabPalette.Teal
                        else -> AudioLabPalette.Cyan
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (sourceName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(sourceName, color = AudioLabPalette.Muted, fontSize = 11.sp, maxLines = 1)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                color = AudioLabPalette.Cyan,
                trackColor = Color(0xFF26334B)
            )
            Spacer(Modifier.height(6.dp))
            Text(stage ?: (progress.toString() + "%"), color = AudioLabPalette.Muted, fontSize = 10.sp)

            if (info.state == WorkInfo.State.ENQUEUED ||
                info.state == WorkInfo.State.RUNNING ||
                info.state == WorkInfo.State.BLOCKED
            ) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF263047),
                        contentColor = AudioLabPalette.Cyan
                    )
                ) {
                    Text(localized("Cancelar", "Cancel", isPt), fontSize = 11.sp)
                }
            }

            if (info.state == WorkInfo.State.SUCCEEDED) {
                Spacer(Modifier.height(7.dp))
                if (outputName != null) {
                    Text(outputName, color = AudioLabPalette.Teal, fontSize = 11.sp)
                } else {
                    Text(
                        localized("Análise concluída.", "Inspection complete.", isPt),
                        color = AudioLabPalette.Teal,
                        fontSize = 11.sp
                    )
                    val mime = info.outputData.getString(ProcessingContract.KEY_MIME).orEmpty()
                    val duration = info.outputData.getLong(ProcessingContract.KEY_DURATION, 0L)
                    val bitrate = info.outputData.getLong(ProcessingContract.KEY_BITRATE, 0L)
                    if (mime.isNotBlank()) Text(mime, color = Color.White, fontSize = 10.sp)
                    if (duration > 0L) Text(
                        localized("Duração: ", "Duration: ", isPt) + formatDuration(duration),
                        color = AudioLabPalette.Muted,
                        fontSize = 10.sp
                    )
                    if (bitrate > 0L) Text(
                        (bitrate / 1000L).toString() + " kbps",
                        color = AudioLabPalette.Muted,
                        fontSize = 10.sp
                    )
                }
                if (outputUri != null) {
                    Button(
                        onClick = { onOpenOutput(outputUri) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123A4E))
                    ) {
                        Text(localized("Abrir resultado", "Open result", isPt), color = AudioLabPalette.Cyan)
                    }
                }
            }
            if (info.state == WorkInfo.State.FAILED) {
                Spacer(Modifier.height(7.dp))
                Text(
                    error ?: localized("A tarefa falhou.", "The job failed.", isPt),
                    color = Color(0xFFFF8797),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(seconds / 60L, seconds % 60L)
}
