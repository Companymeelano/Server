package com.meelano.builder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.meelano.builder.data.Repository
import com.meelano.builder.data.SettingsStore
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.CardBox
import com.meelano.builder.ui.components.errText
import com.meelano.builder.ui.theme.Pal
import com.meelano.builder.util.NsdHelper
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SettingsScreen(store: SettingsStore, repo: Repository, lang: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }
    val serverUrl by store.serverUrl.collectAsStateWithLifecycle("…")
    val auto by store.auto.collectAsStateWithLifecycle(true)
    val aiKey by store.aiKey.collectAsStateWithLifecycle("")
    val aiBase by store.aiBase.collectAsStateWithLifecycle("")
    val token by store.token.collectAsStateWithLifecycle("")
    val theme by store.theme.collectAsStateWithLifecycle("galaxy")

    var urlDraft by remember { mutableStateOf("") }
    var keyDraft by remember { mutableStateOf("") }
    var baseDraft by remember { mutableStateOf("") }
    var tokenDraft by remember { mutableStateOf("") }
    var testMsg by remember { mutableStateOf("") }
    var scanMsg by remember { mutableStateOf("") }
    var nsdBusy by remember { mutableStateOf(false) }
    var nsdUrls by remember { mutableStateOf(listOf<String>()) }
    var nsdDone by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, aiKey, aiBase, token) {
        if (!loaded && serverUrl != "…") {
            urlDraft = serverUrl; keyDraft = aiKey; baseDraft = aiBase
            tokenDraft = token
            loaded = true
        }
    }

    fun scanOptions(): ScanOptions = ScanOptions()
        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        .setPrompt(if (lang == "fa") "کد QR سرور را بگیر" else "Point at the server QR")
        .setBeepEnabled(false)
        .setOrientationLocked(false)

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { res ->
        val c = res.contents ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val o = JSONObject(c)
                val u = o.optString("url", "")
                val tk = o.optString("token", "")
                if (u.startsWith("http")) {
                    store.setServer(u)
                    if (tk.isNotEmpty()) store.setToken(tk)
                    urlDraft = u
                    if (tk.isNotEmpty()) tokenDraft = tk
                    scanMsg = "✓ $u"
                } else scanMsg = "?"
            } catch (_: Exception) {
                if (c.startsWith("http")) {
                    store.setServer(c)
                    urlDraft = c
                    scanMsg = "✓ $c"
                } else scanMsg = "?"
            }
        }
    }
    val camPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanLauncher.launch(scanOptions())
        else scanMsg = S[lang, "camera_denied"]
    }
    fun scan() {
        scanMsg = ""
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            scanLauncher.launch(scanOptions())
        } else {
            camPerm.launch(Manifest.permission.CAMERA)
        }
    }

    fun findServers() {
        nsdBusy = true; nsdDone = false; nsdUrls = emptyList()
        NsdHelper.discover(ctx) { urls ->
            main.post { nsdUrls = urls }
        }
        main.postDelayed({
            nsdBusy = false; nsdDone = true
        }, 9500)
    }

    // Debug builds ship without R8, so the scanner Activity referenced via
    // ActivityResult needs no extra keep rules.

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Pal.accent, unfocusedBorderColor = Pal.line,
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        cursorColor = Pal.accent)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("⚙️ ${S[lang, "settings"]}", color = Pal.accent,
            fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))

        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "srv_help_title"], color = Pal.txt,
                fontWeight = FontWeight.SemiBold)
            Text(S[lang, "srv_help_body"], color = Pal.dim, fontSize = 13.sp,
                lineHeight = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = ::scan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Pal.accent, contentColor = Pal.bg),
                ) { Text(S[lang, "scan_qr"]) }
            }
            Text(S[lang, "scan_hint"], color = Pal.dim, fontSize = 12.sp)
            if (scanMsg.isNotEmpty()) {
                Text(scanMsg, color = Pal.green, fontSize = 13.sp)
            }
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "server_url"], color = Pal.txt, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(urlDraft, { urlDraft = it }, singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = tfColors,
                modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            store.setServer(urlDraft.ifBlank { SettingsStore.DEFAULT_SERVER })
                            store.setToken(tokenDraft)
                            testMsg = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Pal.accent, contentColor = Pal.bg),
                ) { Text("✓") }
                OutlinedButton(onClick = {
                    scope.launch {
                        testMsg = "…"
                        testMsg = try {
                            val h = repo.apiFor(
                                urlDraft.ifBlank { SettingsStore.DEFAULT_SERVER },
                                tokenDraft).health()
                            "${S[lang, "connected"]} · v${h.version}" +
                                (if (h.ai) " · 🤖AI" else "") +
                                (if (h.cloud_build) " · ☁️" else "")
                        } catch (e: Exception) {
                            errText(lang, e)
                        }
                    }
                }) { Text(S[lang, "test"], color = Pal.accent) }
            }
            if (testMsg.isNotEmpty()) Text(testMsg, color = Pal.dim, fontSize = 13.sp)
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "nsd_title"], color = Pal.txt, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = ::findServers,
                enabled = !nsdBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent, contentColor = Pal.bg),
            ) { Text(if (nsdBusy) S[lang, "nsd_finding"] else S[lang, "nsd_find"]) }
            if (nsdUrls.isNotEmpty()) {
                Text(S[lang, "nsd_tap"], color = Pal.dim, fontSize = 13.sp)
                nsdUrls.forEach { u ->
                    Text(u, color = Pal.accent, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable {
                                scope.launch { store.setServer(u) }
                                urlDraft = u
                            }.padding(6.dp))
                }
            } else if (nsdDone) {
                Text(S[lang, "nsd_none"], color = Pal.dim, fontSize = 13.sp)
            }
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text("🔑 ${S[lang, "api_token"]}", color = Pal.txt,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            OutlinedTextField(tokenDraft, { tokenDraft = it }, singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = tfColors,
                modifier = Modifier.fillMaxWidth())
            Text(S[lang, "api_token_hint"], color = Pal.dim, fontSize = 12.sp)
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "theme_title"], color = Pal.txt,
                fontWeight = FontWeight.SemiBold)
            ThemeRow("galaxy", S[lang, "theme_galaxy"], "#D9A7E6", theme) {
                scope.launch { store.setTheme("galaxy") }
            }
            ThemeRow("neon", S[lang, "theme_neon"], "#6FC3FF", theme) {
                scope.launch { store.setTheme("neon") }
            }
            ThemeRow("sunset", S[lang, "theme_sunset"], "#FF9D6F", theme) {
                scope.launch { store.setTheme("sunset") }
            }
            ThemeRow("emerald", S[lang, "theme_emerald"], "#7BD88F", theme) {
                scope.launch { store.setTheme("emerald") }
            }
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "language"], color = Pal.txt, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.selectable(lang == "en") {
                    scope.launch { store.setLang("en") } }
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(lang == "en", onClick = {
                        scope.launch { store.setLang("en") } })
                    Text("English", color = Pal.txt)
                }
                Row(Modifier.selectable(lang == "fa") {
                    scope.launch { store.setLang("fa") } }
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(lang == "fa", onClick = {
                        scope.launch { store.setLang("fa") } })
                    Text("فارسی", color = Pal.txt)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(S[lang, "auto_mode"], color = Pal.txt,
                    modifier = Modifier.weight(1f), fontSize = 14.sp)
                Switch(auto, { scope.launch { store.setAuto(it) } })
            }
        }

        CardBox(Modifier.fillMaxWidth()) {
            Text("🤖 AI", color = Pal.txt, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(keyDraft, { keyDraft = it }, singleLine = true,
                placeholder = { Text(S[lang, "ai_key"], color = Pal.dim, fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp), colors = tfColors,
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(baseDraft, { baseDraft = it }, singleLine = true,
                placeholder = { Text(S[lang, "ai_base"], color = Pal.dim, fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp), colors = tfColors,
                modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    scope.launch { store.setAiKey(keyDraft); store.setAiBase(baseDraft) }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent, contentColor = Pal.bg),
            ) { Text("✓") }
            Text(S[lang, "ai_hint"], color = Pal.dim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ThemeRow(
    id: String,
    name: String,
    dot: String,
    current: String,
    onPick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .selectable(id == current, onClick = onPick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(
            try {
                Color(android.graphics.Color.parseColor(dot))
            } catch (_: Exception) {
                Pal.accent
            }))
        Text(name, color = Pal.txt, modifier = Modifier.weight(1f), fontSize = 14.sp)
        RadioButton(id == current, onClick = onPick)
    }
}
