package com.meelano.builder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Per-theme palette: every screen reads colors from [Pal]. */
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val accent: Color,
    val dim: Color,
    val txt: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val yellow: Color,
)

/** بنفش کهکشانی (default, YBee-like) */
val Galaxy = Palette(
    bg = Color(0xFF170B16),
    surface = Color(0xFF241423),
    surface2 = Color(0xFF33203C),
    line = Color(0xFF3A1E44),
    accent = Color(0xFFD9A7E6),
    dim = Color(0xFF9C6BA8),
    txt = Color(0xFFF2D9F7),
    blue = Color(0xFF4AA3FF),
    green = Color(0xFF7BD88F),
    red = Color(0xFFFF9D9D),
    yellow = Color(0xFFF2D060),
)

/** آبی نئون */
val NeonBlue = Palette(
    bg = Color(0xFF070D1A),
    surface = Color(0xFF0E1930),
    surface2 = Color(0xFF16263F),
    line = Color(0xFF1E3A5F),
    accent = Color(0xFF6FC3FF),
    dim = Color(0xFF5B7FA6),
    txt = Color(0xFFD9EBFF),
    blue = Color(0xFF4AA3FF),
    green = Color(0xFF7BD88F),
    red = Color(0xFFFF9D9D),
    yellow = Color(0xFFF2D060),
)

/** غروب */
val Sunset = Palette(
    bg = Color(0xFF1A0B08),
    surface = Color(0xFF2A1410),
    surface2 = Color(0xFF3D1F16),
    line = Color(0xFF5C2E1E),
    accent = Color(0xFFFF9D6F),
    dim = Color(0xFFA86B4E),
    txt = Color(0xFFFFE3D3),
    blue = Color(0xFF4AA3FF),
    green = Color(0xFF7BD88F),
    red = Color(0xFFFF9D9D),
    yellow = Color(0xFFF2D060),
)

/** زمرد */
val Emerald = Palette(
    bg = Color(0xFF081410),
    surface = Color(0xFF0E241C),
    surface2 = Color(0xFF15352A),
    line = Color(0xFF1E4D3A),
    accent = Color(0xFF7BD88F),
    dim = Color(0xFF5B8A6E),
    txt = Color(0xFFD9F2E2),
    blue = Color(0xFF4AA3FF),
    green = Color(0xFF7BD88F),
    red = Color(0xFFFF9D9D),
    yellow = Color(0xFFF2D060),
)

val THEMES = mapOf(
    "galaxy" to Galaxy,
    "neon" to NeonBlue,
    "sunset" to Sunset,
    "emerald" to Emerald,
)

val LocalPal = staticCompositionLocalOf { Galaxy }

/** Current palette inside any @Composable. */
val Pal: Palette
    @Composable get() = LocalPal.current

@Composable
fun BuilderTheme(themeId: String = "galaxy", content: @Composable () -> Unit) {
    val p = THEMES[themeId] ?: Galaxy
    val scheme = darkColorScheme(
        background = p.bg,
        onBackground = p.txt,
        surface = p.surface,
        onSurface = p.txt,
        surfaceVariant = p.surface2,
        onSurfaceVariant = p.dim,
        primary = p.accent,
        onPrimary = p.bg,
        secondary = p.blue,
        onSecondary = Color(0xFF0B1520),
        tertiary = p.dim,
        outline = p.line,
    )
    CompositionLocalProvider(LocalPal provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
