package com.meelano.builder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meelano.builder.ui.Brand
import com.meelano.builder.ui.S
import kotlinx.coroutines.launch
import com.meelano.builder.ui.theme.Pal

/** Top bar exactly like the YBee screenshot: ☰ Title ... AUTO ▢ */
@Composable
fun YbTopBar(
    lang: String,
    auto: Boolean,
    onMenu: () -> Unit,
    onPhone: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, "menu", tint = Pal.dim)
        }
        Text(Brand.SHORT, color = Pal.accent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        if (auto) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Pal.blue).padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("AUTO", color = Color(0xFF0B1520), fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onPhone) {
            Icon(Icons.Filled.PhoneAndroid, "preview", tint = Pal.dim)
        }
    }
}

@Composable
fun StatusBadge(lang: String, status: String) {
    val (bg, fg) = when (status) {
        "done" -> Color(0xFF1E4D2B) to Pal.green
        "building", "queued" -> Color(0xFF3A2A12) to Pal.yellow
        "failed" -> Color(0xFF5A1E2E) to Pal.red
        else -> Pal.surface2 to Pal.dim
    }
    val label = when (status) {
        "done" -> S[lang, "status_done"]
        "building" -> S[lang, "status_building"]
        "queued" -> S[lang, "status_queued"]
        "partial" -> S[lang, "status_partial"]
        "failed" -> S[lang, "status_failed"]
        else -> status
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg)
        .padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun DrawerContent(
    lang: String,
    route: String?,
    drawer: DrawerState,
    onGo: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    fun go(r: String) {
        scope.launch { drawer.close() }
        onGo(r)
    }
    ModalDrawerSheet(drawerContainerColor = Pal.surface, drawerContentColor = Pal.txt) {
        Column(Modifier.padding(16.dp)) {
            Text("🔨 ${Brand.FULL}", color = Pal.accent,
                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(if (lang == "fa") Brand.TEAM_FA else Brand.TEAM_EN,
                color = Pal.dim, fontSize = 12.sp)
        }
        val colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Pal.surface2, selectedTextColor = Pal.accent,
            unselectedTextColor = Pal.txt)
        NavigationDrawerItem(
            label = { Text(S[lang, "builder"]) }, selected = route == "home",
            onClick = { go("home") },
            icon = { Icon(Icons.Filled.Home, null, tint = Pal.dim) }, colors = colors)
        NavigationDrawerItem(
            label = { Text(S[lang, "my_apps"]) }, selected = route == "apps",
            onClick = { go("apps") },
            icon = { Icon(Icons.Filled.Apps, null, tint = Pal.dim) }, colors = colors)
        NavigationDrawerItem(
            label = { Text(S[lang, "templates"]) }, selected = route == "templates",
            onClick = { go("templates") },
            icon = { Icon(Icons.Filled.Star, null, tint = Pal.dim) }, colors = colors)
        NavigationDrawerItem(
            label = { Text(S[lang, "settings"]) }, selected = route == "settings",
            onClick = { go("settings") },
            icon = { Icon(Icons.Filled.Settings, null, tint = Pal.dim) }, colors = colors)
        NavigationDrawerItem(
            label = { Text(S[lang, "about"]) }, selected = route == "about",
            onClick = { go("about") },
            icon = { Icon(Icons.Filled.Info, null, tint = Pal.dim) }, colors = colors)
    }
}

/** Friendly, localized error text instead of raw exception messages. */
fun errText(lang: String, e: Throwable): String {
    val http = generateSequence(e) { it.cause }.filterIsInstance<retrofit2.HttpException>().firstOrNull()
    if (http != null) {
        return when (http.code()) {
            401 -> S[lang, "err_unauth"]
            429 -> if (lang == "fa") "محدودیت سرعت: کمی بعد دوباره تلاش کن."
            else "Rate limit: try again in a bit."
            else -> if (lang == "fa") "خطای سرور (${http.code()}). دوباره تلاش کن."
            else "Server error (${http.code()}). Try again."
        }
    }
    val io = generateSequence(e) { it.cause }.any {
        it is java.io.IOException || it is java.net.UnknownHostException ||
            it is java.net.SocketTimeoutException ||
            (it is IllegalArgumentException && "$it".contains("baseUrl"))
    }
    if (io) return S[lang, "err_network"]
    return (e.message ?: "$e").take(160)
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Pal.surface)
        .padding(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}
