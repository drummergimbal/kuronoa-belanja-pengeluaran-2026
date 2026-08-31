package com.kuronoa.expensetracker.core.logic

import com.kuronoa.expensetracker.core.model.ExpenseItem

object ExpenseValidator {

    /** Kembalikan daftar pesan error; list kosong berarti valid. */
    fun validate(item: ExpenseItem): List<String> {
        val errors = mutableListOf<String>()
        if (item.tanggal.isBlank() || !isIsoDate(item.tanggal)) {
            errors += "Tanggal wajib diisi dengan format yang benar."
        }
        if (item.kategori.isBlank()) {
            errors += "Kategori wajib dipilih."
        }
        if (item.uraian.isBlank()) {
            errors += "Uraian/nama belanja wajib diisi."
        }
        if (item.jumlah <= 0.0) {
            errors += "Jumlah harus lebih besar dari 0."
        }
        if (item.lokasi.isBlank()) {
            errors += "Lokasi (Toko/Dapur Produksi) wajib dipilih."
        }
        if (item.pembayaran.isBlank()) {
            errors += "Metode pembayaran wajib dipilih."
        }
        return errors
    }

    fun isValid(item: ExpenseItem): Boolean = validate(item).isEmpty()

    private val ISO_DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")
    fun isIsoDate(s: String): Boolean = ISO_DATE_REGEX.matches(s)

    /** "2026-08-07" -> "Agustus" */
    fun monthNameFromIsoDate(iso: String): String {
        if (!isIsoDate(iso)) return ""
        val monthNum = iso.substring(5, 7).toIntOrNull() ?: return ""
        return MONTHS.getOrNull(monthNum - 1) ?: ""
    }

    val MONTHS = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
}
