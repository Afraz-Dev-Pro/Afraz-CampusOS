package com.afraz.campusos.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36

    fun fromNotes(context: Context, title: String, pages: List<String>): File {
        require(pages.isNotEmpty()) { "Add at least one page of notes" }
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, text ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
                drawTextPage(page.canvas, title, text, index + 1)
                document.finishPage(page)
            }
            return save(context, title, document)
        } finally {
            document.close()
        }
    }

    fun fromImages(context: Context, title: String, images: List<Bitmap>): File {
        require(images.isNotEmpty()) { "Choose at least one image" }
        val document = PdfDocument()
        try {
            images.forEachIndexed { index, bitmap ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
                drawImage(page.canvas, bitmap)
                document.finishPage(page)
            }
            return save(context, title, document)
        } finally {
            document.close()
        }
    }

    fun fromImageUris(context: Context, title: String, uris: List<Uri>): File {
        val bitmaps = uris.map { uri ->
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                ?: throw IllegalArgumentException("Unable to open selected image")
        }
        return try {
            fromImages(context, title, bitmaps)
        } finally {
            bitmaps.forEach(Bitmap::recycle)
        }
    }

    private fun drawTextPage(canvas: Canvas, title: String, content: String, pageNumber: Int) {
        val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.DKGRAY; textSize = 12f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL) }
        canvas.drawText(title.take(80), MARGIN.toFloat(), 54f, heading)
        canvas.drawLine(MARGIN.toFloat(), 68f, (PAGE_WIDTH - MARGIN).toFloat(), 68f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCBD5E1.toInt(); strokeWidth = 1f })
        val layout = StaticLayout.Builder.obtain(content, 0, content.length, body, PAGE_WIDTH - 2 * MARGIN)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1f)
            .setIncludePad(true)
            .build()
        val availableHeight = PAGE_HEIGHT - 2 * MARGIN - 70
        require(layout.height <= availableHeight) { "Notes page $pageNumber is too long; split it into separate pages" }
        canvas.save()
        canvas.translate(MARGIN.toFloat(), 86f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawImage(canvas: Canvas, bitmap: Bitmap) {
        val area = Rect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN)
        val ratio = minOf(area.width().toFloat() / bitmap.width, area.height().toFloat() / bitmap.height)
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        val left = (PAGE_WIDTH - width) / 2
        val top = (PAGE_HEIGHT - height) / 2
        canvas.drawBitmap(bitmap, null, Rect(left, top, left + width, top + height), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun save(context: Context, title: String, document: PdfDocument): File {
        val directory = File(context.filesDir, "pdfs").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val safeTitle = title.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "CampusOS" }.take(60)
        val file = File(directory, "$safeTitle-$stamp.pdf")
        FileOutputStream(file).use(document::writeTo)
        return file
    }
}
