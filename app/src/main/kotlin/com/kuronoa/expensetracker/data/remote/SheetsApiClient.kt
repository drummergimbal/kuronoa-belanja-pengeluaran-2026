package com.kuronoa.expensetracker.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiException(message: String, val code: String = "ERROR") : IOException(message)

/**
 * Klien untuk Apps Script Web App. Web App hanya punya SATU endpoint (URL yang
 * dimasukkan pengguna di Settings), semua aksi dibedakan lewat parameter
 * `action`. Semua fungsi suspend, aman dipanggil dari thread manapun (kerja
 * jaringan otomatis dipindah ke Dispatchers.IO).
 */
class SheetsApiClient(
    private val baseUrl: String,
    private val token: String,
    private val client: OkHttpClient,
    private val moshi: com.squareup.moshi.Moshi = NetworkModule.moshi
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun ping(): PingResponse = io { getSync("ping", emptyMap(), PingResponse::class.java, requireToken = false) }

    suspend fun months(): MonthsResponse = io { getSync("months", emptyMap(), MonthsResponse::class.java) }

    suspend fun list(month: String, since: String?): ListResponse = io {
        val params = mutableMapOf("month" to month)
        since?.let { params["since"] = it }
        getSync("list", params, ListResponse::class.java)
    }

    suspend fun all(since: String?): AllResponse = io {
        val params = mutableMapOf<String, String>()
        since?.let { params["since"] = it }
        getSync("all", params, AllResponse::class.java)
    }

    suspend fun recap(): RecapResponse = io { getSync("recap", emptyMap(), RecapResponse::class.java) }

    suspend fun batchSync(operations: List<SyncOperationDto>, since: String?): BatchSyncResponse = io {
        val body = BatchSyncRequest(token = token, operations = operations, since = since)
        postSync(body, BatchSyncRequest::class.java, BatchSyncResponse::class.java)
    }

    suspend fun create(month: String, item: ExpenseDto): ItemResponse = io {
        val body = CreateRequest(token = token, month = month, item = item)
        postSync(body, CreateRequest::class.java, ItemResponse::class.java)
    }

    suspend fun update(month: String, item: ExpenseDto): ItemResponse = io {
        val body = UpdateRequest(token = token, month = month, item = item)
        postSync(body, UpdateRequest::class.java, ItemResponse::class.java)
    }

    suspend fun delete(month: String, id: String): DeleteResponse = io {
        val body = DeleteRequest(token = token, month = month, id = id)
        postSync(body, DeleteRequest::class.java, DeleteResponse::class.java)
    }

    private fun <T> getSync(
        action: String,
        params: Map<String, String>,
        clazz: Class<T>,
        requireToken: Boolean = true
    ): T {
        val httpUrl = baseUrl.toHttpUrlOrNull()
            ?: throw ApiException("URL Apps Script tidak valid. Cek lagi di Pengaturan.", "BAD_URL")
        val builder = httpUrl.newBuilder().addQueryParameter("action", action)
        if (requireToken) builder.addQueryParameter("token", token)
        params.forEach { (k, v) -> builder.addQueryParameter(k, v) }

        val request = Request.Builder().url(builder.build()).get().build()
        return executeAndParse(request, clazz)
    }

    private fun <B, T> postSync(body: B, bodyClass: Class<B>, responseClass: Class<T>): T {
        val httpUrl = baseUrl.toHttpUrlOrNull()
            ?: throw ApiException("URL Apps Script tidak valid. Cek lagi di Pengaturan.", "BAD_URL")
        val json = moshi.adapter(bodyClass).toJson(body)
        val requestBody = json.toRequestBody(jsonMedia)
        val request = Request.Builder().url(httpUrl).post(requestBody).build()
        return executeAndParse(request, responseClass)
    }

    private fun <T> executeAndParse(request: Request, clazz: Class<T>): T {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException("Server merespons error HTTP ${response.code}.", "HTTP_${response.code}")
            }
            if (raw.isBlank()) throw ApiException("Server memberi respons kosong.", "EMPTY_RESPONSE")
            val parsed = try {
                moshi.adapter(clazz).fromJson(raw)
            } catch (e: Exception) {
                throw ApiException(
                    "Gagal membaca respons server (bukan JSON valid). Pastikan URL Apps Script benar & sudah di-deploy.",
                    "PARSE_ERROR"
                )
            } ?: throw ApiException("Respons server kosong/tidak dikenali.", "NULL_RESPONSE")

            if (parsed is ApiResponse && !parsed.ok) {
                throw ApiException(parsed.message ?: "Server menolak permintaan.", "SERVER_REJECTED")
            }
            return parsed
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
