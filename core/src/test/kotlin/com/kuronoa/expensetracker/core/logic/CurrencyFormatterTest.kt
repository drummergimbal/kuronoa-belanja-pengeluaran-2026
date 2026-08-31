package com.kuronoa.expensetracker.core.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `format ribuan dengan titik`() {
        assertEquals("Rp 1.234.567", CurrencyFormatter.format(1234567.0))
    }

    @Test
    fun `format nol`() {
        assertEquals("Rp 0", CurrencyFormatter.format(0.0))
    }

    @Test
    fun `format angka kecil tanpa titik`() {
        assertEquals("Rp 500", CurrencyFormatter.format(500.0))
    }

    @Test
    fun `format negatif`() {
        assertEquals("-Rp 1.000", CurrencyFormatter.format(-1000.0))
    }

    @Test
    fun `parse dengan prefix Rp dan titik ribuan`() {
        assertEquals(1234567.0, CurrencyFormatter.parse("Rp 1.234.567"), 0.001)
    }

    @Test
    fun `parse angka polos`() {
        assertEquals(50000.0, CurrencyFormatter.parse("50000"), 0.001)
    }

    @Test
    fun `parse dengan koma desimal`() {
        assertEquals(1234.5, CurrencyFormatter.parse("1.234,5"), 0.001)
    }

    @Test
    fun `parse string kosong menjadi nol`() {
        assertEquals(0.0, CurrencyFormatter.parse(""), 0.001)
    }

    @Test
    fun `roundtrip format lalu parse`() {
        val original = 9606181.0
        val formatted = CurrencyFormatter.format(original)
        val parsed = CurrencyFormatter.parse(formatted)
        assertEquals(original, parsed, 0.001)
    }
}
