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

@Composable
fun SettingsScreen(onLanguageSelected: (String) -> Unit) {
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

        Text(
            localized("Idioma da aplicação", "App language", isPt),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            localized(
                "Escolhe o idioma dos menus e das ferramentas.",
                "Choose the language used by menus and tools.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 11.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LanguageCard(
                title = "English",
                code = "EN",
                selected = !isPt,
                modifier = Modifier.weight(1f),
                onClick = { onLanguageSelected("en") }
            )
            LanguageCard(
                title = "Português",
                code = "PT",
                selected = isPt,
                modifier = Modifier.weight(1f),
                onClick = { onLanguageSelected("pt") }
            )
        }

        SettingsCard(
            title = localized("Processamento", "Processing", isPt),
            value = localized(
                "Fila persistente • execução local • tarefas continuam ao sair da ferramenta",
                "Persistent queue • local execution • jobs continue when leaving a tool",
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
            value = "0.2.0"
        )
        Spacer(Modifier.height(4.dp))
        Text(
            localized(
                "O Audio Lab não envia os teus ficheiros para um servidor. As operações disponíveis processam os ficheiros no dispositivo.",
                "Audio Lab does not upload your files to a server. Available operations process files on the device.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LanguageCard(
    title: String,
    code: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF21164B) else AudioLabPalette.Surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) AudioLabPalette.Violet else AudioLabPalette.Border
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                code,
                color = if (selected) AudioLabPalette.Cyan else AudioLabPalette.Muted,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp
            )
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (selected) {
                Text("Selected", color = AudioLabPalette.Cyan, fontSize = 9.sp)
            }
        }
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
