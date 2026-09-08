package com.meelano.builder.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meelano.builder.data.Repository
import com.meelano.builder.data.SettingsStore
import com.meelano.builder.ui.components.DrawerContent
import com.meelano.builder.ui.screens.AboutScreen
import com.meelano.builder.ui.screens.AppsScreen
import com.meelano.builder.ui.screens.BuildScreen
import com.meelano.builder.ui.screens.HomeScreen
import com.meelano.builder.ui.screens.PreviewScreen
import com.meelano.builder.ui.screens.SettingsScreen
import com.meelano.builder.ui.screens.TemplatesScreen
import kotlinx.coroutines.launch

@Composable
fun BuilderApp(repo: Repository, store: SettingsStore, lang: String) {
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()

    val serverUrl by store.serverUrl.collectAsStateWithLifecycle(
        SettingsStore.DEFAULT_SERVER)
    val auto by store.auto.collectAsStateWithLifecycle(true)
    val aiKey by store.aiKey.collectAsStateWithLifecycle("")
    val aiBase by store.aiBase.collectAsStateWithLifecycle("")
    val token by store.token.collectAsStateWithLifecycle("")

    fun phoneAction() {
        scope.launch {
            val target = try {
                val jobs = repo.apiFor(serverUrl, token).jobs(1).jobs
                if (jobs.isNotEmpty()) "preview/${jobs[0].id}" else "apps"
            } catch (_: Exception) { "apps" }
            nav.navigate(target)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            DrawerContent(lang, entry?.destination?.route, drawer) { r ->
                nav.navigate(r) { launchSingleTop = true }
            }
        },
    ) {
        NavHost(nav, startDestination = "home") {
            composable("home") {
                HomeScreen(repo, serverUrl, lang, auto, aiKey, aiBase, token,
                    onOpenDrawer = { scope.launch { drawer.open() } },
                    onPhone = ::phoneAction,
                    onBuilt = { nav.navigate("build/$it") })
            }
            composable("build/{id}") { back ->
                val id = back.arguments?.getString("id") ?: ""
                BuildScreen(repo, serverUrl, lang, token, id,
                    onBack = { nav.popBackStack() },
                    onPreview = { nav.navigate("preview/$it") })
            }
            composable("preview/{id}") { back ->
                val id = back.arguments?.getString("id") ?: ""
                PreviewScreen(serverUrl, lang, id, onBack = { nav.popBackStack() })
            }
            composable("apps") {
                AppsScreen(repo, serverUrl, lang, token,
                    onOpen = { nav.navigate("build/$it") })
            }
            composable("templates") {
                TemplatesScreen(repo, serverUrl, lang, auto, aiKey, aiBase, token,
                    onBuilt = { nav.navigate("build/$it") })
            }
            composable("settings") { SettingsScreen(store, repo, lang) }
            composable("about") { AboutScreen(lang) }
        }
    }
}
