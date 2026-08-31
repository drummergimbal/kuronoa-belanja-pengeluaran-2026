package com.kuronoa.expensetracker.ui.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuronoa.expensetracker.KuronoaApp
import com.kuronoa.expensetracker.core.logic.ExpenseValidator
import com.kuronoa.expensetracker.core.model.ExpenseItem
import com.kuronoa.expensetracker.data.repository.ExpenseRepository
import com.kuronoa.expensetracker.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val appContext: Context
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(currentMonthName())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val months: List<String> = ExpenseValidator.MONTHS

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<ExpenseItem>> = _selectedMonth
        .flatMapLatest { month -> repository.observeMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    /**
     * Validasi berjalan SINKRON supaya UI langsung tahu (tanpa race condition)
     * apakah boleh menutup form: true = valid & proses simpan sudah dimulai
     * di latar belakang, false = ada error validasi (lihat [formError]).
     */
    fun save(item: ExpenseItem): Boolean {
        val errors = ExpenseValidator.validate(item)
        if (errors.isNotEmpty()) {
            _formError.value = errors.first()
            return false
        }
        _formError.value = null
        viewModelScope.launch {
            val withMonth = item.copy(bulan = item.bulan.ifBlank { ExpenseValidator.monthNameFromIsoDate(item.tanggal) })
            if (withMonth.localId <= 0) {
                repository.addLocal(withMonth)
            } else {
                repository.updateLocal(withMonth)
            }
            SyncScheduler.syncNowInBackground(appContext)
        }
        return true
    }

    fun delete(item: ExpenseItem) {
        viewModelScope.launch {
            repository.deleteLocal(item)
            SyncScheduler.syncNowInBackground(appContext)
        }
    }

    fun clearFormError() {
        _formError.value = null
    }

    companion object {
        fun currentMonthName(): String {
            val cal = Calendar.getInstance()
            return ExpenseValidator.MONTHS[cal.get(Calendar.MONTH)]
        }

        fun todayIso(): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return fmt.format(Calendar.getInstance().time)
        }

        fun factory(app: KuronoaApp) = ExpenseViewModel(app.expenseRepository, app)
    }
}
