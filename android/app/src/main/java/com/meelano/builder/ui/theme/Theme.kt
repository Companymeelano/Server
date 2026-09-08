package com.meelano.builder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// YBee-like palette: deep purple-black + neon lavender + signal blue.
val Bg = Color(0xFF170B16)
val Surface = Color(0xFF241423)
val Surface2 = Color(0xFF33203C)
val Line = Color(0xFF3A1E44)
val Accent = Color(0xFFD9A7E6)
val Dim = Color(0xFF9C6BA8)
val Txt = Color(0xFFF2D9F7)
val Blue = Color(0xFF4AA3FF)
val Green = Color(0xFF7BD88F)
val Red = Color(0xFFFF9D9D)
val Yellow = Color(0xFFF2D060)

private val Scheme = darkColorScheme(
    background = Bg,
    onBackground = Txt,
    surface = Surface,
    onSurface = Txt,
    surfaceVariant = Surface2,
    onSurfaceVariant = Dim,
    primary = Accent,
    onPrimary = Bg,
    secondary = Blue,
    onSecondary = Color(0xFF0B1520),
    tertiary = Dim,
    outline = Line,
)

@Composable
fun BuilderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
