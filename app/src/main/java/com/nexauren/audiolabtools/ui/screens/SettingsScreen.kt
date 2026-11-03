package com.nexauren.audiolabtools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexauren.audiolabtools.data.isPortuguese
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette
import java.util.Locale

@Composable
fun SettingsScreen() {
    val isPt = isPortuguese()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(
            localized("Definições", "Settings", isPt),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            localized("Controlos gerais do Audio Lab.", "General Audio Lab controls.", isPt),
            color = AudioLabPalette.Muted,
            fontSize = 12.sp
        )
        SettingsCard(
            title = localized("Idioma", "Language", isPt),
            value = Locale.getDefault().displayLanguage
        )
        SettingsCard(
            title = localized("Processamento", "Processing", isPt),
            value = localized(
                "Fila persistente • execução local • sem reinício ao voltar",
                "Persistent queue • local execution • no restart on return",
                isPt
            )
        )
        SettingsCard(
            title = localized("Armazenamento", "Storage", isPt),
            value = localized(
                "Os ficheiros de origem não são alterados. Os resultados são guardados como novos ficheiros.",
                "Source files are not changed. Results are saved as new files.",
                isPt
            )
        )
        SettingsCard(
            title = localized("Versão", "Version", isPt),
            value = "0.1.0 • Foundation"
        )
        Spacer(Modifier.height(4.dp))
        Text(
            localized(
                "O Audio Lab não envia os teus ficheiros para um servidor. As ferramentas desta versão processam os ficheiros no dispositivo.",
                "Audio Lab does not upload your files to a server. Tools in this version process files on the device.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SettingsCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = AudioLabPalette.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, color = Color.White, fontSize = 13.sp)
        }
    }
}
