package com.kuronoa.expensetracker.data.repository

import androidx.room.withTransaction
import com.kuronoa.expensetracker.core.model.ExpenseItem
import com.kuronoa.expensetracker.data.local.AppDatabase
import com.kuronoa.expensetracker.data.local.ExpenseDao
import com.kuronoa.expensetracker.data.local.toDomain
import com.kuronoa.expensetracker.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Satu pintu masuk data pengeluaran: UI SELALU baca/tulis lewat Room (offline-first),
 * lalu perubahan diselaraskan ke server oleh [com.kuronoa.expensetracker.sync.SyncManager].
 * Ini yang membuat app tetap responsif & "koneksi stabil" walau sinyal jelek —
 * input/edit/hapus tidak pernah menunggu jaringan.
 */
class ExpenseRepository(
    private val db: AppDatabase,
    private val dao: ExpenseDao = db.expenseDao()
) {
    fun observeMonth(bulan: String): Flow<List<ExpenseItem>> =
        dao.observeByMonth(bulan).map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<ExpenseItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getDistinctMonths(): List<String> = dao.getDistinctMonths()

    /** Tambah pengeluaran baru secara lokal (langsung terlihat di UI, menunggu sync). */
    suspend fun addLocal(item: ExpenseItem): Long {
        val tempId = UUID.randomUUID().toString()
        val entity = item.copy(id = "", dirty = true, pendingDelete = false, clientTempId = tempId).toEntity()
        return dao.insert(entity)
    }

    /** Ubah pengeluaran yang sudah ada (baik yang sudah tersinkron maupun belum). [item.localId] wajib > 0. */
    suspend fun updateLocal(item: ExpenseItem) {
        require(item.localId > 0) { "updateLocal butuh item yg sudah punya localId (hasil dari observeMonth/observeAll)." }
        dao.update(item.copy(dirty = true).toEntity())
    }

    /** Hapus pengeluaran. Jika belum pernah tersinkron, langsung hilang; jika sudah, ditandai pendingDelete dulu. */
    suspend fun deleteLocal(item: ExpenseItem) {
        require(item.localId > 0) { "deleteLocal butuh item yg sudah punya localId (hasil dari observeMonth/observeAll)." }
        if (item.id.isBlank()) {
            dao.deleteByLocalId(item.localId)
        } else {
            dao.markPendingDelete(item.localId)
        }
    }

    suspend fun getPendingSync(bulan: String): List<ExpenseItem> =
        dao.getPendingSync(bulan).map { it.toDomain() }

    /** Snapshot sekali-ambil (bukan Flow) seluruh item satu bulan — dipakai alur sinkronisasi. */
    suspend fun getFullMonthSnapshot(bulan: String): List<ExpenseItem> =
        dao.getByMonth(bulan).map { it.toDomain() }

    /**
     * Simpan hasil akhir sinkronisasi (setelah push + pull + merge) kembali ke Room,
     * sambil MENCOCOKKAN dgn baris lokal yang sudah ada (via serverId atau
     * clientTempId) supaya localId tidak berubah-ubah (UI tidak "kedip").
     */
    suspend fun applySyncResult(bulan: String, merged: List<ExpenseItem>) {
        db.withTransaction {
            val existing = dao.getByMonth(bulan)
            val byServerId = existing.filter { it.serverId.isNotBlank() }.associateBy { it.serverId }
            val byClientTempId = existing.filter { it.clientTempId != null }.associateBy { it.clientTempId }
            val usedLocalIds = mutableSetOf<Long>()

            for (item in merged) {
                val matchedExisting = (if (item.id.isNotBlank()) byServerId[item.id] else null)
                    ?: byClientTempId[item.clientTempId]
                val localId = matchedExisting?.localId ?: 0L
                dao.insert(item.toEntity(localId))
                if (matchedExisting != null) usedLocalIds += matchedExisting.localId
            }

            val toRemove = existing.filter { it.localId !in usedLocalIds }
            for (e in toRemove) dao.deleteByLocalId(e.localId)
        }
    }
}
