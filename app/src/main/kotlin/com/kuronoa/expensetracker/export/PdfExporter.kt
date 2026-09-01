package com.kuronoa.expensetracker.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.kuronoa.expensetracker.BuildConfig
import com.kuronoa.expensetracker.core.logic.CurrencyFormatter
import com.kuronoa.expensetracker.core.model.ExpenseItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ekspor daftar pengeluaran satu bulan menjadi file PDF sederhana (tabel +
 * total), memakai android.graphics.pdf.PdfDocument bawaan Android — tidak
 * perlu library tambahan. Halaman otomatis bersambung (multi-page) kalau
 * jumlah baris pengeluaran terlalu banyak untuk satu halaman A4.
 */
object PdfExporter {

    // Ukuran A4 dalam satuan point (72 dpi), standar android.graphics.pdf.PdfDocument.
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val ROW_HEIGHT = 20f
    private const val ROWS_TOP_MARGIN = 90f

    /** Buat file PDF di cache dir aplikasi & kembalikan File-nya. */
    fun export(context: Context, bulan: String, items: List<ExpenseItem>): File {
        val visible = items.filter { !it.pendingDelete }.sortedBy { it.tanggal }

        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val subPaint = Paint().apply { textSize = 10f; color = 0xFF666666.toInt() }
        val headerPaint = Paint().apply { textSize = 9f; isFakeBoldText = true }
        val cellPaint = Paint().apply { textSize = 9f }
        val totalPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val linePaint = Paint().apply { color = 0xFFCCCCCC.toInt(); strokeWidth = 0.5f }

        // Kolom tabel: Tanggal | Uraian | Kategori | Supplier | Nilai Transfer | Jumlah
        val colX = floatArrayOf(MARGIN, MARGIN + 55f, MARGIN + 185f, MARGIN + 265f, MARGIN + 365f, MARGIN + 470f)
        val pageWidthF = PAGE_WIDTH.toFloat()
        val pageHeightF = PAGE_HEIGHT.toFloat()

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = drawPageHeader(canvas, bulan, pageNumber, titlePaint, subPaint)
        y = drawTableHeader(canvas, y, colX, pageWidthF, headerPaint, linePaint)

        var total = 0.0
        for (item in visible) {
            if (y + ROW_HEIGHT > pageHeightF - MARGIN - 30f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = drawPageHeader(canvas, bulan, pageNumber, titlePaint, subPaint)
                y = drawTableHeader(canvas, y, colX, pageWidthF, headerPaint, linePaint)
            }
            canvas.drawText(item.tanggal.ifBlank { "-" }, colX[0], y, cellPaint)
            canvas.drawText(truncate(item.uraian.ifBlank { "-" }, 26), colX[1], y, cellPaint)
            canvas.drawText(truncate(item.kategori.ifBlank { "-" }, 14), colX[2], y, cellPaint)
            canvas.drawText(truncate(item.supplier.ifBlank { "-" }, 16), colX[3], y, cellPaint)
            canvas.drawText(item.nilaiTransfer?.let { CurrencyFormatter.format(it) } ?: "-", colX[4], y, cellPaint)
            canvas.drawText(CurrencyFormatter.format(item.jumlah), colX[5], y, cellPaint)
            total += item.jumlah
            y += ROW_HEIGHT
        }

        y += 12f
        canvas.drawLine(MARGIN, y, pageWidthF - MARGIN, y, linePaint)
        y += 18f
        canvas.drawText("Total $bulan: ${CurrencyFormatter.format(total)}", colX[4], y, totalPaint)

        document.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeMonth = bulan.ifBlank { "Semua" }.replace(Regex("[^A-Za-z0-9]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(exportDir, "Pengeluaran_${safeMonth}_$timestamp.pdf")
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
        return outFile
    }

    /** URI aman (lewat FileProvider) untuk dibagikan/disimpan lewat Intent. */
    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)

    private fun drawPageHeader(
        canvas: Canvas,
        bulan: String,
        pageNumber: Int,
        titlePaint: Paint,
        subPaint: Paint
    ): Float {
        canvas.drawText("Laporan Pengeluaran Kuronoa Bakery", MARGIN, MARGIN + 16f, titlePaint)
        canvas.drawText("Bulan: $bulan   •   Halaman $pageNumber", MARGIN, MARGIN + 34f, subPaint)
        canvas.drawText(BuildConfig.APP_CREDIT, MARGIN, MARGIN + 48f, subPaint)
        return MARGIN + ROWS_TOP_MARGIN - 20f
    }

    private fun drawTableHeader(
        canvas: Canvas,
        yStart: Float,
        colX: FloatArray,
        pageWidthF: Float,
        headerPaint: Paint,
        linePaint: Paint
    ): Float {
        var y = yStart
        canvas.drawText("Tanggal", colX[0], y, headerPaint)
        canvas.drawText("Uraian", colX[1], y, headerPaint)
        canvas.drawText("Kategori", colX[2], y, headerPaint)
        canvas.drawText("Supplier", colX[3], y, headerPaint)
        canvas.drawText("Nilai Transfer", colX[4], y, headerPaint)
        canvas.drawText("Jumlah", colX[5], y, headerPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, pageWidthF - MARGIN, y, linePaint)
        y += ROW_HEIGHT
        return y
    }

    private fun truncate(s: String, max: Int): String = if (s.length > max) s.take(max - 1) + "…" else s
}
