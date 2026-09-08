package com.meelano.builder.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import com.meelano.builder.data.Repository
import com.meelano.builder.ui.Brand
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.CardBox
import com.meelano.builder.ui.components.errText
import com.meelano.builder.ui.theme.Pal
import com.meelano.builder.util.ApkInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private data class UpdateInfo(val name: String, val url: String, val notes: String)

@Composable
fun AboutScreen(lang: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var avail by remember { mutableStateOf<UpdateInfo?>(null) }
    var dlPct by remember { mutableStateOf(-2) } // -2 idle, -1 unknown, 0..100

    fun check() {
        busy = true; msg = ""; avail = null
        scope.launch(Dispatchers.IO) {
            try {
                val o = JSONObject(Repository().fetchText(Brand.UPDATE_URL))
                val code = o.optInt("version_code", 0)
                val cur = PackageInfoCompat.getLongVersionCode(
                    ctx.packageManager.getPackageInfo(ctx.packageName, 0)).toInt()
                main.post {
                    busy = false
                    if (code > cur) {
                        avail = UpdateInfo(
                            o.optString("version_name", "?"),
                            o.optString("apk_url", ""),
                            if (lang == "fa") o.optString("notes_fa", "")
                            else o.optString("notes_en", ""))
                    } else {
                        msg = S[lang, "update_latest"]
                    }
                }
            } catch (e: Exception) {
                main.post { busy = false; msg = errText(lang, e) }
            }
        }
    }

    fun download(u: UpdateInfo) {
        if (u.url.isEmpty()) return
        dlPct = 0
        scope.launch(Dispatchers.IO) {
            try {
                val f = ApkInstaller.download(
                    ctx, u.url, "MeeLano-Builder-update.apk", "") { p ->
                    main.post { dlPct = p }
                }
                main.post { ApkInstaller.installApk(ctx, f) }
            } catch (e: Exception) {
                main.post { dlPct = -2; msg = errText(lang, e) }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔨 ${Brand.FULL} v1.0", color = Pal.accent,
            fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))
        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "about_text"], color = Pal.txt, fontSize = 14.sp,
                lineHeight = 22.sp)
            Text("• 🤖 Android APK — " +
                if (lang == "fa") "نصب مستقیم روی گوشی"
                else "direct install on any phone",
                color = Pal.dim, fontSize = 13.sp)
            Text("• 🪟 Windows EXE + Setup — " +
                if (lang == "fa") "فایل نصب ویندوز"
                else "installer for Windows",
                color = Pal.dim, fontSize = 13.sp)
            Text("• 📦 " +
                if (lang == "fa") "سورس‌کد + پیش‌نمایش زنده"
                else "Sources + live preview",
                color = Pal.dim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "update_title"], color = Pal.txt,
                fontWeight = FontWeight.SemiBold)
            Button(
                onClick = ::check,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent, contentColor = Pal.bg),
            ) {
                Text(if (busy) S[lang, "update_checking"]
                else S[lang, "update_check"])
            }
            if (msg.isNotEmpty()) Text(msg, color = Pal.dim, fontSize = 13.sp)
            val u = avail
            if (u != null) {
                Text("${S[lang, "update_avail"]} v${u.name}", color = Pal.txt,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (u.notes.isNotEmpty()) {
                    Text(u.notes, color = Pal.dim, fontSize = 13.sp)
                }
                Button(
                    onClick = { download(u) },
                    enabled = dlPct < 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Pal.accent, contentColor = Pal.bg),
                ) {
                    Text(if (dlPct < 0) S[lang, "update_download"]
                    else if (dlPct >= 0) " ${dlPct}%" else " …")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (lang == "fa") Brand.TEAM_FA else Brand.TEAM_EN,
            color = Pal.dim, fontSize = 13.sp)
    }
}
