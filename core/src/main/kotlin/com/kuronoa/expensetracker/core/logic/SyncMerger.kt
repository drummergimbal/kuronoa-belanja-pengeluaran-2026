package com.kuronoa.expensetracker.core.logic

import com.kuronoa.expensetracker.core.model.ExpenseItem

/** Satu operasi yang perlu dikirim (push) ke Apps Script lewat action=batchSync. */
data class PushOperation(
    val op: String,           // "create" | "update" | "delete"
    val month: String,
    val clientTempId: String? = null,
    val item: ExpenseItem? = null,
    val id: String? = null
)

/** Hasil satu operasi push, sebagaimana dikembalikan server. */
data class PushResult(
    val op: String,
    val month: String,
    val ok: Boolean,
    val clientTempId: String? = null,
    val id: String? = null,
    val item: ExpenseItem? = null,
    val error: String? = null
)

/**
 * Logika inti sinkronisasi dua-arah antara data lokal (Room) dan server
 * (Apps Script / Google Sheets), dengan strategi last-write-wins berbasis
 * timestamp [ExpenseItem.updatedAt]. Murni logika (tanpa I/O) supaya mudah
 * di-unit-test tanpa Android framework atau koneksi jaringan.
 */
object SyncMerger {

    /** Susun daftar operasi yang perlu di-push berdasarkan status lokal. */
    fun buildPushOperations(items: List<ExpenseItem>, month: String): List<PushOperation> {
        val ops = mutableListOf<PushOperation>()
        for (item in items) {
            when {
                item.pendingDelete && !item.isNew -> {
                    ops += PushOperation(op = "delete", month = month, id = item.id)
                }
                item.pendingDelete && item.isNew -> {
                    // Baru dibuat lokal lalu dihapus lagi sebelum sempat sync: tidak perlu dikirim sama sekali.
                }
                item.isNew -> {
                    ops += PushOperation(
                        op = "create",
                        month = month,
                        clientTempId = item.clientTempId ?: item.no,
                        item = item
                    )
                }
                item.dirty -> {
                    ops += PushOperation(op = "update", month = month, item = item)
                }
            }
        }
        return ops
    }

    /** Terapkan hasil push ke daftar lokal: bersihkan flag dirty/pendingDelete, isi id server utk item baru. */
    fun applyPushResults(local: List<ExpenseItem>, results: List<PushResult>): List<ExpenseItem> {
        if (results.isEmpty()) return local
        val byTempId = results.filter { it.op == "create" && it.clientTempId != null }
            .associateBy { it.clientTempId }
        val byId = results.filter { it.op == "update" }
            .associateBy { it.item?.id }
        val deletedIds = results.filter { it.op == "delete" && it.ok }.mapNotNull { it.id }.toSet()

        val out = mutableListOf<ExpenseItem>()
        for (item in local) {
            if (item.id in deletedIds) continue // sukses dihapus di server -> buang dari lokal

            val createResult = if (item.isNew) byTempId[item.clientTempId ?: item.no] else null
            if (createResult != null) {
                out += if (createResult.ok && createResult.item != null) {
                    createResult.item.copy(dirty = false, pendingDelete = false)
                } else {
                    item // create gagal: tetap simpan sbg dirty, dicoba lagi nanti
                }
                continue
            }

            val updateResult = byId[item.id]
            if (updateResult != null) {
                out += if (updateResult.ok && updateResult.item != null) {
                    updateResult.item.copy(dirty = false, pendingDelete = false)
                } else {
                    item // update gagal (mis. NOT_FOUND krn dihapus di server): biarkan, akan direkonsiliasi oleh mergeRemote
                }
                continue
            }

            out += item
        }
        return out
    }

    /**
     * Gabungkan hasil pull dari server ke daftar lokal.
     *
     * Prinsipnya sengaja dibuat sederhana & dapat diprediksi (last-write-wins
     * di level *push*, bukan tebak-tebakan timestamp di klien):
     * - Item lokal yang masih dirty/pendingDelete/baru (belum sukses di-push)
     *   TIDAK PERNAH ditimpa oleh hasil pull — nanti dibereskan oleh
     *   [applyPushResults] setelah push berikutnya berhasil.
     * - Item lokal yang sudah "bersih" (tidak ada perubahan lokal tertunda)
     *   selalu mengikuti data server apa adanya.
     * - Item lokal bersih yang sudah hilang dari server berarti dihapus pihak
     *   lain -> dibuang dari lokal.
     */
    fun mergeRemote(local: List<ExpenseItem>, remote: List<ExpenseItem>): List<ExpenseItem> {
        val localById = local.filter { !it.isNew }.associateBy { it.id }
        val remoteIds = remote.map { it.id }.toSet()
        val out = mutableListOf<ExpenseItem>()

        for (r in remote) {
            val loc = localById[r.id]
            out += when {
                loc == null -> r
                loc.dirty || loc.pendingDelete -> loc
                else -> r
            }
        }
        // Item lokal baru (belum pernah sync) atau sedang dirty/pendingDelete tetap dipertahankan.
        for (l in local) {
            if (l.isNew || l.dirty || l.pendingDelete) {
                if (l.id.isBlank() || l.id !in remoteIds) out += l
            }
        }
        return out
    }

    /** Bandingkan dua timestamp ISO 8601 UTC ("yyyy-MM-dd'T'HH:mm:ss'Z'"); aman dibandingkan sbg string. */
    fun isNewer(a: String?, b: String?): Boolean {
        if (a == null) return false
        if (b == null) return true
        return a > b
    }
}
