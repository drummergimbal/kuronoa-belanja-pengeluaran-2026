package com.kuronoa.expensetracker.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

/** WorkerFactory manual (tanpa Hilt) supaya [SyncWorker] bisa menerima [SyncManager] lewat konstruktor. */
class KuronoaWorkerFactory(private val syncManager: SyncManager) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        SyncWorker::class.java.name -> SyncWorker(appContext, workerParameters, syncManager)
        else -> null
    }
}
