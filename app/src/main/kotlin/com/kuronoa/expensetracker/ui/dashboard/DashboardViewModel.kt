package com.kuronoa.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuronoa.expensetracker.KuronoaApp
import com.kuronoa.expensetracker.core.logic.DashboardAggregator
import com.kuronoa.expensetracker.core.logic.DashboardSummary
import com.kuronoa.expensetracker.data.SettingsRepository
import com.kuronoa.expensetracker.data.repository.ExpenseRepository
import com.kuronoa.expensetracker.sync.SyncManager
import com.kuronoa.expensetracker.sync.SyncOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val summary: DashboardSummary = DashboardAggregator.summarize(emptyList()),
    val isSyncing: Boolean = false,
    val isConfigured: Boolean = false,
    val lastSyncAt: String? = null,
    val syncMessage: String? = null
)

class DashboardViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val syncState = MutableStateFlow(false to null as String?)

    val uiState: StateFlow<DashboardUiState> = combine(
        expenseRepository.observeAll().map { DashboardAggregator.summarize(it) },
        settingsRepository.settingsFlow,
        syncState
    ) { summary, settings, (syncing, message) ->
        DashboardUiState(
            summary = summary,
            isSyncing = syncing,
            isConfigured = settings.isConfigured,
            lastSyncAt = settings.lastSyncAt,
            syncMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun syncNow() {
        if (syncState.value.first) return
        viewModelScope.launch {
            syncState.value = true to null
            val outcome = syncManager.syncNow()
            val message = when (outcome) {
                is SyncOutcome.Success ->
                    "Sinkron selesai — ${outcome.pushed} perubahan dikirim, ${outcome.pulled} data ditarik."
                is SyncOutcome.NotConfigured -> outcome.reason
                is SyncOutcome.Failure -> outcome.message
            }
            syncState.value = false to message
        }
    }

    fun dismissMessage() {
        syncState.value = syncState.value.first to null
    }

    companion object {
        fun factory(app: KuronoaApp) = DashboardViewModel(
            app.expenseRepository, app.settingsRepository, app.syncManager
        )
    }
}
