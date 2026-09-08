package com.meelano.builder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meelano.builder.data.CreateJobReq
import com.meelano.builder.data.Repository
import com.meelano.builder.data.Suggestion
import com.meelano.builder.ui.Brand
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.YbTopBar
import com.meelano.builder.ui.components.errText
import com.meelano.builder.ui.theme.Accent
import com.meelano.builder.ui.theme.Dim
import com.meelano.builder.ui.theme.Line
import com.meelano.builder.ui.theme.Surface
import kotlinx.coroutines.launch

/** Home = the YBee screen: headline, suggestion chips, idea box, footer. */
@Composable
fun HomeScreen(
    repo: Repository,
    serverUrl: String,
    lang: String,
    auto: Boolean,
    aiKey: String,
    aiBase: String,
    token: String,
    onOpenDrawer: () -> Unit,
    onPhone: () -> Unit,
    onBuilt: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var idea by remember { mutableStateOf("") }
    var pAndroid by remember { mutableStateOf(true) }
    var pWindows by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(fallbackSuggestions()) }

    LaunchedEffect(serverUrl, token) {
        try {
            val r = repo.apiFor(serverUrl, token).templates()
            if (r.suggestions.isNotEmpty()) suggestions = r.suggestions
        } catch (_: Exception) { /* offline: keep fallbacks */ }
    }

    fun build(withIdea: String) {
        val text = withIdea.trim()
        if (text.length < 3) { err = S[lang, "need_idea"]; return }
        val plats = if (auto) listOf("android", "windows")
        else buildList {
            if (pAndroid) add("android")
            if (pWindows) add("windows")
        }
        if (plats.isEmpty()) { err = S[lang, "need_platform"]; return }
        busy = true; err = ""
        scope.launch {
            try {
                val job = repo.apiFor(serverUrl, token).createJob(
                    CreateJobReq(text, plats, lang, "", aiKey, aiBase))
                busy = false
                onBuilt(job.id)
            } catch (e: Exception) {
                busy = false
                err = errText(lang, e)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        YbTopBar(lang, auto, onOpenDrawer, onPhone)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Text(
                S[lang, "headline"], color = Accent,
                fontSize = 34.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                lineHeight = 42.sp,
            )
            Spacer(Modifier.height(22.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(suggestions) { s ->
                    FilterChip(
                        selected = false, onClick = { build(if (lang == "fa") s.fa else s.en) },
                        label = { Text("${s.emoji} ${if (lang == "fa") s.fa else s.en}") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Surface, labelColor = Dim),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = false,
                            borderColor = Line),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = idea, onValueChange = { idea = it; err = "" },
                placeholder = { Text(S[lang, "hint"], color = Dim) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (busy) CircularProgressIndicator(color = Accent,
                        modifier = Modifier.padding(12.dp))
                    else IconButton(onClick = { build(idea) }) {
                        Icon(Icons.Filled.ArrowForward, "build", tint = Dim)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, unfocusedBorderColor = Line,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    cursorColor = Accent),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!auto) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(pAndroid, { pAndroid = !pAndroid },
                        label = { Text(S[lang, "platform_android"]) })
                    FilterChip(pWindows, { pWindows = !pWindows },
                        label = { Text(S[lang, "platform_windows"]) })
                }
            }
            if (err.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(err, color = Color(0xFFFF9D9D), fontSize = 13.sp,
                    textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            Text(if (lang == "fa") Brand.TEAM_FA else Brand.TEAM_EN,
                color = Dim, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun fallbackSuggestions() = listOf(
    Suggestion("📰", "Create a Blog reader app", "یک برنامه وبلاگ‌خوان بساز"),
    Suggestion("🎲", "Create a Dice roller app", "یک برنامه تاس بساز"),
    Suggestion("📝", "Create a Notes app", "یک برنامه یادداشت بساز"),
    Suggestion("🧮", "Create a Calculator app", "یک ماشین‌حساب بساز"),
    Suggestion("⛅", "Create a Weather app", "یک برنامه هواشناسی بساز"),
    Suggestion("🛍️", "Create a Mini Shop app", "یک فروشگاه کوچک بساز"),
)
