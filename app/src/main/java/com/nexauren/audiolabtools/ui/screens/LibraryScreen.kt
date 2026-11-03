package com.nexauren.audiolabtools.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nexauren.audiolabtools.data.isPortuguese
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private data class AudioFileItem(
    val uri: Uri,
    val name: String,
    val mime: String,
    val size: Long,
    val duration: Long
)

@Composable
fun LibraryScreen(onOpenFile: (String, String) -> Unit) {
    val context = LocalContext.current
    val isPt = isPortuguese()
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isLoading by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(emptyList<AudioFileItem>()) }
    var error by remember { mutableStateOf<String?>(null) }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            error = null
            try {
                files = withContext(Dispatchers.IO) { queryAudioFiles(context) }
            } catch (problem: Exception) {
                error = problem.message ?: localized("Não foi possível ler a biblioteca.", "Could not read the library.", isPt)
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 22.dp, bottom = 18.dp)
    ) {
        Text(
            localized("Ficheiros de áudio", "Audio files", isPt),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            localized(
                "Biblioteca local do dispositivo. A importação pela ferramenta também funciona sem esta permissão.",
                "Local device library. Importing from inside a tool also works without this permission.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(17.dp))

        if (!hasPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("♫", color = AudioLabPalette.Cyan, fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        localized("Permitir acesso aos áudios", "Allow audio access", isPt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        localized(
                            "A permissão é usada apenas para listar ficheiros de áudio que já estão no dispositivo.",
                            "This permission is only used to list audio files already on the device.",
                            isPt
                        ),
                        color = AudioLabPalette.Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { requestPermission.launch(permission) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769D2))
                    ) {
                        Text(localized("Permitir", "Allow", isPt))
                    }
                }
            }
        } else if (isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = AudioLabPalette.Cyan)
                Spacer(Modifier.height(10.dp))
                Text(localized("A carregar ficheiros...", "Loading files...", isPt), color = AudioLabPalette.Muted)
            }
        } else if (error != null) {
            Text(error!!, color = Color(0xFFFF8797), fontSize = 12.sp)
        } else if (files.isEmpty()) {
            Text(
                localized("Não foram encontrados ficheiros de áudio.", "No audio files were found.", isPt),
                color = AudioLabPalette.Muted
            )
        } else {
            Text(
                files.size.toString() + " " + localized("ficheiros encontrados", "files found", isPt),
                color = AudioLabPalette.Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(files, key = { it.uri.toString() }) { file ->
                    Card(
                        onClick = { onOpenFile(file.uri.toString(), file.name) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("♫", color = AudioLabPalette.Cyan, fontSize = 25.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1)
                                Text(
                                    file.mime + "  •  " + formatBytes(file.size) + "  •  " + formatDuration(file.duration),
                                    color = AudioLabPalette.Muted,
                                    fontSize = 10.sp
                                )
                            }
                            Text("→", color = AudioLabPalette.Cyan, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun queryAudioFiles(context: android.content.Context): List<AudioFileItem> {
    val resolver = context.contentResolver
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DURATION
    )
    val result = mutableListOf<AudioFileItem>()
    resolver.query(
        collection,
        projection,
        null,
        null,
        MediaStore.Audio.Media.DATE_ADDED + " DESC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        while (cursor.moveToNext() && result.size < 1000) {
            val id = cursor.getLong(idIndex)
            result += AudioFileItem(
                uri = ContentUris.withAppendedId(collection, id),
                name = cursor.getString(nameIndex) ?: "Audio",
                mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) ?: "audio/*" else "audio/*",
                size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
            )
        }
    }
    return result
}

private fun formatBytes(size: Long): String {
    if (size < 1024L) return size.toString() + " B"
    val kb = size / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
}
