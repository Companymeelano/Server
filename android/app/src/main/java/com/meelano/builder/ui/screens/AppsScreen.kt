package com.meelano.builder.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meelano.builder.data.Job
import com.meelano.builder.data.Repository
import com.meelano.builder.ui.Brand
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.StatusBadge
import com.meelano.builder.ui.components.accentOf
import com.meelano.builder.ui.components.errText
import com.meelano.builder.ui.theme.Pal

@Composable
fun AppsScreen(
    repo: Repository,
    serverUrl: String,
    lang: String,
    token: String,
    onOpen: (String) -> Unit,
) {
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var err by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(serverUrl, token, tick) {
        try {
            jobs = repo.apiFor(serverUrl, token).jobs(50).jobs
            err = ""
        } catch (e: Exception) {
            err = errText(lang, e)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📱 ${S[lang, "my_apps"]}", color = Pal.accent,
                fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { tick++ }) {
                Icon(Icons.Filled.Refresh, S[lang, "refresh"], tint = Pal.dim)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (err.isNotEmpty()) {
            Text(err, color = Pal.dim, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { tick++ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent, contentColor = Pal.bg),
            ) { Text(S[lang, "retry"]) }
            Spacer(Modifier.height(8.dp))
        }
        if (jobs.isEmpty() && err.isEmpty()) {
            Text(S[lang, "empty_apps"], color = Pal.dim, fontSize = 14.sp)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobs) { j ->
                val ac = accentOf(
                    Brand.TEMPLATE_ACCENTS[j.template] ?: "#D9A7E6")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Pal.surface).clickable { onOpen(j.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📦", fontSize = 26.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(ac))
                            Text(if (j.name.isEmpty()) j.idea else j.name,
                                color = Pal.txt, fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp, maxLines = 1)
                        }
                        Text(j.idea, color = Pal.dim, fontSize = 12.sp, maxLines = 1)
                    }
                    StatusBadge(lang, j.status)
                }
            }
        }
    }
}
