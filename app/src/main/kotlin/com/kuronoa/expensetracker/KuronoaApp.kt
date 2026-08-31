package com.kuronoa.expensetracker

import android.app.Application
import androidx.work.Configuration
import com.kuronoa.expensetracker.data.SettingsRepository
import com.kuronoa.expensetracker.data.local.AppDatabase
import com.kuronoa.expensetracker.data.remote.NetworkModule
import com.kuronoa.expensetracker.data.remote.SheetsApiClient
import com.kuronoa.expensetracker.data.repository.ExpenseRepository
import com.kuronoa.expensetracker.sync.KuronoaWorkerFactory
import com.kuronoa.expensetracker.sync.SyncManager
import com.kuronoa.expensetracker.sync.SyncScheduler

/**
 * Application class + wadah dependency sederhana (manual DI, tanpa Hilt/Dagger
 * supaya proyek tetap ringan & gampang di-build). Semua singleton dibuat
 * malas (lazy) & dipakai bersama oleh UI (ViewModel) dan SyncWorker.
 */
class KuronoaApp : Application(), Configuration.Provider {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database) }
    private val okHttpClient by lazy { NetworkModule.okHttpClient(debug = BuildConfig.DEBUG) }

    val apiClientFactory: (String, String) -> SheetsApiClient = { baseUrl, token ->
        SheetsApiClient(baseUrl = baseUrl, token = token, client = okHttpClient)
    }

    val syncManager: SyncManager by lazy {
        SyncManager(
            repository = expenseRepository,
            settingsRepository = settingsRepository,
            apiClientFactory = apiClientFactory
        )
    }

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedulePeriodic(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .setWorkerFactory(KuronoaWorkerFactory(syncManager))
            .build()
}
