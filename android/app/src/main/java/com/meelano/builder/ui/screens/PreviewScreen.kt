package com.meelano.builder.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.meelano.builder.ui.S
import com.meelano.builder.ui.theme.Pal

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(serverUrl: String, lang: String, jobId: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, S[lang, "back"], tint = Pal.dim)
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(serverUrl.trim().trimEnd('/') +
                        "/api/v1/jobs/$jobId/preview")
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
