package com.nexauren.audiolabtools.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexauren.audiolabtools.data.AudioToolCatalog
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.ui.screens.HomeScreen
import com.nexauren.audiolabtools.ui.screens.LibraryScreen
import com.nexauren.audiolabtools.ui.screens.ProcessingScreen
import com.nexauren.audiolabtools.ui.screens.SettingsScreen
import com.nexauren.audiolabtools.ui.screens.ToolDetailScreen
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette

private enum class Destination {
    HOME, PROCESSING, LIBRARY, SETTINGS, TOOL
}

@Composable
fun AudioLabApp() {
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var previousDestination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var selectedToolId by rememberSaveable { mutableStateOf("audio-info") }
    var selectedFileUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFileName by rememberSaveable { mutableStateOf<String?>(null) }

    fun openTool(toolId: String, uri: String? = null, name: String? = null) {
        previousDestination = destination
        val returningToSameTool = selectedToolId == toolId
        selectedToolId = toolId
        when {
            uri != null -> {
                selectedFileUri = uri
                selectedFileName = name
            }
            !returningToSameTool -> {
                selectedFileUri = null
                selectedFileName = null
            }
            // Reopening the same tool restores its last selected source.
        }
        destination = Destination.TOOL.name
    }

    fun backFromTool() {
        // Keep the last selected source so returning to this tool restores its setup.
        // Leaving a tool must never restart or cancel its WorkManager job.
        destination = previousDestination
    }

    BackHandler(enabled = destination == Destination.TOOL.name) {
        backFromTool()
    }

    val current = Destination.valueOf(destination)

    Scaffold(
        containerColor = AudioLabPalette.Background,
        bottomBar = {
            if (current != Destination.TOOL) {
                NavigationBar(
                    containerColor = Color(0xFF0A1120),
                    tonalElevation = 0.dp
                ) {
                    val items = listOf(
                        Triple(Destination.HOME, "⌂", localized("Início", "Home")),
                        Triple(Destination.PROCESSING, "◷", localized("Processos", "Queue")),
                        Triple(Destination.LIBRARY, "♫", localized("Ficheiros", "Files")),
                        Triple(Destination.SETTINGS, "⚙", localized("Definições", "Settings"))
                    )
                    items.forEach { (target, symbol, label) ->
                        NavigationBarItem(
                            selected = current == target,
                            onClick = { destination = target.name },
                            icon = { Text(symbol) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AudioLabPalette.Cyan,
                                selectedTextColor = AudioLabPalette.Cyan,
                                indicatorColor = Color(0xFF14304B),
                                unselectedIconColor = AudioLabPalette.Muted,
                                unselectedTextColor = AudioLabPalette.Muted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (current) {
                Destination.HOME -> HomeScreen(
                    onOpenTool = { tool -> openTool(tool.id) },
                    onOpenProcessing = { destination = Destination.PROCESSING.name },
                    onOpenLibrary = { destination = Destination.LIBRARY.name }
                )
                Destination.PROCESSING -> ProcessingScreen()
                Destination.LIBRARY -> LibraryScreen(
                    onOpenFile = { uri, name -> openTool("audio-info", uri, name) }
                )
                Destination.SETTINGS -> SettingsScreen()
                Destination.TOOL -> {
                    val tool = AudioToolCatalog.find(selectedToolId)
                    if (tool != null) {
                        ToolDetailScreen(
                            tool = tool,
                            initialUri = selectedFileUri,
                            initialName = selectedFileName,
                            onSourceSelected = { uri, name ->
                                selectedFileUri = uri
                                selectedFileName = name
                            },
                            onBack = ::backFromTool
                        )
                    }
                }
            }
        }
    }
}
