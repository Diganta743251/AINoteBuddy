package com.ainotebuddy.app.search

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.util.FileUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Engine for performing visual searches on images and documents
 */
class VisualSearchEngine(private val context: Context) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val cache = VisualSearchCache(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Performance optimization: Process images at a lower resolution
    private val MAX_IMAGE_DIMENSION = 1024
    
    /**
     * Extract text from an image using ML Kit Text Recognition with caching and performance optimizations
     */
    suspend fun extractTextFromImage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        // Check cache first
        cache.getCachedText(imageUri)?.let { return@withContext it }
        
        try {
            // Load and preprocess image
            val bitmap = loadAndPreprocessImage(imageUri) ?: return@withContext ""
            
            // Process image in chunks to avoid OOM
            val textResults = processImageInChunks(bitmap)
            
            // Cache the result
            if (textResults.isNotBlank()) {
                cache.cacheText(imageUri, textResults)
            }
            
            textResults
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Load and preprocess image for optimal OCR performance
     */
    private suspend fun loadAndPreprocessImage(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            // Get image dimensions
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            
            // Decode with inSampleSize set
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            inputStream2.close()
            
            // Rotate if needed
            if (bitmap == null) return@withContext null
            val rotation = getRotationFromExif(uri)
            return@withContext if (rotation != 0f) rotateBitmap(bitmap, rotation) else bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Process large images in chunks to avoid OOM errors
     */
    private suspend fun processImageInChunks(bitmap: Bitmap): String {
        val chunkSize = 1000 // pixels
        val width = bitmap.width
        val height = bitmap.height
        val results = mutableListOf<Deferred<String>>()
        
        // Process image in chunks in parallel
        for (y in 0 until height step chunkSize) {
            for (x in 0 until width step chunkSize) {
                val chunkWidth = minOf(chunkSize, width - x)
                val chunkHeight = minOf(chunkSize, height - y)
                
                if (chunkWidth <= 0 || chunkHeight <= 0) continue
                
                val chunk = Bitmap.createBitmap(bitmap, x, y, chunkWidth, chunkHeight)
                results.add(scope.async(Dispatchers.IO) {
                    try {
                        val inputImage = InputImage.fromBitmap(chunk, 0)
                        textRecognizer.process(inputImage).await().text
                    } catch (e: Exception) {
                        ""
                    } finally {
                        chunk.recycle()
                    }
                })
            }
        }
        
        // Combine results
        return results.awaitAll().filter { it.isNotBlank() }.joinToString("\n").trim()
    }
    
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    
    private fun getRotationFromExif(uri: Uri): Float {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input)
            } ?: return 0f
            
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Extract text from a document (PDF, DOCX, etc.) with caching
     */
    suspend fun extractTextFromDocument(documentUri: Uri): String = withContext(Dispatchers.IO) {
        // Check cache first
        cache.getCachedText(documentUri)?.let { return@withContext it }
        
        try {
            val text = FileUtils.extractTextFromDocument(context, documentUri)
            
            // Cache the result
            if (text.isNotBlank()) {
                cache.cacheText(documentUri, text)
            }
            
            text
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Clear the visual search cache
     */
    suspend fun clearCache() {
        cache.clearCache()
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): VisualSearchCache.CacheStats {
        return cache.getCacheStats()
    }
    
    /**
     * Index visual content from a note's attachments
     */
    suspend fun indexNoteAttachments(note: NoteWithAttachments) {
        note.attachments.forEach { attachment ->
            when (attachment.type) {
                "image" -> {
                    val text = extractTextFromImage(Uri.parse(attachment.uri))
                    // Store extracted text in search index
                    updateSearchIndex(note.note.id.toString(), text, "image")
                }
                "document" -> {
                    val text = extractTextFromDocument(Uri.parse(attachment.uri))
                    // Store extracted text in search index
                    updateSearchIndex(note.note.id.toString(), text, "document")
                }
                // Add more attachment types as needed
            }
        }
    }
    
    /**
     * Search for visual content across all notes
     */
    suspend fun searchVisualContent(query: String, notes: List<NoteWithAttachments>): List<Pair<Note, Float>> {
        // Implement visual similarity search
        // This is a placeholder - in a real app, you would use a proper search index
        return emptyList()
    }
    
    private fun updateSearchIndex(noteId: String, extractedText: String, contentType: String) {
        // Update the search index with extracted text
        // This would typically involve updating a database or search index
    }
}

/**
 * Composable function to provide VisualSearchEngine
 */
@Composable
fun rememberVisualSearchEngine(): VisualSearchEngine {
    val context = LocalContext.current
    return remember { VisualSearchEngine(context) }
}

/**
 * Extension function to check if a note has visual content that can be searched
 */
fun NoteWithAttachments.hasSearchableVisualContent(): Boolean {
    return attachments.any { it.type in listOf("image", "document") }
}

// Minimal data classes to satisfy references when domain entities are not present
data class Attachment(
    val type: String,
    val uri: String
)

data class NoteWithAttachments(
    val note: Note,
    val attachments: List<Attachment>
)
