package com.meelano.builder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.meelano.builder.data.Artifact
import com.meelano.builder.data.Job
import com.meelano.builder.data.Repository
import com.meelano.builder.data.isTerminal
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.CardBox
import com.meelano.builder.ui.components.StatusBadge
import com.meelano.builder.ui.components.StepTimeline
import com.meelano.builder.ui.components.errText
import com.meelano.builder.ui.theme.Pal
import com.meelano.builder.util.ApkInstaller
import com.meelano.builder.util.QrImage
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Dl(val state: String, val pct: Int, val file: File? = null)

/** Live build: steps timeline + progress + logs + downloads + install + QR. */
@Composable
fun BuildScreen(
    repo: Repository,
    serverUrl: String,
    lang: String,
    token: String,
    jobId: String,
    onBack: () -> Unit,
    onPreview: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }
    val haptics = LocalHapticFeedback.current
    var job by remember { mutableStateOf<Job?>(null) }
    var err by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }
    var celebrated by remember { mutableStateOf(false) }
    var askedNotifs by remember { mutableStateOf(false) }
    var pendingInstall by remember { mutableStateOf<File?>(null) }
    val dls = remember { mutableStateMapOf<String, Dl>() }

    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(jobId, tick) {
        while (true) {
            try {
                val j = repo.apiFor(serverUrl, token).job(jobId)
                job = j
                err = ""
                if (j.isTerminal()) {
                    if (!celebrated) {
                        celebrated = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    break
                }
            } catch (e: Exception) {
                err = errText(lang, e)
            }
            delay(1500)
        }
    }

    // Ask for notification permission once while a build is active.
    LaunchedEffect(job?.status) {
        val j = job
        if (!askedNotifs && j != null && !j.isTerminal() &&
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifs = true
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Resume-install: if the user granted "unknown apps" in Settings and came
    // back, continue the pending install automatically.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, pendingInstall) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) {
                val f = pendingInstall
                if (f != null && f.exists() && ApkInstaller.canInstall(ctx)) {
                    pendingInstall = null
                    ApkInstaller.installApk(ctx, f)
                }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    fun download(a: Artifact) {
        if (dls[a.filename]?.state == "busy") return
        dls[a.filename] = Dl("busy", 0)
        scope.launch {
            try {
                val f = ApkInstaller.download(
                    ctx, repo.absolute(serverUrl, a.url), a.filename, token) { p ->
                    main.post { dls[a.filename] = Dl("busy", p) }
                }
                main.post { dls[a.filename] = Dl("ready", 100, f) }
            } catch (e: Exception) {
                main.post { dls[a.filename] = Dl("error", 0); err = errText(lang, e) }
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, S[lang, "back"], tint = Pal.dim)
            }
        }
        CardBox(Modifier.fillMaxWidth()) {
            val j = job
            AnimatedVisibility(visible = j?.status == "done") {
                Text("🎉", fontSize = 44.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
            Text(
                if (j == null || j.name.isEmpty()) S[lang, "building"]
                else "📦 ${j.name}",
                color = Pal.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (j != null) {
                Text("💡 ${j.idea}", color = Pal.dim, fontSize = 14.sp)
                StatusBadge(lang, j.status)
                LinearProgressIndicator(
                    progress = { j.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Pal.accent, trackColor = Pal.surface2)
                Text("${j.progress}%", color = Pal.dim, fontSize = 12.sp)
                StepTimeline(j.steps)
            }
            if (err.isNotEmpty()) {
                Text(err, color = Color(0xFFFF9D9D), fontSize = 13.sp)
                OutlinedButton(onClick = { err = ""; tick++ }) {
                    Text(S[lang, "retry"], color = Pal.accent)
                }
            }

            // Live logs
            val logs = job?.logs ?: emptyList()
            if (logs.isNotEmpty()) {
                val state = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) state.scrollToItem(logs.size - 1)
                }
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF0E060E))
                        .padding(10.dp),
                ) {
                    items(logs) { Text(it, color = Pal.txt, fontSize = 12.sp) }
                }
            }

            // Artifacts
            val arts = job?.artifacts ?: emptyList()
            if (arts.isNotEmpty()) {
                Text("⬇ ${S[lang, "download"]}", color = Pal.txt,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                arts.forEach { a ->
                    ArtifactRow(lang, a, dls[a.filename],
                        onDownload = { download(a) },
                        onInstall = {
                            val f = dls[a.filename]?.file
                                ?: ApkInstaller.destFile(ctx, a.filename)
                            if (f.exists()) {
                                if (!ApkInstaller.installApk(ctx, f)) {
                                    pendingInstall = f
                                }
                            } else download(a)
                        })
                }
                val apk = arts.firstOrNull { it.kind == "apk" }
                if (apk != null) {
                    Text(S[lang, "install_hint"], color = Pal.dim, fontSize = 12.sp)
                }
            }

            // Preview + QR
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onPreview(jobId) }) {
                    Text(S[lang, "preview"], color = Pal.accent)
                }
            }
            val shareable = (job?.artifacts ?: emptyList())
                .firstOrNull { it.kind == "apk" }
                ?: (job?.artifacts ?: emptyList()).firstOrNull { it.kind == "exe" }
            if (job?.isTerminal() == true && shareable != null) {
                Text(S[lang, "share_qr"], color = Pal.txt,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                QrImage(repo.absolute(serverUrl, shareable.url),
                    Modifier.size(180.dp).align(Alignment.CenterHorizontally))
                Text(shareable.filename, color = Pal.dim, fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ArtifactRow(
    lang: String,
    a: Artifact,
    dl: Dl?,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val ctx = LocalContext.current
    val exists = remember(dl) { ApkInstaller.destFile(ctx, a.filename).exists() }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E060E)).padding(12.dp)
    ) {
        Text("${iconFor(a.kind)} ${a.label}", color = Pal.txt, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold)
        Text(a.filename + if (a.size > 0) " • ${a.size / 1024} KB" else "",
            color = Pal.dim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent, contentColor = Pal.bg),
            ) {
                Icon(Icons.Filled.Download, null)
                Text(when {
                    dl?.state == "busy" && dl.pct >= 0 -> " ${dl.pct}%"
                    dl?.state == "busy" -> " …"
                    else -> " ${S[lang, "download"]}"
                })
            }
            if (a.kind == "apk" && (dl?.state == "ready" || exists)) {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E4D2B),
                        contentColor = Color(0xFF7BD88F)),
                ) { Text(S[lang, "install"]) }
            }
        }
        if (dl?.state == "busy") {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (if (dl.pct < 0) 0 else dl.pct) / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Pal.accent, trackColor = Pal.surface2)
        }
    }
}

private fun iconFor(kind: String) = when (kind) {
    "apk" -> "🤖"
    "exe" -> "🪟"
    "source" -> "📦"
    else -> "📄"
}
