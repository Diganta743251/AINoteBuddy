package com.ainotebuddy.app.ai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ainotebuddy.app.data.NoteEntity
import com.google.android.gms.maps.model.LatLng
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidAIService(private val context: Context) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var textToSpeech: TextToSpeech? = null
    
    init {
        initializeTextToSpeech()
    }
    
    /**
     * Extract text from images using ML Kit OCR
     */
    suspend fun extractTextFromImage(imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                val image = InputImage.fromBitmap(bitmap, 0)
                
                suspendCancellableCoroutine { continuation ->
                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val extractedText = visionText.text
                            continuation.resume(extractedText)
                        }
                        .addOnFailureListener { exception ->
                            continuation.resumeWithException(exception)
                        }
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * Extract text from camera capture
     */
    suspend fun extractTextFromCamera(bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            suspendCancellableCoroutine { continuation ->
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        continuation.resume(extractedText)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
        }
    }
    
    /**
     * Smart categorization using Android context
     */
    suspend fun suggestCategoryWithContext(title: String, content: String, location: LatLng? = null): String {
        return withContext(Dispatchers.Default) {
            val text = "$title $content".lowercase()
            
            // Location-based categorization
            location?.let { loc ->
                val nearbyPlaces = getNearbyPlaces(loc)
                if (nearbyPlaces.any { place -> place.contains("restaurant") || place.contains("cafe") }) {
                    return@withContext "Food & Dining"
                }
                if (nearbyPlaces.any { place -> place.contains("gym") || place.contains("fitness") }) {
                    return@withContext "Health & Fitness"
                }
                if (nearbyPlaces.any { place -> place.contains("office") || place.contains("work") }) {
                    return@withContext "Work"
                }
            }
            
            // Time-based categorization
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..11 -> if (text.contains("breakfast") || text.contains("morning")) return@withContext "Personal"
                in 12..17 -> if (text.contains("lunch") || text.contains("meeting")) return@withContext "Work"
                in 18..22 -> if (text.contains("dinner") || text.contains("evening")) return@withContext "Personal"
            }
            
            // Content-based categorization
            when {
                text.contains("meeting") || text.contains("agenda") || text.contains("minutes") -> "Work"
                text.contains("todo") || text.contains("task") || text.contains("reminder") -> "Tasks"
                text.contains("idea") || text.contains("concept") || text.contains("brainstorm") -> "Ideas"
                text.contains("recipe") || text.contains("cook") || text.contains("food") -> "Recipes"
                text.contains("code") || text.contains("programming") || text.contains("bug") -> "Code"
                text.contains("book") || text.contains("read") || text.contains("review") -> "Reading"
                text.contains("travel") || text.contains("trip") || text.contains("vacation") -> "Travel"
                text.contains("health") || text.contains("fitness") || text.contains("exercise") -> "Health"
                text.contains("finance") || text.contains("money") || text.contains("budget") -> "Finance"
                text.contains("personal") || text.contains("diary") || text.contains("journal") -> "Personal"
                else -> "General"
            }
        }
    }
    
    /**
     * Smart tagging with Android context
     */
    suspend fun suggestTagsWithContext(title: String, content: String, location: LatLng? = null): List<String> {
        return withContext(Dispatchers.Default) {
            val tags = mutableListOf<String>()
            val text = "$title $content".lowercase()
            
            // Location-based tags
            location?.let { loc ->
                val nearbyPlaces = getNearbyPlaces(loc)
                nearbyPlaces.take(3).forEach { place ->
                    tags.add(place.lowercase().replace(" ", ""))
                }
            }
            
            // Time-based tags
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            when (dayOfWeek) {
                Calendar.MONDAY -> tags.add("monday")
                Calendar.FRIDAY -> tags.add("friday")
                Calendar.SATURDAY, Calendar.SUNDAY -> tags.add("weekend")
            }
            
            when (hour) {
                in 6..11 -> tags.add("morning")
                in 12..17 -> tags.add("afternoon")
                in 18..22 -> tags.add("evening")
                else -> tags.add("night")
            }
            
            // Content-based tags
            val words = text.split(" ").filter { it.length > 3 }
            val wordFrequency = words.groupingBy { it }.eachCount()
            
            wordFrequency.entries
                .sortedByDescending { it.value }
                .take(5)
                .forEach { (word, _) ->
                    if (word.length > 3 && !tags.contains(word)) {
                        tags.add(word)
                    }
                }
            
            // Contextual tags
            if (text.contains("urgent") || text.contains("important")) tags.add("important")
            if (text.contains("project")) tags.add("project")
            if (text.contains("deadline")) tags.add("deadline")
            if (text.contains("idea")) tags.add("idea")
            if (text.contains("draft")) tags.add("draft")
            if (text.contains("meeting")) tags.add("meeting")
            if (text.contains("call")) tags.add("call")
            if (text.contains("email")) tags.add("email")
            
            tags.distinct().take(10)
        }
    }
    
    /**
     * Voice-to-text with Android speech recognition
     */
    fun createVoiceRecognitionIntent(): Intent {
        return Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your note...")
        }
    }
    
    /**
     * Text-to-speech for reading notes aloud
     */
    suspend fun speakNote(text: String, utteranceId: String = "note_reading") {
        withContext(Dispatchers.Main) {
            textToSpeech?.let { tts ->
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }
    }
    
    /**
     * Smart note summarization
     */
    suspend fun summarizeNote(note: NoteEntity): String {
        return withContext(Dispatchers.Default) {
            val content = note.content
            if (content.length <= 100) {
                return@withContext content
            }
            
            val sentences = content.split(". ")
            val keySentences = sentences.take(3)
            keySentences.joinToString(". ") + "."
        }
    }
    
    /**
     * Find related notes based on content similarity
     */
    suspend fun findRelatedNotes(currentNote: NoteEntity, allNotes: List<NoteEntity>): List<NoteEntity> {
        return withContext(Dispatchers.Default) {
            val currentWords = (currentNote.title + " " + currentNote.content)
                .lowercase()
                .split(" ")
                .filter { it.length > 3 }
                .toSet()
            
            allNotes.filter { note ->
                note.id != currentNote.id && note.content.isNotEmpty()
            }.map { note ->
                val noteWords = (note.title + " " + note.content)
                    .lowercase()
                    .split(" ")
                    .filter { it.length > 3 }
                    .toSet()
                
                val intersection = currentWords.intersect(noteWords)
                val similarity = intersection.size.toFloat() / currentWords.size
                Pair(note, similarity)
            }.filter { (_, similarity) ->
                similarity > 0.1f
            }.sortedByDescending { (_, similarity) ->
                similarity
            }.take(5).map { (note, _) ->
                note
            }
        }
    }
    
    /**
     * Smart reminder suggestions
     */
    suspend fun suggestReminders(note: NoteEntity): List<String> {
        return withContext(Dispatchers.Default) {
            val suggestions = mutableListOf<String>()
            val content = note.content.lowercase()
            
            if (content.contains("meeting") || content.contains("appointment")) {
                suggestions.add("Set meeting reminder")
            }
            if (content.contains("deadline") || content.contains("due")) {
                suggestions.add("Set deadline reminder")
            }
            if (content.contains("call") || content.contains("phone")) {
                suggestions.add("Set call reminder")
            }
            if (content.contains("follow up") || content.contains("follow-up")) {
                suggestions.add("Set follow-up reminder")
            }
            
            suggestions
        }
    }
    
    /**
     * Backup suggestions based on note importance
     */
    suspend fun suggestBackupStrategy(note: NoteEntity): String {
        return withContext(Dispatchers.Default) {
            val content = note.content.lowercase()
            val title = note.title.lowercase()
            
            when {
                content.contains("important") || content.contains("urgent") || title.contains("important") -> {
                    "High priority - Backup to cloud and local storage"
                }
                content.contains("project") || content.contains("work") -> {
                    "Work note - Backup to cloud with version control"
                }
                content.contains("personal") || content.contains("private") -> {
                    "Personal note - Encrypted backup to secure cloud"
                }
                else -> {
                    "Regular note - Standard cloud backup"
                }
            }
        }
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                })
            }
        }
    }
    
    private fun getNearbyPlaces(location: LatLng): List<String> {
        // Mock implementation - in real app, use Places API
        return listOf("Coffee Shop", "Restaurant", "Office Building")
    }
    
    fun cleanup() {
        textToSpeech?.shutdown()
        textRecognizer.close()
    }
} 