package com.kuronoa.expensetracker.sync

import com.kuronoa.expensetracker.core.logic.ExpenseValidator
import com.kuronoa.expensetracker.core.logic.PushOperation
import com.kuronoa.expensetracker.core.logic.PushResult
import com.kuronoa.expensetracker.core.logic.SyncMerger
import com.kuronoa.expensetracker.data.SettingsRepository
import com.kuronoa.expensetracker.data.remote.ApiException
import com.kuronoa.expensetracker.data.remote.SheetsApiClient
import com.kuronoa.expensetracker.data.remote.SyncOperationDto
import com.kuronoa.expensetracker.data.remote.toDomain
import com.kuronoa.expensetracker.data.remote.toDto
import com.kuronoa.expensetracker.data.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class SyncOutcome {
    data class Success(val pushed: Int, val pulled: Int, val failedPush: Int) : SyncOutcome()
    data class NotConfigured(val reason: String) : SyncOutcome()
    data class Failure(val message: String) : SyncOutcome()
}

/**
 * Orkestrasi sinkronisasi dua-arah: kumpulkan perubahan lokal semua bulan ->
 * kirim SATU kali request (action=batchSync, hemat kuota & lebih stabil di
 * jaringan lambat) -> terapkan hasil push -> gabungkan data terbaru dari
 * server -> simpan balik ke Room.
 */
class SyncManager(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val apiClientFactory: (baseUrl: String, token: String) -> SheetsApiClient
) {
    suspend fun syncNow(): SyncOutcome {
        val settings = settingsRepository.current()
        if (!settings.isConfigured) {
            return SyncOutcome.NotConfigured("URL Apps Script & token belum diisi di Pengaturan.")
        }
        val api = apiClientFactory(settings.apiBaseUrl, settings.apiToken)

        return try {
            val months = runCatching { api.months().months }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: ExpenseValidator.MONTHS

            val pushOps = mutableListOf<PushOperation>()
            for (month in months) {
                val pending = repository.getPendingSync(month)
                pushOps += SyncMerger.buildPushOperations(pending, month)
            }
            val opDtos = pushOps.map { op ->
                SyncOperationDto(
                    op = op.op,
                    month = op.month,
                    clientTempId = op.clientTempId,
                    item = op.item?.toDto(),
                    id = op.id
                )
            }

            // PENTING: `since` SENGAJA tidak dipakai utk memfilter data yg ditarik (pull).
            // mergeRemote() menganggap daftar remote per-bulan sbg keadaan LENGKAP server
            // (item lokal bersih yg tidak ada di situ = dianggap terhapus pihak lain).
            // Kalau pull di-filter delta (since=lastSyncAt), baris yg TIDAK berubah jadi
            // hilang dari respons & bisa salah kehapus di lokal. Skala data (pembukuan
            // toko/bulan) kecil, jadi full-pull tiap sync jauh lebih aman drpd optimisasi
            // delta yg butuh mekanisme tombstone tambahan di server.
            val response = api.batchSync(opDtos, since = null)

            val pushResults = response.pushResults.map { dto ->
                PushResult(
                    op = dto.op,
                    month = dto.month,
                    ok = dto.ok,
                    clientTempId = dto.clientTempId,
                    id = dto.id,
                    item = dto.item?.toDomain(),
                    error = dto.error
                )
            }

            var pulledCount = 0
            for (month in months) {
                val localFull = repository.getFullMonthSnapshot(month)
                val monthPushResults = pushResults.filter { it.month == month }
                val afterPush = SyncMerger.applyPushResults(localFull, monthPushResults)
                val remote = response.months[month]?.map { it.toDomain() } ?: emptyList()
                pulledCount += remote.size
                val merged = SyncMerger.mergeRemote(afterPush, remote)
                repository.applySyncResult(month, merged)
            }

            settingsRepository.setLastSyncAt(response.serverTime ?: nowIso())
            val failedPush = pushResults.count { !it.ok }
            SyncOutcome.Success(pushed = pushOps.size, pulled = pulledCount, failedPush = failedPush)
        } catch (e: ApiException) {
            SyncOutcome.Failure(e.message ?: "Sinkronisasi gagal.")
        } catch (e: Exception) {
            SyncOutcome.Failure("Sinkronisasi gagal: ${e.message ?: e::class.simpleName}")
        }
    }

    private fun nowIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
