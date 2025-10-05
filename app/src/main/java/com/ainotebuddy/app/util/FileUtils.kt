package com.ainotebuddy.app.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {
    fun getFileName(uri: Uri, context: Context): String {

        var name = "unknown"
        val resolver = context.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    name = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {
            // ignore
        } finally {
            cursor?.close()
        }
        return name
    }

    // Minimal placeholder to allow document text extraction calls to compile.
    // In a full implementation, parse text content from PDFs or other document types.
    fun extractTextFromDocument(context: Context, uri: Uri): String {
        // Best-effort: if it's a text/* MIME type, attempt to read as UTF-8 string; otherwise return empty.
        return try {
            val type = context.contentResolver.getType(uri) ?: ""
            if (type.startsWith("text/")) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}