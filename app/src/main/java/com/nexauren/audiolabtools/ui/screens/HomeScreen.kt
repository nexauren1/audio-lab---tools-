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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    onOpenProcessing: () -> Unit,
    onOpenTools: () -> Unit
) {
    val isPt = isPortuguese()
    val quickTools = listOf(
        "audio-info" to localized("Analisar", "Analyze", isPt),
        "normalize-wav" to localized("Normalizar", "Normalize", isPt),
        "amplify-wav" to localized("Volume", "Gain", isPt)
    ).mapNotNull { (id, label) -> AudioToolCatalog.find(id)?.let { it to label } }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(AudioLabPalette.Violet, AudioLabPalette.Cyan))),
                contentAlignment = Alignment.Center
            ) {
                Text("♫", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Audio Lab", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    localized("FERRAMENTAS DE ÁUDIO", "AUDIO TOOLBOX", isPt),
                    color = AudioLabPalette.Muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF111D32))
                    .border(1.dp, Color(0xFF29466D), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text("●  " + localized("LOCAL", "OFFLINE", isPt), color = AudioLabPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(23.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(218.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF171044), Color(0xFF102A58), Color(0xFF061323))))
                .border(1.dp, Color(0xFF344A94), RoundedCornerShape(25.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.72f)
            ) {
                Text(
                    localized("O teu áudio.\nO teu espaço de trabalho.", "Your audio\nworkspace.", isPt),
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    localized(
                        "Ferramentas profissionais para editar, converter e melhorar ficheiros de áudio.",
                        "Professional tools to edit, convert and enhance audio files.",
                        isPt
                    ),
                    color = Color(0xFFD1DDF4),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(Brush.horizontalGradient(listOf(AudioLabPalette.Violet, AudioLabPalette.Blue, AudioLabPalette.Cyan)))
                        .clickable(onClick = onOpenTools)
                        .padding(horizontal = 15.dp, vertical = 9.dp)
                ) {
                    Text(
                        localized("Explorar ferramentas  →", "Explore tools  →", isPt),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HeadphoneArtwork(
                modifier = Modifier.align(Alignment.CenterEnd).size(112.dp, 145.dp)
            )
        }

        Spacer(Modifier.height(25.dp))
        SectionHeading(
            title = localized("Ações rápidas", "Quick actions", isPt),
            action = localized("Ver tudo  →", "See all  →", isPt),
            onAction = onOpenTools
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            quickTools.forEachIndexed { index, (tool, label) ->
                val tint = listOf(
                    Color(0xFFB53BFF), Color(0xFF00BDEB), Color(0xFF21D6A2)
                )[index]
                QuickActionCard(
                    title = label,
                    symbol = tool.symbol,
                    tint = tint,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenTool(tool) }
                )
            }
            QuickActionCard(
                title = localized("Ver tudo", "All tools", isPt),
                symbol = "▦",
                tint = Color(0xFFFF963B),
                modifier = Modifier.weight(1f),
                onClick = onOpenTools
            )
        }

        Spacer(Modifier.height(26.dp))
        SectionHeading(
            title = localized("Categorias", "Categories", isPt),
            action = localized("Todas  →", "All tools  →", isPt),
            onAction = onOpenTools
        )
        Spacer(Modifier.height(12.dp))
        val categoryItems = listOf(
            Triple(ToolCategory.EDIT, localized("Editar", "Edit", isPt), Color(0xFFFF4A8B)),
            Triple(ToolCategory.CONVERT, localized("Converter", "Convert", isPt), Color(0xFF19BFFF)),
            Triple(ToolCategory.CLEAN, localized("Melhorar", "Enhance", isPt), Color(0xFF37D9A5)),
            Triple(ToolCategory.ANALYZE, localized("Analisar", "Analyze", isPt), Color(0xFF9B6CFF))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            categoryItems.forEach { (category, title, tint) ->
                CategoryCard(
                    title = title,
                    category = category,
                    tint = tint,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTools
                )
            }
        }

        Spacer(Modifier.height(25.dp))
        SectionHeading(
            title = localized("A tua atividade", "Your activity", isPt),
            action = localized("Abrir fila  →", "Open queue  →", isPt),
            onAction = onOpenProcessing
        )
        Spacer(Modifier.height(11.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenProcessing,
            shape = RoundedCornerShape(19.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2D)),
            border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(43.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF252052)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◷", color = AudioLabPalette.Violet, fontSize = 26.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        localized("Fila de processamento", "Processing queue", isPt),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Text(
                        localized("Acompanha tarefas e resultados", "Track jobs and completed results", isPt),
                        color = AudioLabPalette.Muted, fontSize = 11.sp
                    )
                }
                Text("›", color = AudioLabPalette.Cyan, fontSize = 27.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            localized(
                "Processamento local • Os ficheiros originais são preservados",
                "Local processing • Original files are preserved",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            action,
            color = AudioLabPalette.Cyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(5.dp)
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    symbol: String,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.13f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(symbol, color = tint, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    category: ToolCategory,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(category.symbol, color = tint, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun HeadphoneArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width * 0.53f
        val top = size.height * 0.2f
        drawArc(
            brush = Brush.sweepGradient(listOf(Color(0xFFB42FFF), Color(0xFF04C9FF), Color(0xFFB42FFF))),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - size.width * 0.38f, top),
            size = Size(size.width * 0.76f, size.height * 0.67f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF8D3BFF),
            topLeft = Offset(centerX - size.width * 0.39f, top + size.height * 0.27f),
            size = Size(size.width * 0.18f, size.height * 0.34f),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF08C9F4),
            topLeft = Offset(centerX + size.width * 0.21f, top + size.height * 0.27f),
            size = Size(size.width * 0.18f, size.height * 0.34f),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        val heights = listOf(0.18f, 0.34f, 0.55f, 0.78f, 0.48f, 0.27f, 0.62f, 0.39f, 0.7f)
        heights.forEachIndexed { index, fraction ->
            val x = size.width * (0.12f + index * 0.095f)
            val barHeight = size.height * fraction * 0.28f
            drawRoundRect(
                color = if (index % 2 == 0) Color(0xFF984DFF) else Color(0xFF00BDF4),
                topLeft = Offset(x, size.height * 0.91f - barHeight / 2f),
                size = Size(3.dp.toPx(), barHeight),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}
