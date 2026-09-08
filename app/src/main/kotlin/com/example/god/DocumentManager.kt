package com.example.god

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.BufferedReader
import java.io.InputStreamReader

/** Real document reader. Text files are read directly; PDFs are extracted with PDFBox-Android. */
class DocumentManager(private val context: Context) {
    init { PDFBoxResourceLoader.init(context.applicationContext) }

    fun readText(uri: Uri, maxChars: Int = 150_000): String {
        val type = context.contentResolver.getType(uri).orEmpty().lowercase()
        return try {
            if (type.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")) {
                extractPdf(uri, maxChars)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText().take(maxChars) }
                }.orEmpty()
            }
        } catch (_: Exception) { "" }
    }

    private fun extractPdf(uri: Uri, maxChars: Int): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                return PDFTextStripper().getText(document).take(maxChars)
            }
        }
        return ""
    }

    fun search(uri: Uri, query: String, maxChars: Int = 20_000): String {
        val text = readText(uri, 500_000)
        if (text.isBlank() || query.isBlank()) return ""
        val q = query.trim().lowercase()
        val lines = text.lines()
        val hits = lines.filter { it.lowercase().contains(q) }
        return hits.joinToString("\n").take(maxChars)
    }

    fun summarizeInput(uri: Uri, maxChars: Int = 40_000): String {
        return readText(uri, maxChars)
    }

    fun isPdf(uri: Uri): Boolean =
        context.contentResolver.getType(uri)?.contains("pdf", ignoreCase = true) == true ||
            uri.toString().lowercase().endsWith(".pdf")

    fun isLikelyText(uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri).orEmpty().lowercase()
        return type.startsWith("text/") || type.contains("json") || type.contains("xml") ||
            type.contains("javascript") || type.contains("csv")
    }
}
