package com.kuronoa.expensetracker.core.logic

import com.kuronoa.expensetracker.core.model.ExpenseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseValidatorTest {

    private val valid = ExpenseItem(
        tanggal = "2026-08-07",
        kategori = "Bahan Baku",
        uraian = "Tepung Terigu 25kg",
        jumlah = 350000.0,
        lokasi = "Dapur Produksi",
        pembayaran = "Transfer"
    )

    @Test
    fun `item lengkap dinyatakan valid`() {
        assertTrue(ExpenseValidator.isValid(valid))
        assertEquals(0, ExpenseValidator.validate(valid).size)
    }

    @Test
    fun `jumlah nol tidak valid`() {
        val item = valid.copy(jumlah = 0.0)
        assertFalse(ExpenseValidator.isValid(item))
    }

    @Test
    fun `tanggal format salah tidak valid`() {
        val item = valid.copy(tanggal = "07-08-2026")
        assertFalse(ExpenseValidator.isValid(item))
    }

    @Test
    fun `uraian kosong tidak valid`() {
        val item = valid.copy(uraian = "")
        assertFalse(ExpenseValidator.isValid(item))
    }

    @Test
    fun `kategori kosong tidak valid`() {
        val item = valid.copy(kategori = "")
        assertFalse(ExpenseValidator.isValid(item))
    }

    @Test
    fun `deteksi nama bulan dari tanggal iso`() {
        assertEquals("Agustus", ExpenseValidator.monthNameFromIsoDate("2026-08-07"))
        assertEquals("Januari", ExpenseValidator.monthNameFromIsoDate("2026-01-01"))
        assertEquals("Desember", ExpenseValidator.monthNameFromIsoDate("2026-12-31"))
    }

    @Test
    fun `tanggal iso tidak valid mengembalikan string kosong`() {
        assertEquals("", ExpenseValidator.monthNameFromIsoDate("bukan-tanggal"))
    }
}
