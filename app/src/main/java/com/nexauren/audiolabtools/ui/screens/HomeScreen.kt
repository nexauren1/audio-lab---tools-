package com.nexauren.audiolabtools.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
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
    onOpenLibrary: () -> Unit,
    favoriteToolIds: Set<String>,
    onToggleFavorite: (String) -> Unit
) {
    val isPt = isPortuguese()
    var search by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }

    val matchingTools = AudioToolCatalog.tools.filter { tool ->
        val matchesSearch = search.isBlank() ||
            tool.title(isPt).contains(search, ignoreCase = true) ||
            tool.description(isPt).contains(search, ignoreCase = true) ||
            tool.category.titlePt.contains(search, ignoreCase = true) ||
            tool.category.titleEn.contains(search, ignoreCase = true)
        val matchesCategory = selectedCategory == "ALL" || tool.category.name == selectedCategory
        matchesSearch && matchesCategory
    }
    val readyCount = AudioToolCatalog.tools.count { it.isAvailable }
    val totalCount = AudioToolCatalog.tools.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 26.dp)
    ) {
        Header(isPt = isPt, onOpenLibrary = onOpenLibrary)

        Spacer(Modifier.height(17.dp))
        SearchBar(
            value = search,
            onValueChange = { search = it },
            isPt = isPt
        )

        Spacer(Modifier.height(16.dp))
        HeroWorkspace(isPt = isPt, onOpenProcessing = onOpenProcessing)

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                value = totalCount.toString(),
                label = localized("Ferramentas", "Tools", isPt),
                symbol = "▦",
                tint = AudioLabPalette.Cyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = ToolCategory.entries.size.toString(),
                label = localized("Categorias", "Categories", isPt),
                symbol = "◫",
                tint = AudioLabPalette.Violet,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = readyCount.toString(),
                label = localized("Operacionais", "Operational", isPt),
                symbol = "✓",
                tint = AudioLabPalette.Teal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(25.dp))
        SectionHeading(
            title = localized("Explorar por categoria", "Explore by category", isPt),
            subtitle = localized("Cada área reúne ferramentas para uma tarefa específica.", "Each area groups tools for a specific task.", isPt)
        )
        Spacer(Modifier.height(11.dp))
        CategoryExplorer(
            selectedCategory = selectedCategory,
            onSelect = { selectedCategory = it },
            isPt = isPt
        )

        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (selectedCategory == "ALL") localized("Biblioteca de ferramentas", "Tool library", isPt)
                    else ToolCategory.valueOf(selectedCategory).let { if (isPt) it.titlePt else it.titleEn },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    localized(
                        "${matchingTools.size} ferramentas • pesquisa e filtros disponíveis",
                        "${matchingTools.size} tools • search and filters available",
                        isPt
                    ),
                    color = AudioLabPalette.Muted,
                    fontSize = 11.sp
                )
            }
            Text(
                totalCount.toString() + localized(" no catálogo", " in catalogue", isPt),
                color = AudioLabPalette.Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))

        if (matchingTools.isEmpty()) {
            EmptySearchState(isPt = isPt, onClear = {
                search = ""
                selectedCategory = "ALL"
            })
        } else {
            matchingTools.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { tool ->
                        ToolboxToolCard(
                            tool = tool,
                            isPt = isPt,
                            modifier = Modifier.weight(1f),
                            isFavorite = tool.id in favoriteToolIds,
                            onToggleFavorite = { onToggleFavorite(tool.id) },
                            onClick = { onOpenTool(tool) }
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        ProcessingHubCard(isPt = isPt, onClick = onOpenProcessing)

        Spacer(Modifier.height(12.dp))
        PrivacyNote(isPt = isPt)
    }
}

@Composable
private fun Header(isPt: Boolean, onOpenLibrary: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(49.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(AudioLabPalette.Cyan, AudioLabPalette.Blue, AudioLabPalette.Violet))),
            contentAlignment = Alignment.Center
        ) {
            Text("A", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.size(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Audio Lab", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "PRO AUDIO TOOLBOX",
                color = AudioLabPalette.Cyan,
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onOpenLibrary,
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF15243B),
                contentColor = AudioLabPalette.Cyan
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("♫  " + localized("Ficheiros", "Files", isPt), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit, isPt: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(localized("Pesquisar uma ferramenta, formato ou tarefa…", "Search a tool, format or task…", isPt),
                color = AudioLabPalette.Muted, fontSize = 12.sp)
        },
        leadingIcon = { Text("⌕", color = AudioLabPalette.Cyan, fontSize = 25.sp) },
        trailingIcon = {
            if (value.isNotBlank()) {
                androidx.compose.material3.TextButton(onClick = { onValueChange("") }) {
                    Text("×", color = AudioLabPalette.Muted, fontSize = 18.sp)
                }
            }
        },
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
}

@Composable
private fun HeroWorkspace(isPt: Boolean, onOpenProcessing: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF102C58), Color(0xFF171748), Color(0xFF090F20))
                )
            )
            .border(1.dp, Color(0xFF2866A4), RoundedCornerShape(25.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val wave = listOf(0.12f, 0.22f, 0.38f, 0.57f, 0.82f, 0.64f, 0.45f, 0.76f, 0.91f, 0.59f, 0.34f, 0.71f, 0.48f, 0.25f, 0.52f, 0.86f, 0.62f, 0.31f)
            val baseX = size.width * 0.62f
            val barGap = size.width * 0.018f
            wave.forEachIndexed { index, amplitude ->
                val barHeight = size.height * 0.65f * amplitude
                drawRoundRect(
                    color = if (index % 4 == 0) AudioLabPalette.Violet.copy(alpha = 0.72f) else AudioLabPalette.Cyan.copy(alpha = 0.62f),
                    topLeft = androidx.compose.ui.geometry.Offset(baseX + index * barGap, (size.height - barHeight) / 2f),
                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
            drawCircle(
                color = AudioLabPalette.Blue.copy(alpha = 0.12f),
                radius = size.height * 0.48f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.83f, size.height * 0.50f)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 19.dp, end = 18.dp, top = 17.dp, bottom = 17.dp)
                .fillMaxWidth(0.72f)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x3320D4FF))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    localized("ESTAÇÃO DE TRABALHO DE ÁUDIO", "AUDIO WORKSPACE", isPt),
                    color = AudioLabPalette.Cyan,
                    fontSize = 8.sp,
                    letterSpacing = 1.1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(11.dp))
            Text(
                localized("Todas as ferramentas.\nUm só espaço.", "Every tool.\nOne workspace.", isPt),
                color = Color.White,
                fontSize = 25.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(7.dp))
            Text(
                localized("Editar • Converter • Limpar • Analisar • Organizar", "Edit • Convert • Clean • Analyse • Organize", isPt),
                color = Color(0xFFBBD9F5),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            Spacer(Modifier.height(13.dp))
            Button(
                onClick = onOpenProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF087FD5),
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(localized("Centro de processamento  →", "Processing centre  →", isPt),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "01 / 06",
            modifier = Modifier.align(Alignment.BottomEnd).padding(13.dp),
            color = Color(0xFF91A9C9),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    symbol: String,
    tint: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier.height(77.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AudioLabPalette.Border)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(symbol, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(label, color = AudioLabPalette.Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = AudioLabPalette.Muted, fontSize = 11.sp)
    }
}

@Composable
private fun CategoryExplorer(
    selectedCategory: String,
    onSelect: (String) -> Unit,
    isPt: Boolean
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == "ALL",
            onClick = { onSelect("ALL") },
            label = { Text(localized("Todas", "All", isPt), fontSize = 11.sp) }
        )
        ToolCategory.entries.forEach { category ->
            val selected = selectedCategory == category.name
            FilterChip(
                selected = selected,
                onClick = { onSelect(category.name) },
                label = {
                    Text(
                        (if (isPt) category.titlePt else category.titleEn),
                        fontSize = 11.sp
                    )
                },
                leadingIcon = { Text(category.symbol, fontSize = 13.sp) }
            )
        }
    }
}

@Composable
private fun ToolboxToolCard(
    tool: AudioTool,
    isPt: Boolean,
    modifier: Modifier,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val tint = categoryTint(tool.category)
    Card(
        onClick = onClick,
        modifier = modifier.height(158.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E192B)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (tool.isAvailable) tint.copy(alpha = 0.72f) else AudioLabPalette.Border
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(39.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(tint.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tool.symbol, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(30.dp)
                ) {
                    Text(
                        if (isFavorite) "★" else "☆",
                        color = if (isFavorite) Color(0xFFFFC46B) else AudioLabPalette.Muted,
                        fontSize = 18.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (tool.isAvailable) Color(0xFF103D39) else Color(0xFF242D40))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (tool.isAvailable) localized("ATIVA", "READY", isPt)
                        else localized("EM OBRA", "IN DEV", isPt),
                        color = if (tool.isAvailable) AudioLabPalette.Teal else Color(0xFF9EABC4),
                        fontSize = 7.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                tool.title(isPt),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                tool.description(isPt),
                color = AudioLabPalette.Muted,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isPt) tool.category.titlePt.uppercase() else tool.category.titleEn.uppercase(),
                    color = tint,
                    fontSize = 7.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text("↗", color = tint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProcessingHubCard(isPt: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111E34)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28466D))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(47.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(AudioLabPalette.Blue, AudioLabPalette.Violet))),
                contentAlignment = Alignment.Center
            ) {
                Text("◷", color = Color.White, fontSize = 25.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(localized("Centro de processamento", "Processing centre", isPt),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(localized("Fila, progresso, resultados e tarefas anteriores.",
                    "Queue, progress, results and previous jobs.", isPt),
                    color = AudioLabPalette.Muted, fontSize = 10.sp)
            }
            Text("→", color = AudioLabPalette.Cyan, fontSize = 22.sp)
        }
    }
}

@Composable
private fun PrivacyNote(isPt: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0B1424))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◈", color = AudioLabPalette.Teal, fontSize = 17.sp)
        Spacer(Modifier.width(9.dp))
        Text(
            localized(
                "Processamento local sempre que possível. Os ficheiros originais são preservados.",
                "Local processing whenever possible. Original files are preserved.",
                isPt
            ),
            color = AudioLabPalette.Muted,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun EmptySearchState(isPt: Boolean, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⌕", color = AudioLabPalette.Cyan, fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(localized("Nenhuma ferramenta encontrada", "No tools found", isPt),
                color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Button(onClick = onClear) {
                Text(localized("Limpar pesquisa", "Clear search", isPt))
            }
        }
    }
}

private fun categoryTint(category: ToolCategory): Color = when (category) {
    ToolCategory.EDIT -> Color(0xFF27A8FF)
    ToolCategory.CONVERT -> Color(0xFF23D0B5)
    ToolCategory.CLEAN -> Color(0xFFAE7BFF)
    ToolCategory.EFFECTS -> Color(0xFFFF79C6)
    ToolCategory.ANALYZE -> Color(0xFF49C9F2)
    ToolCategory.METADATA -> Color(0xFFFFB15E)
    ToolCategory.BATCH -> Color(0xFFFF70BA)
}
