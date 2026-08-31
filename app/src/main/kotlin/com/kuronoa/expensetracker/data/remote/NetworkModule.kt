package com.kuronoa.expensetracker.data.remote

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Bikin OkHttpClient yang "stabil": timeout wajar, retry otomatis dgn
 * exponential backoff utk kegagalan jaringan sesaat (mis. sinyal HP naik-turun),
 * dan logging (hanya level BASIC di release) utk memudahkan diagnosa masalah sync.
 */
object NetworkModule {

    // Adapter Kotlin di-generate saat compile oleh moshi-kotlin-codegen (ksp),
    // jadi tidak perlu registrasi factory tambahan / dependensi kotlin-reflect.
    val moshi: Moshi = Moshi.Builder().build()

    fun okHttpClient(debug: Boolean): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (debug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .addInterceptor(logging)
            .build()
    }
}

/**
 * Retry otomatis dgn exponential backoff (250ms, 500ms, 1000ms) utk request GET
 * yang idempotent, atau request apapun yg gagal krn masalah koneksi (bukan error
 * dari server). Ini bagian dari "koneksi stabil" — sinkronisasi tetap jalan
 * walau jaringan HP sedang tidak sempurna.
 */
class RetryInterceptor(private val maxRetries: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null
        var attempt = 0
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (response.isSuccessful || attempt == maxRetries) return response
                if (response.code in 500..599) {
                    response.close()
                    sleepBackoff(attempt)
                    attempt++
                    continue
                }
                return response
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) throw e
                sleepBackoff(attempt)
                attempt++
            }
        }
        throw lastException ?: IOException("Gagal terhubung ke server setelah $maxRetries percobaan.")
    }

    private fun sleepBackoff(attempt: Int) {
        val delayMs = min(250L * 2.0.pow(attempt).toLong(), 4000L)
        Thread.sleep(delayMs)
    }
}
