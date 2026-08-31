package com.kuronoa.expensetracker.core.logic

import com.kuronoa.expensetracker.core.model.ExpenseItem

data class CategoryTotal(val kategori: String, val total: Double, val percent: Double)
data class MonthTotal(val bulan: String, val total: Double)
data class DashboardSummary(
    val grandTotal: Double,
    val transactionCount: Int,
    val byCategory: List<CategoryTotal>,
    val byMonth: List<MonthTotal>,
    val bySupplier: List<Pair<String, Double>>, // top supplier by spend, terbesar dulu
    val byLokasi: List<Pair<String, Double>>
)

object DashboardAggregator {

    /** Item yang sedang menunggu dihapus (pendingDelete) tidak dihitung. */
    fun summarize(items: List<ExpenseItem>, topSupplierLimit: Int = 5): DashboardSummary {
        val active = items.filter { !it.pendingDelete }
        val grandTotal = active.sumOf { it.jumlah }

        val byCategoryMap = LinkedHashMap<String, Double>()
        for (it in active) {
            val key = it.kategori.ifBlank { "Lainnya" }
            byCategoryMap[key] = (byCategoryMap[key] ?: 0.0) + it.jumlah
        }
        val byCategory = byCategoryMap.entries
            .sortedByDescending { it.value }
            .map { CategoryTotal(it.key, it.value, if (grandTotal > 0) it.value / grandTotal * 100.0 else 0.0) }

        val byMonthMap = LinkedHashMap<String, Double>()
        for (it in active) {
            val key = it.bulan.ifBlank { ExpenseValidator.monthNameFromIsoDate(it.tanggal) }
            if (key.isBlank()) continue
            byMonthMap[key] = (byMonthMap[key] ?: 0.0) + it.jumlah
        }
        val byMonth = ExpenseValidator.MONTHS
            .filter { byMonthMap.containsKey(it) }
            .map { MonthTotal(it, byMonthMap[it] ?: 0.0) }

        val supplierTotals = LinkedHashMap<String, Double>()
        for (it in active) {
            if (it.supplier.isBlank()) continue
            supplierTotals[it.supplier] = (supplierTotals[it.supplier] ?: 0.0) + it.jumlah
        }
        val bySupplier = supplierTotals.entries
            .sortedByDescending { it.value }
            .take(topSupplierLimit)
            .map { it.key to it.value }

        val lokasiTotals = LinkedHashMap<String, Double>()
        for (it in active) {
            val key = it.lokasi.ifBlank { "Lainnya" }
            lokasiTotals[key] = (lokasiTotals[key] ?: 0.0) + it.jumlah
        }
        val byLokasi = lokasiTotals.entries.sortedByDescending { it.value }.map { it.key to it.value }

        return DashboardSummary(
            grandTotal = grandTotal,
            transactionCount = active.size,
            byCategory = byCategory,
            byMonth = byMonth,
            bySupplier = bySupplier,
            byLokasi = byLokasi
        )
    }
}
