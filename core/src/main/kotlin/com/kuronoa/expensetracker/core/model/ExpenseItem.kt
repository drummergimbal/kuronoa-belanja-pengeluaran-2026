package com.kuronoa.expensetracker.core.model

/**
 * Satu baris pengeluaran (URAIAN) pada sheet bulanan "Pengeluaran Belanja 2026".
 *
 * [id] kosong ("") berarti item ini baru dibuat secara lokal dan belum pernah
 * disinkronkan ke server (belum punya ID dari Apps Script).
 *
 * [updatedAt] & [dirty] dipakai oleh [com.kuronoa.expensetracker.core.logic.SyncMerger]
 * untuk menentukan arah sinkronisasi (mana yang perlu di-push, mana yang perlu ditarik).
 */
data class ExpenseItem(
    val localId: Long = 0,              // ID lokal (Room) saja — 0 berarti item belum pernah disimpan lokal.
    val id: String = "",
    val no: String = "",
    val tanggal: String = "",           // format ISO "yyyy-MM-dd"
    val bulan: String = "",
    val kategori: String = "",
    val nilaiTransfer: Double? = null,
    val uraian: String = "",
    val lokasi: String = "",
    val supplier: String = "",
    val buktiTransaksi: String = "",
    val pembayaran: String = "",
    val jumlah: Double = 0.0,
    val tanggalPembayaran: String = "",
    val noPV: String = "",
    val keterangan: String = "",
    val cekTransfer: String = "",
    val updatedAt: String? = null,      // format ISO 8601 "yyyy-MM-dd'T'HH:mm:ss'Z'", dari server
    val dirty: Boolean = false,         // true = ada perubahan lokal yang belum ter-push
    val pendingDelete: Boolean = false, // true = dihapus lokal, menunggu di-push ke server
    val clientTempId: String? = null    // dipakai utk mencocokkan hasil create batch-sync
) {
    val isNew: Boolean get() = id.isBlank()

    companion object {
        val KATEGORI_OPTIONS = listOf(
            "Bahan Baku", "Operasional", "Perlengkapan", "Gaji", "Sewa", "Lainnya"
        )
        val LOKASI_OPTIONS = listOf("Toko", "Dapur Produksi")
        val PEMBAYARAN_OPTIONS = listOf("Cash", "Transfer")
    }
}
