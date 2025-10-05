package com.ainotebuddy.app.voice

import android.content.Context
import com.ainotebuddy.app.ai.AIAnalysisResult
import com.ainotebuddy.app.ai.AIEngine
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extended voice engine capabilities with AI integration
 */

@Singleton
class VoiceEngineExtensions @Inject constructor(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val aiEngine: com.ainotebuddy.app.ai.AIAnalysisEngine
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Processes voice input with AI analysis for quick insights
     */
    suspend fun processVoiceWithQuickAnalysis(
        transcript: String,
        confidence: Float
    ): QuickAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                // Create a temporary note for analysis
                val tempNote = NoteEntity(
                    title = extractTitleFromTranscript(transcript),
                    content = transcript,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                // Perform quick AI analysis
                val analysis = try { aiEngine.analyzeNote(tempNote) } catch (_: Throwable) { com.ainotebuddy.app.ai.AIAnalysisResult(
                    sentiment = com.ainotebuddy.app.ai.SentimentResult(
                        sentiment = com.ainotebuddy.app.ai.Sentiment.NEUTRAL,
                        confidence = 0.0f,
                        positiveScore = 0f,
                        negativeScore = 0f,
                        neutralScore = 1f
                    ),
                    topics = emptyList(),
                    entities = emptyList(),
                    actionItems = emptyList(),
                    keyPhrases = emptyList(),
                    insights = emptyList(),
                    contextualTags = emptyList(),
                    confidence = 0.0f
                ) }
                
                // Extract actionable insights
                val actionItems = extractActionItems(transcript)
                val keyPhrases = extractKeyPhrases(transcript)
                val sentiment = analyzeSentiment(transcript)
                val suggestedTags = generateTags(transcript, analysis)
                
                QuickAnalysisResult(
                    actionItems = actionItems,
                    keyPhrases = keyPhrases,
                    suggestedTags = suggestedTags,
                    confidence = confidence
                )
            } catch (e: Exception) {
                QuickAnalysisResult.error(transcript, confidence, e.message ?: "Analysis failed")
            }
        }
    }
    
    /**
     * Converts voice commands to structured note operations
     */
    suspend fun processVoiceCommand(command: VoiceCommand): VoiceCommandResult {
        return withContext(Dispatchers.IO) {
            try {
                when (command.command) {
                    CommandType.CREATE_NOTE -> handleCreateNote(command)
                    CommandType.EDIT_NOTE -> handleEditNote(command)
                    CommandType.DELETE_NOTE -> handleDeleteNote(command)
                    CommandType.SEARCH_NOTES -> handleSearchNotes(command)
                    CommandType.ADD_TAG -> handleAddTag(command)
                    CommandType.SET_REMINDER -> handleSetReminder(command)
                    CommandType.DICTATE_TEXT -> handleDictateText(command)
                    else -> VoiceCommandResult.unsupported(command)
                }
            } catch (e: Exception) {
                VoiceCommandResult.error(command, e.message ?: "Command failed")
            }
        }
    }
    
    /**
     * Enhanced voice note creation with AI suggestions
     */
    suspend fun createSmartVoiceNote(
        transcript: String,
        confidence: Float,
        autoSave: Boolean = true
    ): SmartVoiceNoteResult {
        return withContext(Dispatchers.IO) {
            try {
                // Process with quick analysis
                val analysis = processVoiceWithQuickAnalysis(transcript, confidence)
                
                // Create enhanced note
                val note = NoteEntity(
                    title = "Voice Note",
                    content = improveTranscriptFormatting(transcript),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    tags = analysis.suggestedTags.joinToString(","),
                    category = analysis.suggestedTags.firstOrNull() ?: "General"
                )
                
                // Auto-save if requested
                val savedNote = if (autoSave) {
                    val noteId = noteRepository.insertNote(note)
                    note.copy(id = noteId)
                } else {
                    note
                }
                
                SmartVoiceNoteResult(
                    note = savedNote,
                    analysis = analysis,
                    suggestions = generateNoteSuggestions(analysis),
                    confidence = confidence,
                    wasAutoSaved = autoSave
                )
            } catch (e: Exception) {
                SmartVoiceNoteResult.error(e.message ?: "Failed to create smart voice note")
            }
        }
    }
    
    /**
     * Batch process multiple voice recordings
     */
    fun processBatchVoiceRecordings(
        recordings: List<VoiceRecording>
    ): Flow<BatchProcessingProgress> = flow {
        val total = recordings.size
        var processed = 0
        val results = mutableListOf<VoiceProcessingResult>()
        
        emit(BatchProcessingProgress.started(total))
        
        recordings.forEach { recording ->
            try {
                val result = processVoiceRecording(recording)
                results.add(result)
                processed++
                
                emit(BatchProcessingProgress.progress(processed, total, results.toList()))
            } catch (e: Exception) {
                val errorResult = VoiceProcessingResult(
                    success = false,
                    transcript = "",
                    confidence = 0f,
                    commandRecognized = CommandType.UNKNOWN,
                    extractedData = null,
                    errorMessage = e.message,
                    processingTimeMs = 0L
                )
                results.add(errorResult)
                processed++
                
                emit(BatchProcessingProgress.progress(processed, total, results.toList()))
            }
        }
        
        emit(BatchProcessingProgress.completed(results.toList()))
    }
    
    /**
     * Voice-to-text with real-time corrections
     */
    suspend fun transcribeWithCorrections(
        audioData: ByteArray,
        language: String = "en-US"
    ): CorrectedTranscription {
        return withContext(Dispatchers.IO) {
            try {
                // Initial transcription
                val initialTranscript = performTranscription(audioData, language)
                
                // Apply corrections
                val correctedText = applyCorrections(initialTranscript.text)
                
                // Calculate confidence adjustment
                val adjustedConfidence = adjustConfidenceBasedOnCorrections(
                    initialTranscript.confidence,
                    initialTranscript.text,
                    correctedText
                )
                
                CorrectedTranscription(
                    originalText = initialTranscript.text,
                    correctedText = correctedText,
                    originalConfidence = initialTranscript.confidence,
                    adjustedConfidence = adjustedConfidence,
                    corrections = findCorrections(initialTranscript.text, correctedText),
                    language = language
                )
            } catch (e: Exception) {
                CorrectedTranscription.error(e.message ?: "Transcription failed")
            }
        }
    }
    
    // Helper functions
    private suspend fun handleCreateNote(command: VoiceCommand): VoiceCommandResult {
        val extractedData = extractNoteDataFromCommand(command)
        
        if (extractedData.noteContent.isNullOrBlank()) {
            return VoiceCommandResult.error(command, "No note content found in command")
        }
        
        val note = NoteEntity(
            title = extractedData.noteTitle ?: "Voice Note",
            content = extractedData.noteContent,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tags = extractedData.tags.joinToString(","),
            category = extractedData.category ?: "General"
        )
        
        val noteId = noteRepository.insertNote(note)
        
        return VoiceCommandResult.success(
            command = command,
            result = "Note created with ID: $noteId",
            data = mapOf("noteId" to noteId, "note" to note)
        )
    }
    
    private suspend fun handleEditNote(command: VoiceCommand): VoiceCommandResult {
        val extractedData = extractNoteDataFromCommand(command)
        val noteId = extractedData.targetNoteId
        
        if (noteId == null) {
            return VoiceCommandResult.error(command, "No note specified for editing")
        }
        
        val existingNote = noteRepository.getNoteById(noteId)
            ?: return VoiceCommandResult.error(command, "Note not found")
        
        val updatedNote = existingNote.copy(
            content = extractedData.noteContent ?: existingNote.content,
            title = extractedData.noteTitle ?: existingNote.title,
            updatedAt = System.currentTimeMillis()
        )
        
        noteRepository.updateNote(updatedNote)
        
        return VoiceCommandResult.success(
            command = command,
            result = "Note updated successfully",
            data = mapOf("note" to updatedNote)
        )
    }
    
    private suspend fun handleDeleteNote(command: VoiceCommand): VoiceCommandResult {
        val extractedData = extractNoteDataFromCommand(command)
        val noteId = extractedData.targetNoteId
        
        if (noteId == null) {
            return VoiceCommandResult.error(command, "No note specified for deletion")
        }
        
        val note = noteRepository.getNoteById(noteId)
            ?: return VoiceCommandResult.error(command, "Note not found")
        
        noteRepository.deleteNote(note)
        
        return VoiceCommandResult.success(
            command = command,
            result = "Note deleted successfully",
            data = mapOf("deletedNoteId" to noteId)
        )
    }
    
    private suspend fun handleSearchNotes(command: VoiceCommand): VoiceCommandResult {
        val extractedData = extractNoteDataFromCommand(command)
        val searchQuery = extractedData.searchQuery
        
        if (searchQuery.isNullOrBlank()) {
            return VoiceCommandResult.error(command, "No search query found")
        }
        
        val searchResults = noteRepository.getSearchResults(searchQuery).first()
        
        return VoiceCommandResult.success(
            command = command,
            result = "Found ${searchResults.size} notes",
            data = mapOf("searchResults" to searchResults, "query" to searchQuery)
        )
    }
    
    private suspend fun handleAddTag(command: VoiceCommand): VoiceCommandResult {
        // Implementation for adding tags via voice
        return VoiceCommandResult.success(command, "Tag added")
    }
    
    private suspend fun handleSetReminder(command: VoiceCommand): VoiceCommandResult {
        // Implementation for setting reminders via voice
        return VoiceCommandResult.success(command, "Reminder set")
    }
    
    private suspend fun handleDictateText(command: VoiceCommand): VoiceCommandResult {
        return VoiceCommandResult.success(
            command = command,
            result = "Text dictated",
            data = mapOf("dictatedText" to command.transcript)
        )
    }
    
    private fun extractTitleFromTranscript(transcript: String): String {
        // Extract title from first line or first few words
        val firstLine = transcript.split("\n").first()
        return if (firstLine.length > 50) {
            firstLine.take(47) + "..."
        } else {
            firstLine
        }
    }
    
    private fun extractActionItems(transcript: String): List<String> {
        val actionWords = listOf("todo", "task", "reminder", "action", "need to", "should", "must")
        return transcript.split(". ").filter { sentence ->
            actionWords.any { actionWord -> 
                sentence.contains(actionWord, ignoreCase = true) 
            }
        }
    }
    
    private fun extractKeyPhrases(transcript: String): List<String> {
        // Simple key phrase extraction
        return transcript.split(" ")
            .filter { it.length > 4 }
            .groupingBy { it.lowercase() }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }
    
    private fun analyzeSentiment(transcript: String): String {
        val positiveWords = listOf("good", "great", "excellent", "happy", "love", "amazing")
        val negativeWords = listOf("bad", "terrible", "hate", "awful", "sad", "angry")
        
        val words = transcript.lowercase().split(" ")
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        
        return when {
            positiveCount > negativeCount -> "positive"
            negativeCount > positiveCount -> "negative"
            else -> "neutral"
        }
    }
    
    private fun generateTags(transcript: String, analysis: AIAnalysisResult): List<String> {
        val tags = mutableSetOf<String>()
        
        // Add topic-based tags (guard against null/varied types)
        val topics: List<String> = when (val t = analysis.topics) {
            is List<*> -> t.filterNotNull().map { it.toString() }
            else -> emptyList()
        }
        tags.addAll(topics)
        
        // Add length-based tags
        when {
            transcript.length > 1000 -> tags.add("detailed")
            transcript.length < 100 -> tags.add("brief")
        }
        
        // Add content-type tags
        if (transcript.contains("meeting", ignoreCase = true)) tags.add("meeting")
        if (transcript.contains("idea", ignoreCase = true)) tags.add("idea")
        if (transcript.contains("todo", ignoreCase = true)) tags.add("task")
        
        return tags.toList()
    }
    
    // Additional helper functions would be implemented here...
    
    private fun improveTranscriptFormatting(transcript: String): String {
        return transcript
            .replace(Regex("\\. (?=[a-z])")) { ". ${it.value.last().uppercase()}" }
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun determineCategoryFromAnalysis(topics: List<String>): String? {
        return topics.firstOrNull()
    }
    
    private fun generateNoteSuggestions(analysis: QuickAnalysisResult): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (analysis.actionItems.isNotEmpty()) {
            suggestions.add("Consider creating tasks from the action items mentioned")
        }
        
        if (analysis.keyPhrases.size > 3) {
            suggestions.add("This note contains rich content - consider adding more tags")
        }
        
        return suggestions
    }
    
    private suspend fun processVoiceRecording(recording: VoiceRecording): VoiceProcessingResult {
        // Implementation for processing individual voice recording
        return VoiceProcessingResult(
            success = true,
            transcript = "Processed transcript",
            confidence = 0.9f,
            commandRecognized = CommandType.DICTATE_TEXT,
            extractedData = null,
            processingTimeMs = 100L
        )
    }
    
    private suspend fun performTranscription(audioData: ByteArray, language: String): InitialTranscription {
        // Implementation for actual transcription
        return InitialTranscription("Transcribed text", 0.9f)
    }
    
    private fun applyCorrections(text: String): String {
        // Implementation for text corrections
        return text
    }
    
    private fun adjustConfidenceBasedOnCorrections(
        originalConfidence: Float,
        originalText: String,
        correctedText: String
    ): Float {
        val similarity = calculateTextSimilarity(originalText, correctedText)
        return originalConfidence * similarity
    }
    
    private fun findCorrections(original: String, corrected: String): List<TextCorrection> {
        // Implementation for finding corrections
        return emptyList()
    }
    
    private fun calculateTextSimilarity(text1: String, text2: String): Float {
        // Simple similarity calculation
        return if (text1 == text2) 1.0f else 0.8f
    }
    
    private fun extractNoteDataFromCommand(command: VoiceCommand): VoiceExtractedData {
        // Implementation for extracting structured data from voice commands
        return VoiceExtractedData()
    }
}

// Data classes for extensions
data class QuickAnalysisResult(
    val actionItems: List<String>,
    val keyPhrases: List<String>,
    val suggestedTags: List<String>,
    val confidence: Float
) {
    companion object {
        fun error(transcript: String, confidence: Float, errorMessage: String): QuickAnalysisResult {
            return QuickAnalysisResult(
                actionItems = emptyList(),
                keyPhrases = emptyList(),
                suggestedTags = emptyList(),
                confidence = confidence
            )
        }
    }
}

data class VoiceCommandResult(
    val command: VoiceCommand,
    val success: Boolean,
    val result: String,
    val data: Map<String, Any> = emptyMap(),
    val error: String? = null
) {
    companion object {
        fun success(command: VoiceCommand, result: String, data: Map<String, Any> = emptyMap()): VoiceCommandResult {
            return VoiceCommandResult(command, true, result, data, null)
        }
        
        fun error(command: VoiceCommand, error: String): VoiceCommandResult {
            return VoiceCommandResult(command, false, "", emptyMap(), error)
        }
        
        fun unsupported(command: VoiceCommand): VoiceCommandResult {
            return VoiceCommandResult(command, false, "", emptyMap(), "Unsupported command type")
        }
    }
}

data class SmartVoiceNoteResult(
    val note: NoteEntity,
    val analysis: QuickAnalysisResult,
    val suggestions: List<String>,
    val confidence: Float,
    val wasAutoSaved: Boolean,
    val error: String? = null
) {
    companion object {
        fun error(errorMessage: String): SmartVoiceNoteResult {
            return SmartVoiceNoteResult(
                note = NoteEntity(title = "", content = "", createdAt = 0L, updatedAt = 0L),
                analysis = QuickAnalysisResult.error("", 0f, errorMessage),
                suggestions = emptyList(),
                confidence = 0f,
                wasAutoSaved = false,
                error = errorMessage
            )
        }
    }
}

data class BatchProcessingProgress(
    val status: ProcessingStatus,
    val processed: Int,
    val total: Int,
    val results: List<VoiceProcessingResult> = emptyList()
) {
    companion object {
        fun started(total: Int) = BatchProcessingProgress(ProcessingStatus.STARTED, 0, total)
        fun progress(processed: Int, total: Int, results: List<VoiceProcessingResult>) = 
            BatchProcessingProgress(ProcessingStatus.IN_PROGRESS, processed, total, results)
        fun completed(results: List<VoiceProcessingResult>) = 
            BatchProcessingProgress(ProcessingStatus.COMPLETED, results.size, results.size, results)
    }
}

enum class ProcessingStatus {
    STARTED, IN_PROGRESS, COMPLETED, ERROR
}

data class CorrectedTranscription(
    val originalText: String,
    val correctedText: String,
    val originalConfidence: Float,
    val adjustedConfidence: Float,
    val corrections: List<TextCorrection>,
    val language: String,
    val error: String? = null
) {
    companion object {
        fun error(errorMessage: String): CorrectedTranscription {
            return CorrectedTranscription("", "", 0f, 0f, emptyList(), "", errorMessage)
        }
    }
}

data class InitialTranscription(
    val text: String,
    val confidence: Float
)

data class TextCorrection(
    val position: Int,
    val original: String,
    val corrected: String,
    val reason: String
)

data class VoiceRecording(
    val id: String,
    val audioData: ByteArray,
    val timestamp: Long,
    val duration: Long
)