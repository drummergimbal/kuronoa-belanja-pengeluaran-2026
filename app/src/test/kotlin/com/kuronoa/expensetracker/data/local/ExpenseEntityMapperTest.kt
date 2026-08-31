package com.kuronoa.expensetracker.data.local

import com.kuronoa.expensetracker.core.model.ExpenseItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test murni JVM (tanpa Robolectric/Android SDK) utk mapper Entity<->Domain —
 * memastikan tidak ada field yg "hilang" saat data bolak-balik lokal <-> server.
 */
class ExpenseEntityMapperTest {

    @Test
    fun `roundtrip domain ke entity ke domain mempertahankan semua field`() {
        val original = ExpenseItem(
            localId = 7,
            id = "srv-123",
            no = "3",
            tanggal = "2026-08-07",
            bulan = "Agustus",
            kategori = "Bahan Baku",
            nilaiTransfer = 4488000.0,
            uraian = "Tepung Bola Salju 3kg",
            lokasi = "Dapur Produksi",
            supplier = "Sinar Mulia",
            buktiTransaksi = "Nota",
            pembayaran = "Transfer",
            jumlah = 350000.0,
            tanggalPembayaran = "2026-08-08",
            noPV = "PV-001",
            keterangan = "stok bulanan",
            cekTransfer = "OK",
            updatedAt = "2026-08-07T10:00:00Z",
            dirty = true,
            pendingDelete = false,
            clientTempId = "tmp-abc"
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `entity baru tanpa clientTempId dibackfill pakai localId saat dikonversi`() {
        val entity = ExpenseEntity(localId = 42, serverId = "", uraian = "x", clientTempId = null)
        val domain = entity.toDomain()
        assertEquals("42", domain.clientTempId)
    }

    @Test
    fun `toEntity default localId ikut dari item bukan selalu nol`() {
        val item = ExpenseItem(localId = 99, id = "srv-1")
        val entity = item.toEntity()
        assertEquals(99L, entity.localId)
    }
}
