package com.meelano.builder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meelano.builder.ui.Brand
import com.meelano.builder.ui.S
import com.meelano.builder.ui.components.CardBox
import com.meelano.builder.ui.theme.Accent
import com.meelano.builder.ui.theme.Dim
import com.meelano.builder.ui.theme.Txt

@Composable
fun AboutScreen(lang: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔨 ${Brand.FULL} v1.0", color = Accent,
            fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))
        CardBox(Modifier.fillMaxWidth()) {
            Text(S[lang, "about_text"], color = Txt, fontSize = 14.sp,
                lineHeight = 22.sp)
            Text("• 🤖 Android APK — " +
                if (lang == "fa") "نصب مستقیم روی گوشی"
                else "direct install on any phone",
                color = Dim, fontSize = 13.sp)
            Text("• 🪟 Windows EXE + Setup — " +
                if (lang == "fa") "فایل نصب ویندوز"
                else "installer for Windows",
                color = Dim, fontSize = 13.sp)
            Text("• 📦 " +
                if (lang == "fa") "سورس‌کد + پیش‌نمایش زنده"
                else "Sources + live preview",
                color = Dim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(if (lang == "fa") Brand.TEAM_FA else Brand.TEAM_EN,
            color = Dim, fontSize = 13.sp)
    }
}
