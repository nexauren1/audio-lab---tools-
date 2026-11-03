package com.nexauren.audiolabtools.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexauren.audiolabtools.data.AudioTool
import com.nexauren.audiolabtools.data.AudioToolCatalog
import com.nexauren.audiolabtools.data.isPortuguese
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette

@Composable
fun FavoritesScreen(
    favoriteToolIds: Set<String>,
    onOpenTool: (AudioTool) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val isPt = isPortuguese()
    val favorites = AudioToolCatalog.tools.filter { it.id in favoriteToolIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp)
            .padding(top = 23.dp, bottom = 24.dp)
    ) {
        Text(
            localized("As tuas ferramentas", "Your tools", isPt),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            localized(
                "Acesso rápido às ferramentas que guardaste.",
                "Quick access to the tools you have saved.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(18.dp))

        if (favorites.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("☆", color = Color(0xFFFFC46B), fontSize = 38.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        localized("Ainda não guardaste ferramentas", "No saved tools yet", isPt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        localized(
                            "Abre a Caixa de Ferramentas e toca na estrela de uma ferramenta para a guardar aqui.",
                            "Open the Toolbox and tap a tool's star to save it here.",
                            isPt
                        ),
                        color = AudioLabPalette.Muted,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    favorites.size.toString() + " " + localized("guardadas", "saved", isPt),
                    color = AudioLabPalette.Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    localized("Toque para abrir", "Tap to open", isPt),
                    color = AudioLabPalette.Muted,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            favorites.forEach { tool ->
                FavoriteToolCard(
                    tool = tool,
                    isPt = isPt,
                    onClick = { onOpenTool(tool) },
                    onRemove = { onToggleFavorite(tool.id) }
                )
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun FavoriteToolCard(
    tool: AudioTool,
    isPt: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val tint = when (tool.category) {
        com.nexauren.audiolabtools.data.ToolCategory.EDIT -> Color(0xFF27A8FF)
        com.nexauren.audiolabtools.data.ToolCategory.CONVERT -> Color(0xFF23D0B5)
        com.nexauren.audiolabtools.data.ToolCategory.CLEAN -> Color(0xFFAE7BFF)
        com.nexauren.audiolabtools.data.ToolCategory.EFFECTS -> Color(0xFFFF79C6)
        com.nexauren.audiolabtools.data.ToolCategory.ANALYZE -> Color(0xFF49C9F2)
        com.nexauren.audiolabtools.data.ToolCategory.METADATA -> Color(0xFFFFB15E)
        com.nexauren.audiolabtools.data.ToolCategory.BATCH -> Color(0xFFFF70BA)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F192B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(tool.symbol, color = tint, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tool.title(isPt), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(tool.description(isPt), color = AudioLabPalette.Muted, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (tool.isAvailable) localized("PRONTA", "READY", isPt)
                    else localized("EM DESENVOLVIMENTO", "IN DEVELOPMENT", isPt),
                    color = if (tool.isAvailable) AudioLabPalette.Teal else Color(0xFFFFBF73),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }
            IconButton(onClick = onRemove) {
                Text("★", color = Color(0xFFFFC46B), fontSize = 22.sp)
            }
        }
    }
}
