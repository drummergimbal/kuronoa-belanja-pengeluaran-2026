package com.kuronoa.expensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuronoa.expensetracker.core.model.ExpenseItem

/**
 * Representasi lokal (Room/SQLite) satu baris pengeluaran. [localId] adalah kunci
 * lokal saja (bukan dikirim ke server); [serverId] kosong berarti item belum
 * pernah disinkronkan.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String = "",
    val no: String = "",
    val tanggal: String = "",
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
    val updatedAt: String? = null,
    val dirty: Boolean = false,
    val pendingDelete: Boolean = false,
    val clientTempId: String? = null
)

fun ExpenseEntity.toDomain(): ExpenseItem = ExpenseItem(
    localId = localId,
    id = serverId,
    no = no,
    tanggal = tanggal,
    bulan = bulan,
    kategori = kategori,
    nilaiTransfer = nilaiTransfer,
    uraian = uraian,
    lokasi = lokasi,
    supplier = supplier,
    buktiTransaksi = buktiTransaksi,
    pembayaran = pembayaran,
    jumlah = jumlah,
    tanggalPembayaran = tanggalPembayaran,
    noPV = noPV,
    keterangan = keterangan,
    cekTransfer = cekTransfer,
    updatedAt = updatedAt,
    dirty = dirty,
    pendingDelete = pendingDelete,
    clientTempId = clientTempId ?: localId.toString()
)

fun ExpenseItem.toEntity(localId: Long = this.localId): ExpenseEntity = ExpenseEntity(
    localId = localId,
    serverId = id,
    no = no,
    tanggal = tanggal,
    bulan = bulan,
    kategori = kategori,
    nilaiTransfer = nilaiTransfer,
    uraian = uraian,
    lokasi = lokasi,
    supplier = supplier,
    buktiTransaksi = buktiTransaksi,
    pembayaran = pembayaran,
    jumlah = jumlah,
    tanggalPembayaran = tanggalPembayaran,
    noPV = noPV,
    keterangan = keterangan,
    cekTransfer = cekTransfer,
    updatedAt = updatedAt,
    dirty = dirty,
    pendingDelete = pendingDelete,
    clientTempId = clientTempId ?: localId.toString()
)
