package com.nexauren.audiolabtools.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexauren.audiolabtools.data.AudioTool
import com.nexauren.audiolabtools.data.AudioToolCatalog
import com.nexauren.audiolabtools.data.ToolCategory
import com.nexauren.audiolabtools.data.isPortuguese
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette

@Composable
fun HomeScreen(
    onOpenTool: (AudioTool) -> Unit,
    onOpenProcessing: () -> Unit
) {
    val isPt = isPortuguese()
    var search by rememberSaveable { mutableStateOf("") }
    val visibleTools = AudioToolCatalog.tools.filter { tool ->
        search.isBlank() ||
            tool.title(isPt).contains(search, ignoreCase = true) ||
            tool.description(isPt).contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 18.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(AudioLabPalette.Cyan, AudioLabPalette.Violet))),
                contentAlignment = Alignment.Center
            ) {
                Text("♫", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Audio Lab", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    localized("TOOLS  •  CAIXA DE FERRAMENTAS", "TOOLS  •  AUDIO TOOLBOX", isPt),
                    color = AudioLabPalette.Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF13243B))
                    .border(1.dp, Color(0xFF24466A), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Text("OFFLINE", color = AudioLabPalette.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(localized("Pesquisar ferramentas...", "Search tools...", isPt), color = AudioLabPalette.Muted)
            },
            leadingIcon = { Text("⌕", color = AudioLabPalette.Cyan, fontSize = 23.sp) },
            shape = RoundedCornerShape(17.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AudioLabPalette.Cyan,
                unfocusedBorderColor = AudioLabPalette.Border,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AudioLabPalette.Cyan,
                focusedContainerColor = AudioLabPalette.Surface,
                unfocusedContainerColor = AudioLabPalette.Surface
            )
        )

        Spacer(Modifier.height(18.dp))
        HeroPanel(isPt = isPt, onOpenProcessing = onOpenProcessing)

        Spacer(Modifier.height(25.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                localized("Caixa de ferramentas", "Toolbox", isPt),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                visibleTools.count { it.isAvailable }.toString() + " " +
                    localized("ativas", "ready", isPt),
                color = AudioLabPalette.Teal,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            localized(
                "Ferramentas práticas para trabalhar com ficheiros de áudio.",
                "Practical tools for working with audio files.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 13.sp
        )

        ToolCategory.entries.forEach { category ->
            val tools = visibleTools.filter { it.category == category }
            if (tools.isNotEmpty()) {
                Spacer(Modifier.height(21.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.symbol, color = AudioLabPalette.Cyan, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPt) category.titlePt else category.titleEn,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                tools.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        pair.forEach { tool ->
                            ToolTile(
                                tool = tool,
                                isPt = isPt,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenTool(tool) }
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(11.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProcessing),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111D32)),
            border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◷", color = AudioLabPalette.Cyan, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        localized("Fila de processamento", "Processing queue", isPt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        localized(
                            "Acompanha tarefas mesmo depois de sair da ferramenta.",
                            "Track jobs even after leaving a tool.",
                            isPt
                        ),
                        color = AudioLabPalette.Muted,
                        fontSize = 12.sp
                    )
                }
                Text("→", color = AudioLabPalette.Cyan, fontSize = 23.sp)
            }
        }
    }
}

@Composable
private fun HeroPanel(isPt: Boolean, onOpenProcessing: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF102B55), Color(0xFF19123C), Color(0xFF080F22))
                )
            )
            .border(1.dp, Color(0xFF285D9B), RoundedCornerShape(23.dp))
            .clickable(onClick = onOpenProcessing)
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
                .fillMaxWidth(0.67f)
        ) {
            Text(
                localized("O teu áudio.\nAs tuas ferramentas.", "Your audio.\nYour tools.", isPt),
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                localized(
                    "Editar • Converter • Limpar • Analisar",
                    "Edit • Convert • Clean • Analyse",
                    isPt
                ),
                color = AudioLabPalette.Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                localized(
                    "Uma caixa de ferramentas, sem funções de canto.",
                    "One toolbox, with no singing features.",
                    isPt
                ),
                color = Color(0xFFC5D5ED),
                fontSize = 11.sp
            )
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(126.dp, 92.dp)
        ) {
            val bars = listOf(18, 30, 48, 66, 82, 56, 38, 72, 88, 58, 34, 50, 76, 42)
            val gap = size.width / (bars.size * 1.55f)
            bars.forEachIndexed { index, height ->
                val x = index * gap + 2.dp.toPx()
                val h = height / 100f * size.height
                drawRoundRect(
                    color = if (index % 3 == 0) AudioLabPalette.Violet else AudioLabPalette.Cyan,
                    topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - h) / 2f),
                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
            }
        }
        Text(
            "↗",
            modifier = Modifier.align(Alignment.BottomEnd),
            color = AudioLabPalette.Cyan,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun ToolTile(
    tool: AudioTool,
    isPt: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint = when (tool.category) {
        ToolCategory.EDIT -> Color(0xFF1768D8)
        ToolCategory.CONVERT -> Color(0xFF087F82)
        ToolCategory.CLEAN -> Color(0xFF5D31B8)
        ToolCategory.ANALYZE -> Color(0xFF126B9C)
        ToolCategory.METADATA -> Color(0xFF9C4E18)
        ToolCategory.BATCH -> Color(0xFF9C286B)
    }
    Card(
        onClick = onClick,
        modifier = modifier.height(126.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.19f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (tool.isAvailable) tint.copy(alpha = 0.85f) else tint.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tint.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tool.symbol, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                if (!tool.isAvailable) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF202B43))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            localized("EM BREVE", "SOON", isPt),
                            color = AudioLabPalette.Muted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                tool.title(isPt),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                tool.description(isPt),
                color = Color(0xFFB7C4D9),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 2
            )
        }
    }
}
