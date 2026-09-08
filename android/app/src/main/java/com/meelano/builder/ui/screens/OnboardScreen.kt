package com.meelano.builder.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meelano.builder.ui.theme.Pal
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardScreen(lang: String, onDone: () -> Unit) {
    val fa = lang == "fa"
    val pages = if (fa) listOf(
        Triple("💡", "یه جمله بنویس", "مثلاً: «یه برنامه تاس بساز» — همین! نیازی به دانش برنامه‌نویسی نیست."),
        Triple("🔨", "بیلد را زنده ببین", "سورس‌کد، پیش‌نمایش، فایل ویندوز و اندروید خودکار ساخته می‌شود."),
        Triple("📲", "نصب کن و لذت ببر", "APK مستقیم روی گوشی نصب می‌شود؛ فایل Setup هم برای ویندوز داری."),
    ) else listOf(
        Triple("💡", "Type one sentence", "Like: “Create a Dice app” — that's it. No coding needed."),
        Triple("🔨", "Watch it build", "Sources, preview, Windows and Android outputs are made automatically."),
        Triple("📲", "Install & enjoy", "The APK installs right on your phone; a Setup file covers Windows."),
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            Column(Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(pages[i].first, fontSize = 84.sp)
                Spacer(Modifier.height(20.dp))
                Text(pages[i].second, color = Pal.accent, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(pages[i].third, color = Pal.dim, fontSize = 15.sp,
                    textAlign = TextAlign.Center, lineHeight = 24.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { i ->
                Box(Modifier.size(if (i == pager.currentPage) 22.dp else 8.dp, 8.dp)
                    .clip(CircleShape)
                    .background(if (i == pager.currentPage) Pal.accent else Pal.surface2))
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (pager.currentPage < pages.size - 1) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else onDone()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Pal.accent, contentColor = Pal.bg),
        ) {
            Text(if (pager.currentPage < pages.size - 1) {
                if (fa) "بعدی ←" else "Next →"
            } else {
                if (fa) "شروع 🚀" else "Start 🚀"
            })
        }
        TextButton(onClick = onDone) { Text(if (fa) "رد شدن" else "Skip", color = Pal.dim) }
        Spacer(Modifier.height(8.dp))
    }
}
