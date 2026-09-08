package com.meelano.builder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("mbuilder")

class SettingsStore(private val ctx: Context) {
    companion object {
        // 10.0.2.2 = host PC when running on the Android emulator.
        // On a real phone use your PC's LAN IP, e.g. http://192.168.1.5:8000
        const val DEFAULT_SERVER = "http://10.0.2.2:8000"
        private val K_SERVER = stringPreferencesKey("server_url")
        private val K_LANG = stringPreferencesKey("lang")
        private val K_AUTO = booleanPreferencesKey("auto")
        private val K_AIKEY = stringPreferencesKey("ai_key")
        private val K_AIBASE = stringPreferencesKey("ai_base")
        private val K_TOKEN = stringPreferencesKey("api_token")
    }

    val serverUrl: Flow<String> = ctx.dataStore.data.map { it[K_SERVER] ?: DEFAULT_SERVER }
    val lang: Flow<String> = ctx.dataStore.data.map { it[K_LANG] ?: "en" }
    val auto: Flow<Boolean> = ctx.dataStore.data.map { it[K_AUTO] ?: true }
    val aiKey: Flow<String> = ctx.dataStore.data.map { it[K_AIKEY] ?: "" }
    val aiBase: Flow<String> = ctx.dataStore.data.map { it[K_AIBASE] ?: "" }
    val token: Flow<String> = ctx.dataStore.data.map { it[K_TOKEN] ?: "" }

    suspend fun setServer(v: String) = ctx.dataStore.edit { it[K_SERVER] = v.trim().trimEnd('/') }
    suspend fun setLang(v: String) = ctx.dataStore.edit { it[K_LANG] = v }
    suspend fun setAuto(v: Boolean) = ctx.dataStore.edit { it[K_AUTO] = v }
    suspend fun setAiKey(v: String) = ctx.dataStore.edit { it[K_AIKEY] = v.trim() }
    suspend fun setAiBase(v: String) = ctx.dataStore.edit { it[K_AIBASE] = v.trim().trimEnd('/') }
    suspend fun setToken(v: String) = ctx.dataStore.edit { it[K_TOKEN] = v.trim() }
}
