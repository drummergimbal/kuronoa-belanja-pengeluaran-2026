package com.kuronoa.expensetracker.core.logic

import com.kuronoa.expensetracker.core.model.ExpenseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergerTest {

    private fun item(
        id: String = "",
        jumlah: Double = 1000.0,
        dirty: Boolean = false,
        pendingDelete: Boolean = false,
        updatedAt: String? = null,
        clientTempId: String? = null,
        uraian: String = "Item"
    ) = ExpenseItem(
        id = id, uraian = uraian, kategori = "Bahan Baku", jumlah = jumlah,
        lokasi = "Toko", pembayaran = "Cash", tanggal = "2026-08-01",
        dirty = dirty, pendingDelete = pendingDelete, updatedAt = updatedAt,
        clientTempId = clientTempId
    )

    // ---------- buildPushOperations ----------

    @Test
    fun `item baru menghasilkan operasi create`() {
        val local = listOf(item(id = "", clientTempId = "tmp-1"))
        val ops = SyncMerger.buildPushOperations(local, "Agustus")
        assertEquals(1, ops.size)
        assertEquals("create", ops[0].op)
        assertEquals("tmp-1", ops[0].clientTempId)
    }

    @Test
    fun `item dirty dgn id menghasilkan operasi update`() {
        val local = listOf(item(id = "srv-1", dirty = true))
        val ops = SyncMerger.buildPushOperations(local, "Agustus")
        assertEquals(1, ops.size)
        assertEquals("update", ops[0].op)
        assertEquals("srv-1", ops[0].item?.id)
    }

    @Test
    fun `item pendingDelete dgn id menghasilkan operasi delete`() {
        val local = listOf(item(id = "srv-1", pendingDelete = true))
        val ops = SyncMerger.buildPushOperations(local, "Agustus")
        assertEquals(1, ops.size)
        assertEquals("delete", ops[0].op)
        assertEquals("srv-1", ops[0].id)
    }

    @Test
    fun `item baru yg langsung dihapus sblm sync tidak menghasilkan operasi apapun`() {
        val local = listOf(item(id = "", pendingDelete = true, clientTempId = "tmp-2"))
        val ops = SyncMerger.buildPushOperations(local, "Agustus")
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `item bersih tidak menghasilkan operasi`() {
        val local = listOf(item(id = "srv-1", dirty = false, pendingDelete = false))
        val ops = SyncMerger.buildPushOperations(local, "Agustus")
        assertTrue(ops.isEmpty())
    }

    // ---------- applyPushResults ----------

    @Test
    fun `create sukses mengisi id server dan membersihkan dirty`() {
        val local = listOf(item(id = "", clientTempId = "tmp-1"))
        val results = listOf(
            PushResult(
                op = "create", month = "Agustus", ok = true, clientTempId = "tmp-1",
                item = item(id = "srv-99", clientTempId = "tmp-1")
            )
        )
        val merged = SyncMerger.applyPushResults(local, results)
        assertEquals(1, merged.size)
        assertEquals("srv-99", merged[0].id)
        assertTrue(!merged[0].dirty)
    }

    @Test
    fun `delete sukses membuang item dari daftar lokal`() {
        val local = listOf(item(id = "srv-1", pendingDelete = true), item(id = "srv-2"))
        val results = listOf(PushResult(op = "delete", month = "Agustus", ok = true, id = "srv-1"))
        val merged = SyncMerger.applyPushResults(local, results)
        assertEquals(1, merged.size)
        assertEquals("srv-2", merged[0].id)
    }

    @Test
    fun `update gagal tetap menyimpan item apa adanya utk dicoba lagi`() {
        val local = listOf(item(id = "srv-1", dirty = true))
        val results = listOf(PushResult(op = "update", month = "Agustus", ok = false, error = "NOT_FOUND"))
        val merged = SyncMerger.applyPushResults(local, results)
        assertEquals(1, merged.size)
        assertTrue(merged[0].dirty) // masih dirty krn gagal, akan direkonsiliasi via mergeRemote
    }

    // ---------- mergeRemote ----------

    @Test
    fun `item remote baru ditambahkan ke lokal`() {
        val local = emptyList<ExpenseItem>()
        val remote = listOf(item(id = "srv-1", updatedAt = "2026-08-01T10:00:00Z"))
        val merged = SyncMerger.mergeRemote(local, remote)
        assertEquals(1, merged.size)
        assertEquals("srv-1", merged[0].id)
    }

    @Test
    fun `perubahan lokal yg belum ter-push tidak ditimpa oleh remote yg lebih lama`() {
        val local = listOf(item(id = "srv-1", dirty = true, updatedAt = "2026-08-01T12:00:00Z", jumlah = 5000.0))
        val remote = listOf(item(id = "srv-1", updatedAt = "2026-08-01T10:00:00Z", jumlah = 1000.0))
        val merged = SyncMerger.mergeRemote(local, remote)
        assertEquals(1, merged.size)
        assertEquals(5000.0, merged[0].jumlah, 0.001)
        assertTrue(merged[0].dirty)
    }

    @Test
    fun `remote yg lebih baru menimpa lokal yg bersih (last-write-wins)`() {
        val local = listOf(item(id = "srv-1", dirty = false, updatedAt = "2026-08-01T09:00:00Z", jumlah = 1000.0))
        val remote = listOf(item(id = "srv-1", updatedAt = "2026-08-01T11:00:00Z", jumlah = 9999.0))
        val merged = SyncMerger.mergeRemote(local, remote)
        assertEquals(1, merged.size)
        assertEquals(9999.0, merged[0].jumlah, 0.001)
    }

    @Test
    fun `item bersih yg hilang dari remote dibuang (dihapus pihak lain)`() {
        val local = listOf(item(id = "srv-1", dirty = false))
        val remote = emptyList<ExpenseItem>()
        val merged = SyncMerger.mergeRemote(local, remote)
        assertTrue(merged.isEmpty())
    }

    @Test
    fun `item lokal yg masih pendingDelete tidak dihidupkan lagi oleh remote`() {
        val local = listOf(item(id = "srv-1", pendingDelete = true))
        val remote = listOf(item(id = "srv-1", updatedAt = "2026-08-01T23:00:00Z"))
        val merged = SyncMerger.mergeRemote(local, remote)
        assertEquals(1, merged.size)
        assertTrue(merged[0].pendingDelete)
    }

    @Test
    fun `item baru yg belum pernah sync tetap dipertahankan setelah pull`() {
        val local = listOf(item(id = "", clientTempId = "tmp-1"))
        val remote = listOf(item(id = "srv-2"))
        val merged = SyncMerger.mergeRemote(local, remote)
        assertEquals(2, merged.size)
        assertTrue(merged.any { it.clientTempId == "tmp-1" })
        assertTrue(merged.any { it.id == "srv-2" })
    }

    // ---------- isNewer ----------

    @Test
    fun `isNewer membandingkan timestamp iso scr leksikografis`() {
        assertTrue(SyncMerger.isNewer("2026-08-02T00:00:00Z", "2026-08-01T23:59:59Z"))
        assertTrue(!SyncMerger.isNewer("2026-08-01T00:00:00Z", "2026-08-01T23:59:59Z"))
        assertTrue(SyncMerger.isNewer("2026-08-01T00:00:00Z", null))
        assertTrue(!SyncMerger.isNewer(null, "2026-08-01T00:00:00Z"))
    }
}
