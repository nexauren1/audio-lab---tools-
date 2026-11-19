package com.nexauren.audiolabtools.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nexauren.audiolabtools.ui.screens.ToolsScreen
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette

private enum class Destination {
    HOME, TOOLS, PROCESSING, LIBRARY, SETTINGS, TOOL
}

@Composable
fun AudioLabApp() {
    val context = LocalContext.current
    val languagePreferences = context.getSharedPreferences("audio_lab_settings", Activity.MODE_PRIVATE)
    var showFirstLaunchLanguagePicker by rememberSaveable {
        mutableStateOf(!languagePreferences.contains("app_language"))
    }
    fun chooseLanguage(languageTag: String) {
        languagePreferences.edit().putString("app_language", languageTag).apply()
        showFirstLaunchLanguagePicker = false
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        (context as? Activity)?.recreate()
    }
    var destination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var previousDestination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var selectedToolId by rememberSaveable { mutableStateOf("audio-info") }
    var selectedFileUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFileName by rememberSaveable { mutableStateOf<String?>(null) }

    fun openTool(toolId: String, uri: String? = null, name: String? = null) {
        previousDestination = destination
        selectedToolId = toolId
        selectedFileUri = uri
        selectedFileName = name
        destination = Destination.TOOL.name
    }

    fun backFromTool() {
        destination = previousDestination
        selectedFileUri = null
        selectedFileName = null
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
                    containerColor = Color(0xFF090E1B),
                    tonalElevation = 0.dp
                ) {
                    val items = listOf(
                        Triple(Destination.HOME, "⌂", localized("Início", "Home")),
                        Triple(Destination.TOOLS, "▦", localized("Ferramentas", "Tools")),
                        Triple(Destination.LIBRARY, "♫", localized("Ficheiros", "Library")),
                        Triple(Destination.SETTINGS, "⚙", localized("Definições", "Settings"))
                    )
                    items.forEach { (target, symbol, label) ->
                        NavigationBarItem(
                            selected = current == target,
                            onClick = { destination = target.name },
                            icon = { Text(symbol) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AudioLabPalette.Violet,
                                selectedTextColor = AudioLabPalette.Violet,
                                indicatorColor = Color(0xFF21164B),
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
                    onOpenTools = { destination = Destination.TOOLS.name }
                )
                Destination.TOOLS -> ToolsScreen(
                    onOpenTool = { tool -> openTool(tool.id) }
                )
                Destination.PROCESSING -> ProcessingScreen()
                Destination.LIBRARY -> LibraryScreen(
                    onOpenFile = { uri, name -> openTool("audio-info", uri, name) }
                )
                Destination.SETTINGS -> SettingsScreen(onLanguageSelected = ::chooseLanguage)
                Destination.TOOL -> {
                    val tool = AudioToolCatalog.find(selectedToolId)
                    if (tool != null) {
                        ToolDetailScreen(
                            tool = tool,
                            initialUri = selectedFileUri,
                            initialName = selectedFileName,
                            onBack = ::backFromTool
                        )
                    }
                }
            }
        }
    if (showFirstLaunchLanguagePicker) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Choose your language", color = Color.White) },
            text = {
                Text(
                    "English is the default. Choose English or Portuguese to continue. You can change it later in Settings.",
                    color = AudioLabPalette.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { chooseLanguage("en") }) {
                    Text("English", color = AudioLabPalette.Cyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { chooseLanguage("pt") }) {
                    Text("Português", color = AudioLabPalette.Violet)
                }
            },
            containerColor = AudioLabPalette.Surface,
            titleContentColor = Color.White,
            textContentColor = AudioLabPalette.Muted
        )
    }

    }
}
