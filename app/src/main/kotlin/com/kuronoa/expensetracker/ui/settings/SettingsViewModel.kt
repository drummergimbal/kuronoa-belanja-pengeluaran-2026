package com.kuronoa.expensetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuronoa.expensetracker.KuronoaApp
import com.kuronoa.expensetracker.data.SettingsRepository
import com.kuronoa.expensetracker.data.SyncSettings
import com.kuronoa.expensetracker.data.remote.SheetsApiClient
import com.kuronoa.expensetracker.sync.SyncManager
import com.kuronoa.expensetracker.sync.SyncOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectionTestState(val inProgress: Boolean = false, val resultMessage: String? = null, val success: Boolean? = null)
data class SyncActionState(val inProgress: Boolean = false, val resultMessage: String? = null)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    private val apiClientFactory: (String, String) -> SheetsApiClient
) : ViewModel() {

    val settings: StateFlow<SyncSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncSettings())

    private val _testState = MutableStateFlow(ConnectionTestState())
    val testState: StateFlow<ConnectionTestState> = _testState

    private val _syncState = MutableStateFlow(SyncActionState())
    val syncState: StateFlow<SyncActionState> = _syncState

    fun saveConnection(baseUrl: String, token: String) {
        viewModelScope.launch {
            settingsRepository.saveConnection(baseUrl, token)
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSync(enabled) }
    }

    fun testConnection(baseUrl: String, token: String) {
        viewModelScope.launch {
            _testState.value = ConnectionTestState(inProgress = true)
            val result = runCatching { apiClientFactory(baseUrl, token).ping() }
            _testState.value = result.fold(
                onSuccess = { ConnectionTestState(success = true, resultMessage = "Berhasil terhubung! Versi server: ${it.version ?: "-"}") },
                onFailure = { ConnectionTestState(success = false, resultMessage = it.message ?: "Gagal terhubung.") }
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncState.value = SyncActionState(inProgress = true)
            val outcome = syncManager.syncNow()
            val message = when (outcome) {
                is SyncOutcome.Success -> "Sinkron selesai — ${outcome.pushed} dikirim, ${outcome.pulled} ditarik" +
                    if (outcome.failedPush > 0) ", ${outcome.failedPush} gagal (akan dicoba lagi)." else "."
                is SyncOutcome.NotConfigured -> outcome.reason
                is SyncOutcome.Failure -> outcome.message
            }
            _syncState.value = SyncActionState(inProgress = false, resultMessage = message)
        }
    }

    companion object {
        fun factory(app: KuronoaApp) = SettingsViewModel(app.settingsRepository, app.syncManager, app.apiClientFactory)
    }
}
