package com.kuronoa.expensetracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kuronoa_settings")

data class SyncSettings(
    val apiBaseUrl: String = "",
    val apiToken: String = "",
    val autoSyncEnabled: Boolean = true,
    val lastSyncAt: String? = null
) {
    val isConfigured: Boolean get() = apiBaseUrl.isNotBlank() && apiToken.isNotBlank()
}

/** Penyimpanan pengaturan koneksi (URL Apps Script + token) & status sync terakhir. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val API_TOKEN = stringPreferencesKey("api_token")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        val LAST_SYNC_AT = stringPreferencesKey("last_sync_at")
    }

    val settingsFlow: Flow<SyncSettings> = context.dataStore.data.map { prefs ->
        SyncSettings(
            apiBaseUrl = prefs[Keys.API_BASE_URL] ?: "",
            apiToken = prefs[Keys.API_TOKEN] ?: "",
            autoSyncEnabled = prefs[Keys.AUTO_SYNC] ?: true,
            lastSyncAt = prefs[Keys.LAST_SYNC_AT]
        )
    }

    suspend fun current(): SyncSettings = settingsFlow.first()

    suspend fun saveConnection(baseUrl: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = baseUrl.trim()
            prefs[Keys.API_TOKEN] = token.trim()
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_SYNC] = enabled }
    }

    suspend fun setLastSyncAt(iso: String) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_SYNC_AT] = iso }
    }
}
