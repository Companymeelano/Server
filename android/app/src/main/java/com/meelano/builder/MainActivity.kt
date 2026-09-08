package com.meelano.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meelano.builder.data.Repository
import com.meelano.builder.data.SettingsStore
import com.meelano.builder.ui.BuilderApp
import com.meelano.builder.ui.theme.Bg
import com.meelano.builder.ui.theme.BuilderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(applicationContext)
        val repo = Repository()
        setContent {
            BuilderTheme {
                val lang by store.lang.collectAsStateWithLifecycle("en")
                CompositionLocalProvider(
                    LocalLayoutDirection provides
                        if (lang == "fa") LayoutDirection.Rtl
                        else LayoutDirection.Ltr
                ) {
                    Surface(Modifier.fillMaxSize(), color = Bg) {
                        BuilderApp(repo, store, lang)
                    }
                }
            }
        }
    }
}
