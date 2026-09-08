package com.meelano.builder.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meelano.builder.data.Step
import com.meelano.builder.ui.theme.Pal

/** Parse a #hex accent safely (falls back to the theme accent). */
@Composable
fun accentOf(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Pal.accent
}

/** Slow-breathing aurora glow behind the home screen. */
@Composable
fun GlowingBackground() {
    val inf = rememberInfiniteTransition(label = "glow")
    val a by inf.animateFloat(
        0.10f, 0.20f,
        infiniteRepeatable(tween(5000), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxSize().background(Pal.bg)) {
        Box(Modifier.size(420.dp).offset((-120).dp, (-100).dp).background(
            Brush.radialGradient(
                listOf(Pal.accent.copy(alpha = a), Color.Transparent))))
        Box(Modifier.size(380.dp).align(Alignment.BottomEnd)
            .offset(120.dp, 140.dp).background(
                Brush.radialGradient(
                    listOf(Pal.blue.copy(alpha = a * 0.8f), Color.Transparent))))
    }
}

private fun stepTitle(name: String) = when (name) {
    "analyze" -> "🔍 analyze"
    "generate" -> "🧠 generate"
    "package" -> "📦 package"
    "windows" -> "🪟 windows"
    "android" -> "🤖 android"
    "cloud" -> "☁️ cloud"
    else -> name
}

/** Animated build-steps timeline (✓ done / ● running / ✕ failed). */
@Composable
fun StepTimeline(steps: List<Step>) {
    if (steps.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEach { s ->
            val icon = when (s.state) {
                "ok" -> "✓"
                "fail" -> "✕"
                else -> "●"
            }
            val color = when (s.state) {
                "ok" -> Pal.green
                "fail" -> Pal.red
                else -> Pal.accent
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = color, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Text(stepTitle(s.name), color = Pal.txt, fontSize = 13.sp)
            }
        }
    }
}
