package com.kuronoa.expensetracker.data.remote

import com.kuronoa.expensetracker.core.model.ExpenseItem
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO JSON persis mengikuti kontrak field Apps Script (lihat apps-script/Code.gs,
 * fungsi rowToItem_). Dipisah dari [ExpenseItem] (model domain) supaya perubahan
 * format API tidak langsung merembet ke seluruh app.
 */
@JsonClass(generateAdapter = true)
data class ExpenseDto(
    val id: String = "",
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
    val clientTempId: String? = null
)

fun ExpenseDto.toDomain(): ExpenseItem = ExpenseItem(
    id = id, no = no, tanggal = tanggal, bulan = bulan, kategori = kategori,
    nilaiTransfer = nilaiTransfer, uraian = uraian, lokasi = lokasi, supplier = supplier,
    buktiTransaksi = buktiTransaksi, pembayaran = pembayaran, jumlah = jumlah,
    tanggalPembayaran = tanggalPembayaran, noPV = noPV, keterangan = keterangan,
    cekTransfer = cekTransfer, updatedAt = updatedAt, dirty = false, pendingDelete = false
)

fun ExpenseItem.toDto(): ExpenseDto = ExpenseDto(
    id = id, no = no, tanggal = tanggal, bulan = bulan, kategori = kategori,
    nilaiTransfer = nilaiTransfer, uraian = uraian, lokasi = lokasi, supplier = supplier,
    buktiTransaksi = buktiTransaksi, pembayaran = pembayaran, jumlah = jumlah,
    tanggalPembayaran = tanggalPembayaran, noPV = noPV, keterangan = keterangan,
    cekTransfer = cekTransfer, updatedAt = updatedAt, clientTempId = clientTempId
)

/** Diimplementasikan semua response DTO supaya SheetsApiClient bisa cek ok/message secara seragam. */
interface ApiResponse {
    val ok: Boolean
    val message: String?
}

@JsonClass(generateAdapter = true)
data class PingResponse(override val ok: Boolean, val serverTime: String? = null, val version: String? = null, override val message: String? = null) : ApiResponse

@JsonClass(generateAdapter = true)
data class MonthsResponse(override val ok: Boolean, val months: List<String> = emptyList(), override val message: String? = null) : ApiResponse

@JsonClass(generateAdapter = true)
data class ListResponse(override val ok: Boolean, val month: String? = null, val items: List<ExpenseDto> = emptyList(), override val message: String? = null) : ApiResponse

@JsonClass(generateAdapter = true)
data class AllResponse(override val ok: Boolean, val months: Map<String, List<ExpenseDto>> = emptyMap(), val serverTime: String? = null, override val message: String? = null) : ApiResponse

@JsonClass(generateAdapter = true)
data class ItemResponse(override val ok: Boolean, val item: ExpenseDto? = null, val error: String? = null, override val message: String? = null) : ApiResponse

@JsonClass(generateAdapter = true)
data class DeleteResponse(override val ok: Boolean, val id: String? = null, val error: String? = null, override val message: String? = null) : ApiResponse

@JsonClass(generateAdapter = true)
data class RecapCategoryDto(val kategori: String, val total: Double)

@JsonClass(generateAdapter = true)
data class RecapMonthDto(val month: String, val total: Double, val count: Int)

@JsonClass(generateAdapter = true)
data class RecapResponse(
    override val ok: Boolean,
    val byMonth: List<RecapMonthDto> = emptyList(),
    val byCategory: List<RecapCategoryDto> = emptyList(),
    val grandTotal: Double = 0.0,
    override val message: String? = null
) : ApiResponse

// ---- batchSync ----

@JsonClass(generateAdapter = true)
data class SyncOperationDto(
    val op: String,               // "create" | "update" | "delete"
    val month: String,
    val clientTempId: String? = null,
    val item: ExpenseDto? = null,
    val id: String? = null
)

@JsonClass(generateAdapter = true)
data class BatchSyncRequest(
    val token: String,
    val action: String = "batchSync",
    val operations: List<SyncOperationDto>,
    val since: String? = null
)

@JsonClass(generateAdapter = true)
data class PushResultDto(
    val op: String,
    val month: String,
    val ok: Boolean,
    val clientTempId: String? = null,
    val id: String? = null,
    val item: ExpenseDto? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class BatchSyncResponse(
    override val ok: Boolean,
    val serverTime: String? = null,
    val pushResults: List<PushResultDto> = emptyList(),
    val months: Map<String, List<ExpenseDto>> = emptyMap(),
    override val message: String? = null
) : ApiResponse

@JsonClass(generateAdapter = true)
data class CreateRequest(val token: String, val action: String = "create", val month: String, val item: ExpenseDto)

@JsonClass(generateAdapter = true)
data class UpdateRequest(val token: String, val action: String = "update", val month: String, val item: ExpenseDto)

@JsonClass(generateAdapter = true)
data class DeleteRequest(val token: String, val action: String = "delete", val month: String, val id: String)
