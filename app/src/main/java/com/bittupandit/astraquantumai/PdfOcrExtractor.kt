package com.bittupandit.astraquantumai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/** On-device OCR fallback for scanned PDFs. Pages are processed one at a time. */
object PdfOcrExtractor {
    private const val MAX_PAGES = 60
    private const val MAX_CHARS = 80_000
    private const val MAX_PAGE_EDGE = 1_600

    suspend fun extract(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("PDF फ़ाइल खोली नहीं जा सकी।")
        val renderer = PdfRenderer(descriptor)
        val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        try {
            require(renderer.pageCount > 0) { "PDF में कोई पेज नहीं मिला।" }
            val pageLimit = min(renderer.pageCount, MAX_PAGES)
            val output = StringBuilder()
            for (pageIndex in 0 until pageLimit) {
                coroutineContext.ensureActive()
                val page = renderer.openPage(pageIndex)
                try {
                    val longestEdge = max(page.width, page.height).coerceAtLeast(1)
                    val scale = min(1.0, MAX_PAGE_EDGE.toDouble() / longestEdge)
                    val width = max(1, (page.width * scale).toInt())
                    val height = max(1, (page.height * scale).toInt())
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    try {
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, Rect(0, 0, width, height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val latin = latinRecognizer.process(image).awaitResult()
                        val devanagari = devanagariRecognizer.process(image).awaitResult()
                        data class OcrLine(val top: Int, val left: Int, val text: String)
                        val lines = buildList {
                            latin.textBlocks.flatMap { it.lines }.forEach { line ->
                                if (line.text.none { it in '\u0900'..'\u097F' }) {
                                    add(OcrLine(line.boundingBox?.top ?: 0, line.boundingBox?.left ?: 0, line.text))
                                }
                            }
                            devanagari.textBlocks.flatMap { it.lines }.forEach { line ->
                                if (line.text.any { it in '\u0900'..'\u097F' }) {
                                    add(OcrLine(line.boundingBox?.top ?: 0, line.boundingBox?.left ?: 0, line.text))
                                }
                            }
                        }
                        val pageText = lines.sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left })
                            .joinToString("\n") { it.text }.trim()
                        if (pageText.isNotBlank()) {
                            val remaining = MAX_CHARS - output.length
                            if (remaining <= 0) break
                            output.append("\n\n[पेज ").append(pageIndex + 1).append("]\n")
                                .append(pageText.take(remaining))
                        }
                    } finally {
                        bitmap.recycle()
                    }
                } finally {
                    page.close()
                }
                if (output.length >= MAX_CHARS) break
            }
            if (output.isBlank()) throw IllegalArgumentException("PDF के पन्नों से पढ़ने योग्य टेक्स्ट नहीं मिला।")
            if (renderer.pageCount > pageLimit) {
                output.append("\n\n[सीमा: कुल ").append(renderer.pageCount)
                    .append(" पेज में से पहले ").append(pageLimit).append(" पेज OCR किए गए।]")
            } else if (output.length >= MAX_CHARS) {
                output.append("\n\n[सीमा: OCR टेक्स्ट 80,000 अक्षरों पर रोका गया।]")
            }
            output.toString()
        } finally {
            latinRecognizer.close()
            devanagariRecognizer.close()
            renderer.close()
            descriptor.close()
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) continuation.resume(value)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
}
