package com.kuronoa.expensetracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult

/**
 * Worker latar belakang (WorkManager) yang menjalankan [SyncManager.syncNow].
 * Dipakai baik utk sinkronisasi periodik (tiap ~30 menit, hemat baterai &
 * kuota) maupun sinkronisasi sekali-jalan saat pengguna menekan tombol
 * "Sync Sekarang" atau setelah menyimpan perubahan.
 *
 * Retry otomatis WorkManager (BackoffPolicy) menambah lapisan "koneksi
 * stabil" di atas retry HTTP yang sudah ada di [com.kuronoa.expensetracker.data.remote.RetryInterceptor].
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): WorkResult {
        return when (val outcome = syncManager.syncNow()) {
            is SyncOutcome.Success -> WorkResult.success()
            is SyncOutcome.NotConfigured -> WorkResult.success() // belum di-setup, bukan error -> jangan retry terus
            is SyncOutcome.Failure -> {
                if (runAttemptCount < MAX_ATTEMPTS) WorkResult.retry() else WorkResult.failure()
            }
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_NAME = "kuronoa_periodic_sync"
        const val UNIQUE_ONE_TIME_NAME = "kuronoa_manual_sync"
        private const val MAX_ATTEMPTS = 5
    }
}
