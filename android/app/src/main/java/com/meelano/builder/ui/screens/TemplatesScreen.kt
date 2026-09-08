package com.meelano.builder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meelano.builder.data.CreateJobReq
import com.meelano.builder.data.Repository
import com.meelano.builder.data.TemplateInfo
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.errText
import com.meelano.builder.ui.theme.Accent
import com.meelano.builder.ui.theme.Bg
import com.meelano.builder.ui.theme.Dim
import com.meelano.builder.ui.theme.Surface
import com.meelano.builder.ui.theme.Txt
import kotlinx.coroutines.launch

@Composable
fun TemplatesScreen(
    repo: Repository,
    serverUrl: String,
    lang: String,
    auto: Boolean,
    aiKey: String,
    aiBase: String,
    token: String,
    onBuilt: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<TemplateInfo>>(emptyList()) }
    var busy by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    LaunchedEffect(serverUrl, token) {
        try {
            list = repo.apiFor(serverUrl, token).templates().templates
        } catch (e: Exception) {
            err = errText(lang, e)
        }
    }

    fun build(t: TemplateInfo) {
        busy = t.id; err = ""
        val idea = if (lang == "fa") "یک ${t.name_fa} بساز"
        else "Create a ${t.name_en} app"
        scope.launch {
            try {
                val plats = if (auto) listOf("android", "windows")
                else listOf("android", "windows")
                val job = repo.apiFor(serverUrl, token).createJob(
                    CreateJobReq(idea, plats, lang, "", aiKey, aiBase))
                busy = ""
                onBuilt(job.id)
            } catch (e: Exception) {
                busy = ""
                err = errText(lang, e)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("⭐ ${S[lang, "templates"]}", color = Accent,
            fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        if (err.isNotEmpty()) Text(err, color = Dim, fontSize = 13.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list.filter { it.id != "starter" }) { t ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Surface).padding(14.dp)
                ) {
                    Text(t.emoji, fontSize = 34.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(if (lang == "fa") t.name_fa else t.name_en,
                            color = Txt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (lang == "fa") t.desc_fa else t.desc_en,
                            color = Dim, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { build(t) },
                            enabled = busy.isEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent, contentColor = Bg),
                        ) {
                            Text(if (busy == t.id) "…" else S[lang, "build_this"])
                        }
                    }
                }
            }
        }
    }
}
