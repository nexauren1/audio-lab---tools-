package com.nexauren.audiolabtools.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AudioLabColors = darkColorScheme(
    primary = Color(0xFF18C9FF),
    onPrimary = Color(0xFF00131D),
    secondary = Color(0xFF9B70FF),
    onSecondary = Color(0xFF160B2B),
    tertiary = Color(0xFF00D6B0),
    background = Color(0xFF070B16),
    onBackground = Color(0xFFF4F7FF),
    surface = Color(0xFF10182A),
    onSurface = Color(0xFFF1F5FF),
    surfaceVariant = Color(0xFF1A2540),
    onSurfaceVariant = Color(0xFFADB9D0),
    error = Color(0xFFFF647C)
)

@Composable
fun AudioLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AudioLabColors,
        content = content
    )
}

object AudioLabPalette {
    val Background = Color(0xFF070B16)
    val Surface = Color(0xFF10182A)
    val SurfaceRaised = Color(0xFF151F35)
    val Cyan = Color(0xFF18C9FF)
    val Blue = Color(0xFF236BFF)
    val Violet = Color(0xFF8A4DFF)
    val Teal = Color(0xFF00C9A7)
    val Muted = Color(0xFF9EABC4)
    val Border = Color(0xFF273657)
}
