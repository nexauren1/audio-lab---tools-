package com.nexauren.audiolabtools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
fun ToolsScreen(onOpenTool: (AudioTool) -> Unit) {
    val isPt = isPortuguese()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }

    val filters = listOf(
        Triple("ALL", localized("Tudo", "All", isPt), null),
        Triple("EDIT", localized("Editar", "Edit", isPt), ToolCategory.EDIT),
        Triple("CONVERT", localized("Converter", "Convert", isPt), ToolCategory.CONVERT),
        Triple("CLEAN", localized("Melhorar", "Enhance", isPt), ToolCategory.CLEAN),
        Triple("ANALYZE", localized("Analisar", "Analyze", isPt), ToolCategory.ANALYZE),
        Triple("METADATA", localized("Metadados", "Metadata", isPt), ToolCategory.METADATA)
    )

    val visibleTools = AudioToolCatalog.tools.filter { tool ->
        val matchesCategory = selectedCategory == "ALL" || tool.category.name == selectedCategory
        val matchesQuery = query.isBlank() ||
            tool.title(isPt).contains(query, ignoreCase = true) ||
            tool.description(isPt).contains(query, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp)
            .padding(top = 18.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    localized("Todas as ferramentas", "All Tools", isPt),
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    localized("Escolhe uma ferramenta para começar.", "Choose a tool to get started.", isPt),
                    color = AudioLabPalette.Muted,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(AudioLabPalette.Violet, AudioLabPalette.Blue))),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = Color.White, fontSize = 23.sp)
            }
        }

        Spacer(Modifier.height(19.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(localized("Pesquisar ferramentas...", "Search tools...", isPt), color = AudioLabPalette.Muted)
            },
            leadingIcon = { Text("⌕", color = AudioLabPalette.Cyan, fontSize = 23.sp) },
            shape = RoundedCornerShape(17.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AudioLabPalette.Violet,
                unfocusedBorderColor = AudioLabPalette.Border,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AudioLabPalette.Cyan,
                focusedContainerColor = Color(0xFF11182A),
                unfocusedContainerColor = Color(0xFF11182A)
            )
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (key, label, _) ->
                FilterChip(
                    selected = selectedCategory == key,
                    onClick = { selectedCategory = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(22.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AudioLabPalette.Violet,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF161D31),
                        labelColor = Color(0xFFCCD5E7)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == key,
                        borderColor = AudioLabPalette.Border,
                        selectedBorderColor = AudioLabPalette.Violet
                    )
                )
            }
        }

        Spacer(Modifier.height(19.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (selectedCategory == "ALL") localized("Catálogo completo", "Complete catalogue", isPt)
                else filters.firstOrNull { it.first == selectedCategory }?.second.orEmpty(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                visibleTools.size.toString() + " " + localized("ferramentas", "tools", isPt),
                color = AudioLabPalette.Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (visibleTools.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AudioLabPalette.Surface)
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⌕", color = AudioLabPalette.Cyan, fontSize = 34.sp)
                    Text(
                        localized("Nenhuma ferramenta encontrada", "No tools found", isPt),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        localized("Experimenta outro termo de pesquisa.", "Try another search term.", isPt),
                        color = AudioLabPalette.Muted,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
            visibleTools.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    pair.forEach { tool ->
                        ToolCatalogueCard(
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

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF111B2F)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ⓘ", color = AudioLabPalette.Cyan, fontSize = 19.sp)
            Spacer(Modifier.width(9.dp))
            Text(
                localized(
                    "As ferramentas ainda em desenvolvimento aparecem identificadas como «Em breve».",
                    "Tools still in development are marked as “Coming soon”.",
                    isPt
                ),
                color = AudioLabPalette.Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun ToolCatalogueCard(
    tool: AudioTool,
    isPt: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint = when (tool.category) {
        ToolCategory.EDIT -> Color(0xFFFF3F8F)
        ToolCategory.CONVERT -> Color(0xFF12BFFF)
        ToolCategory.CLEAN -> Color(0xFF8F55FF)
        ToolCategory.ANALYZE -> Color(0xFF14D0C1)
        ToolCategory.METADATA -> Color(0xFFFF8A3D)
        ToolCategory.BATCH -> Color(0xFFE34CA7)
    }
    Card(
        onClick = onClick,
        modifier = modifier.height(137.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12192B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.25f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(43.dp).clip(RoundedCornerShape(13.dp))
                        .background(tint.copy(alpha = 0.17f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tool.symbol, color = tint, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text("›", color = Color(0xFFB8C6DF), fontSize = 23.sp)
            }
            Spacer(Modifier.height(9.dp))
            Text(
                tool.title(isPt),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                tool.description(isPt),
                color = Color(0xFFABB8D0),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (tool.isAvailable) localized("PRONTA", "READY", isPt)
                else localized("EM BREVE", "COMING SOON", isPt),
                color = if (tool.isAvailable) Color(0xFF28D5B0) else Color(0xFF929DB4),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
