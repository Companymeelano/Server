package com.meelano.builder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meelano.builder.data.Repository
import com.meelano.builder.data.SettingsStore
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.CardBox
import com.meelano.builder.ui.theme.Accent
import com.meelano.builder.ui.theme.Bg
import com.meelano.builder.ui.theme.Dim
import com.meelano.builder.ui.theme.Line
import com.meelano.builder.ui.theme.Txt
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(store: SettingsStore, repo: Repository, lang: String) {
    val scope = rememberCoroutineScope()
    val serverUrl by store.serverUrl.collectAsStateWithLifecycle("…")
    val auto by store.auto.collectAsStateWithLifecycle(true)
    val aiKey by store.aiKey.collectAsStateWithLifecycle("")
    val aiBase by store.aiBase.collectAsStateWithLifecycle("")

    var urlDraft by remember { mutableStateOf("") }
    var keyDraft by remember { mutableStateOf("") }
    var baseDraft by remember { mutableStateOf("") }
    var testMsg by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, aiKey, aiBase) {
        if (!loaded && serverUrl != "…") {
            urlDraft = serverUrl; keyDraft = aiKey; baseDraft = aiBase
            loaded = true
        }
    }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Accent, unfocusedBorderColor = Line,
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        cursorColor = Accent)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("⚙️ ${S[lang, "settings"]}", color = Accent,
            fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))

        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "server_url"], color = Txt, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(urlDraft, { urlDraft = it }, singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = tfColors,
                modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            store.setServer(urlDraft.ifBlank { SettingsStore.DEFAULT_SERVER })
                            testMsg = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent, contentColor = Bg),
                ) { Text("✓") }
                OutlinedButton(onClick = {
                    scope.launch {
                        testMsg = "…"
                        testMsg = try {
                            val h = repo.apiFor(
                                urlDraft.ifBlank { SettingsStore.DEFAULT_SERVER }).health()
                            "${S[lang, "connected"]} · v${h.version}" +
                                (if (h.ai) " · 🤖AI" else "") +
                                (if (h.cloud_build) " · ☁️" else "")
                        } catch (e: Exception) {
                            S[lang, "conn_fail"] + " (${e.message})"
                        }
                    }
                }) { Text(S[lang, "test"], color = Accent) }
            }
            if (testMsg.isNotEmpty()) Text(testMsg, color = Dim, fontSize = 13.sp)
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "language"], color = Txt, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.selectable(lang == "en") {
                    scope.launch { store.setLang("en") } }
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(lang == "en", onClick = {
                        scope.launch { store.setLang("en") } })
                    Text("English", color = Txt)
                }
                Row(Modifier.selectable(lang == "fa") {
                    scope.launch { store.setLang("fa") } }
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(lang == "fa", onClick = {
                        scope.launch { store.setLang("fa") } })
                    Text("فارسی", color = Txt)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(S[lang, "auto_mode"], color = Txt,
                    modifier = Modifier.weight(1f), fontSize = 14.sp)
                Switch(auto, { scope.launch { store.setAuto(it) } })
            }
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text("🤖 AI", color = Txt, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(keyDraft, { keyDraft = it }, singleLine = true,
                placeholder = { Text(S[lang, "ai_key"], color = Dim, fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp), colors = tfColors,
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(baseDraft, { baseDraft = it }, singleLine = true,
                placeholder = { Text(S[lang, "ai_base"], color = Dim, fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp), colors = tfColors,
                modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    scope.launch { store.setAiKey(keyDraft); store.setAiBase(baseDraft) }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent, contentColor = Bg),
            ) { Text("✓") }
            Text(S[lang, "ai_hint"], color = Dim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}
