package com.kuronoa.expensetracker.core.logic

import com.kuronoa.expensetracker.core.model.ExpenseItem
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardAggregatorTest {

    private fun item(kategori: String, jumlah: Double, bulan: String = "Agustus", supplier: String = "", lokasi: String = "Toko", pendingDelete: Boolean = false) =
        ExpenseItem(
            id = "x", kategori = kategori, jumlah = jumlah, bulan = bulan, supplier = supplier,
            lokasi = lokasi, uraian = "u", tanggal = "2026-08-01", pembayaran = "Cash", pendingDelete = pendingDelete
        )

    @Test
    fun `total keseluruhan menjumlahkan semua item aktif`() {
        val items = listOf(item("Bahan Baku", 1000.0), item("Operasional", 2000.0))
        val summary = DashboardAggregator.summarize(items)
        assertEquals(3000.0, summary.grandTotal, 0.001)
        assertEquals(2, summary.transactionCount)
    }

    @Test
    fun `item pendingDelete tidak dihitung`() {
        val items = listOf(item("Bahan Baku", 1000.0), item("Operasional", 2000.0, pendingDelete = true))
        val summary = DashboardAggregator.summarize(items)
        assertEquals(1000.0, summary.grandTotal, 0.001)
        assertEquals(1, summary.transactionCount)
    }

    @Test
    fun `breakdown per kategori terurut dari terbesar & persentase benar`() {
        val items = listOf(
            item("Bahan Baku", 3000.0),
            item("Operasional", 1000.0)
        )
        val summary = DashboardAggregator.summarize(items)
        assertEquals("Bahan Baku", summary.byCategory[0].kategori)
        assertEquals(75.0, summary.byCategory[0].percent, 0.001)
        assertEquals("Operasional", summary.byCategory[1].kategori)
        assertEquals(25.0, summary.byCategory[1].percent, 0.001)
    }

    @Test
    fun `breakdown per bulan terurut sesuai urutan kalender`() {
        val items = listOf(
            item("Bahan Baku", 1000.0, bulan = "Desember"),
            item("Bahan Baku", 2000.0, bulan = "Januari")
        )
        val summary = DashboardAggregator.summarize(items)
        assertEquals(listOf("Januari", "Desember"), summary.byMonth.map { it.bulan })
    }

    @Test
    fun `top supplier terurut dari pengeluaran terbesar & supplier kosong diabaikan`() {
        val items = listOf(
            item("Bahan Baku", 500.0, supplier = "Sinar Mulia"),
            item("Bahan Baku", 1500.0, supplier = "Tip Top"),
            item("Bahan Baku", 700.0, supplier = "")
        )
        val summary = DashboardAggregator.summarize(items)
        assertEquals("Tip Top" to 1500.0, summary.bySupplier[0])
        assertEquals("Sinar Mulia" to 500.0, summary.bySupplier[1])
        assertEquals(2, summary.bySupplier.size)
    }

    @Test
    fun `dataset kosong tidak error dan grandTotal nol`() {
        val summary = DashboardAggregator.summarize(emptyList())
        assertEquals(0.0, summary.grandTotal, 0.001)
        assertEquals(0, summary.transactionCount)
        assertEquals(0, summary.byCategory.size)
    }
}
