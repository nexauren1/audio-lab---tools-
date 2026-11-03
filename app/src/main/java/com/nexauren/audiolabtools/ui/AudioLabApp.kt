package com.nexauren.audiolabtools.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexauren.audiolabtools.data.AudioToolCatalog
import com.nexauren.audiolabtools.data.localized
import com.nexauren.audiolabtools.ui.screens.FavoritesScreen
import com.nexauren.audiolabtools.ui.screens.HomeScreen
import com.nexauren.audiolabtools.ui.screens.LibraryScreen
import com.nexauren.audiolabtools.ui.screens.ProcessingScreen
import com.nexauren.audiolabtools.ui.screens.SettingsScreen
import com.nexauren.audiolabtools.ui.screens.ToolDetailScreen
import com.nexauren.audiolabtools.ui.theme.AudioLabPalette

private enum class Destination {
    HOME, FAVORITES, PROCESSING, LIBRARY, SETTINGS, TOOL
}

@Composable
fun AudioLabApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("audio_lab_preferences", Context.MODE_PRIVATE)
    }
    var favoriteToolIds by remember {
        mutableStateOf(preferences.getStringSet("favorite_tools", emptySet())?.toSet() ?: emptySet())
    }
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

    fun toggleFavorite(toolId: String) {
        val updated = favoriteToolIds.toMutableSet()
        if (!updated.add(toolId)) updated.remove(toolId)
        favoriteToolIds = updated.toSet()
        preferences.edit().putStringSet("favorite_tools", updated.toSet()).apply()
    }

    BackHandler(enabled = destination == Destination.TOOL.name) {
        backFromTool()
    }

    val current = Destination.valueOf(destination)
    // A tool ID is part of the page key so moving directly between tools also animates.
    val pageKey = if (current == Destination.TOOL) {
        "TOOL:$selectedToolId"
    } else {
        current.name
    }

    Scaffold(
        containerColor = AudioLabPalette.Background,
        bottomBar = {
            AnimatedVisibility(
                visible = current != Destination.TOOL,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(220)
                ) + fadeIn(animationSpec = tween(180)),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(160)
                ) + fadeOut(animationSpec = tween(140))
            ) {
                NavigationBar(
                    containerColor = Color(0xFF0A1120),
                    tonalElevation = 0.dp
                ) {
                    val items = listOf(
                        Triple(Destination.HOME, "⌂", localized("Início", "Home")),
                        Triple(Destination.FAVORITES, "★", localized("Favoritos", "Saved")),
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
        AnimatedContent(
            targetState = pageKey,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(230)) +
                        slideInHorizontally(
                            initialOffsetX = { width -> width / 14 },
                            animationSpec = tween(230)
                        ) +
                        scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(230)
                        )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(160)) +
                        slideOutHorizontally(
                            targetOffsetX = { width -> -width / 18 },
                            animationSpec = tween(160)
                        ) +
                        scaleOut(
                            targetScale = 0.992f,
                            animationSpec = tween(160)
                        )
                    ) using SizeTransform(clip = false)
            },
            label = "audio_lab_page_transition"
        ) { animatedPageKey ->
            val screen = Destination.valueOf(animatedPageKey.substringBefore(':'))
            val screenToolId = animatedPageKey
                .takeIf { it.startsWith("TOOL:") }
                ?.substringAfter(':')
                ?: selectedToolId

            Box(modifier = Modifier.fillMaxSize()) {
                when (screen) {
                    Destination.HOME -> HomeScreen(
                        onOpenTool = { tool -> openTool(tool.id) },
                        onOpenProcessing = { destination = Destination.PROCESSING.name },
                        onOpenLibrary = { destination = Destination.LIBRARY.name },
                        favoriteToolIds = favoriteToolIds,
                        onToggleFavorite = ::toggleFavorite
                    )
                    Destination.FAVORITES -> FavoritesScreen(
                        favoriteToolIds = favoriteToolIds,
                        onOpenTool = { tool -> openTool(tool.id) },
                        onToggleFavorite = ::toggleFavorite
                    )
                    Destination.PROCESSING -> ProcessingScreen()
                    Destination.LIBRARY -> LibraryScreen(
                        onOpenFile = { uri, name -> openTool("audio-info", uri, name) }
                    )
                    Destination.SETTINGS -> SettingsScreen()
                    Destination.TOOL -> {
                        val tool = AudioToolCatalog.find(screenToolId)
                        if (tool != null) {
                            ToolDetailScreen(
                                tool = tool,
                                initialUri = if (screenToolId == selectedToolId) selectedFileUri else null,
                                initialName = if (screenToolId == selectedToolId) selectedFileName else null,
                                onSourceSelected = { uri, name ->
                                    if (screenToolId == selectedToolId) {
                                        selectedFileUri = uri
                                        selectedFileName = name
                                    }
                                },
                                onBack = ::backFromTool
                            )
                        }
                    }
                }
            }
        }
    }
}
