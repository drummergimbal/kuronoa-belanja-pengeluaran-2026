package com.kuronoa.expensetracker.core.logic

/**
 * Format & parse nilai Rupiah tanpa bergantung pada java.text.NumberFormat
 * (yang perilakunya bisa berbeda antar-locale device), supaya hasilnya
 * konsisten dan gampang di-unit-test di JVM murni.
 */
object CurrencyFormatter {

    /** 1234567.0 -> "Rp 1.234.567" */
    fun format(amount: Double): String {
        val rounded = Math.round(amount)
        val negative = rounded < 0
        val digits = Math.abs(rounded).toString()
        val grouped = StringBuilder()
        for ((index, ch) in digits.reversed().withIndex()) {
            if (index > 0 && index % 3 == 0) grouped.append('.')
            grouped.append(ch)
        }
        val result = grouped.reverse().toString()
        return (if (negative) "-Rp " else "Rp ") + result
    }

    /** "Rp 1.234.567" atau "1234567" atau "1.234.567,50" -> 1234567.0 */
    fun parse(text: String): Double {
        if (text.isBlank()) return 0.0
        var s = text.trim()
            .replace("Rp", "", ignoreCase = true)
            .replace(" ", "")
        val negative = s.startsWith("-")
        if (negative) s = s.substring(1)
        // Buang pemisah ribuan '.', ubah koma desimal ',' jadi '.'
        s = if (s.contains(',')) {
            s.replace(".", "").replace(",", ".")
        } else {
            s.replace(".", "")
        }
        val value = s.toDoubleOrNull() ?: 0.0
        return if (negative) -value else value
    }
}
