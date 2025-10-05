package com.ainotebuddy.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.model.Task
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.repository.TaskRepository
import com.ainotebuddy.app.repository.TaskRepositoryImpl
import com.ainotebuddy.app.ai.SimpleAIService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class NoteEditorViewModel(private val context: Context) : ViewModel() {
    private val noteRepository = NoteRepository(context)
    private val taskRepository = TaskRepositoryImpl(context)
    private val aiService = SimpleAIService(context)

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val _currentNote = MutableStateFlow<NoteEntity?>(null)
    val currentNote: StateFlow<NoteEntity?> = _currentNote.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _autoSaveEnabled = MutableStateFlow(true)
    val autoSaveEnabled: StateFlow<Boolean> = _autoSaveEnabled.asStateFlow()

    private var editStartTime: Long = 0
    private var lastSaveTime: Long = 0
    private var changesSinceLastSave = 0

    // AI Tools state
    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()
    private val _aiTags = MutableStateFlow<List<String>>(emptyList())
    val aiTags: StateFlow<List<String>> = _aiTags.asStateFlow()
    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _summarizeLoading = MutableStateFlow(false)
    val summarizeLoading: StateFlow<Boolean> = _summarizeLoading.asStateFlow()
    private val _autoTagLoading = MutableStateFlow(false)
    val autoTagLoading: StateFlow<Boolean> = _autoTagLoading.asStateFlow()

    private enum class AiAction { SUMMARIZE, AUTOTAG }
    private var lastAiAction: AiAction? = null

    // Smart Reminder candidate state
    private val _reminderCandidate = MutableStateFlow<com.ainotebuddy.app.ai.ReminderNlu.ReminderCandidate?>(null)
    val reminderCandidate: StateFlow<com.ainotebuddy.app.ai.ReminderNlu.ReminderCandidate?> = _reminderCandidate.asStateFlow()

    fun clearAiError() { _aiError.value = null }

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val note = noteRepository.getNoteById(noteId)
                
                _currentNote.value = note
                _uiState.value = _uiState.value.copy(
                    title = note?.title ?: "",
                    content = note?.content ?: "",
                    tags = note?.tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                    category = note?.category,
                    isFavorite = note?.isFavorite ?: false
                )
                
                if (note != null) {
                    loadNoteTasks(noteId)
                }
                
                editStartTime = System.currentTimeMillis()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to load note: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, hasUnsavedChanges = true)
        changesSinceLastSave++
        
        if (_autoSaveEnabled.value) {
            scheduleAutoSave()
        }
    }

    fun updateContent(content: String) {
        val oldContent = _uiState.value.content
        _uiState.value = _uiState.value.copy(content = content, hasUnsavedChanges = true)
        changesSinceLastSave++
        
        // Track content changes for analytics
        if (content.length != oldContent.length) {
            trackContentChange(oldContent.length, content.length)
        }
        
        if (_autoSaveEnabled.value) {
            scheduleAutoSave()
        }
        
        // Trigger AI analysis for significant changes
        if (kotlin.math.abs(content.length - oldContent.length) > 50) {
            performAIAnalysis(content)
        }

        // Smart Reminder: run lightweight NLU (debounced by content change threshold above)
        analyzeForReminder(content)
    }

    fun addTag(tag: String) {
        val currentTags = _uiState.value.tags.toMutableList()
        if (tag.isNotBlank() && !currentTags.contains(tag)) {
            currentTags.add(tag)
            _uiState.value = _uiState.value.copy(
                tags = currentTags,
                hasUnsavedChanges = true
            )
            changesSinceLastSave++
        }
    }

    fun removeTag(tag: String) {
        val currentTags = _uiState.value.tags.toMutableList()
        if (currentTags.remove(tag)) {
            _uiState.value = _uiState.value.copy(
                tags = currentTags,
                hasUnsavedChanges = true
            )
            changesSinceLastSave++
        }
    }

    // AI Tools API for UI
    fun requestSummary(noteId: Long) {
        viewModelScope.launch {
            try {
                _summarizeLoading.value = true
                _aiError.value = null
                lastAiAction = AiAction.SUMMARIZE
                
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    delay(500) // Simulate AI processing time
                    val summary = aiService.summarizeNote(note)
                    _aiSummary.value = summary
                } else {
                    _aiError.value = "Note not found"
                }
            } catch (t: Throwable) {
                _aiError.value = "AI unavailable: ${t.message}"
            } finally {
                _summarizeLoading.value = false
            }
        }
    }

    fun requestAutoTags(noteId: Long) {
        viewModelScope.launch {
            try {
                _autoTagLoading.value = true
                _aiError.value = null
                lastAiAction = AiAction.AUTOTAG
                
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    delay(300) // Simulate AI processing time
                    val tags = aiService.generateTags(note)
                    _aiTags.value = tags
                } else {
                    _aiError.value = "Note not found"
                }
            } catch (t: Throwable) {
                _aiError.value = "AI unavailable: ${t.message}"
            } finally {
                _autoTagLoading.value = false
            }
        }
    }

    fun applySummaryToContent() {
        val s = _aiSummary.value ?: return
        updateContent(_uiState.value.content + "\n\n" + s)
    }

    fun applyTagsToNote() {
        val tags = _aiTags.value
        if (tags.isEmpty()) return
        tags.forEach { addTag(it) }
    }

    fun retryAiAction() {
        val id = _currentNote.value?.id ?: return
        when (lastAiAction) {
            AiAction.SUMMARIZE -> requestSummary(id)
            AiAction.AUTOTAG -> requestAutoTags(id)
            else -> {}
        }
    }

    // --- Smart Reminder API ---
    fun analyzeForReminder(text: String) {
        // Stub implementation - no reminder analysis for now
        _reminderCandidate.value = null
    }

    fun confirmReminder() {
        // Stub implementation - no reminder confirmation for now
        _reminderCandidate.value = null
        _uiState.value = _uiState.value.copy(snackbarMessage = "Reminder feature not available")
    }

    fun dismissReminderSuggestion() {
        _reminderCandidate.value = null
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(
            category = category,
            hasUnsavedChanges = true
        )
        changesSinceLastSave++
    }
    
    fun suggestCategory() {
        viewModelScope.launch {
            try {
                _currentNote.value?.let { note ->
                    val suggestedCategory = aiService.suggestCategory(note)
                    _uiState.value = _uiState.value.copy(
                        category = suggestedCategory,
                        hasUnsavedChanges = true
                    )
                }
            } catch (t: Throwable) {
                // Handle error silently
            }
        }
    }

    fun toggleFavorite() {
        _uiState.value = _uiState.value.copy(
            isFavorite = !_uiState.value.isFavorite,
            hasUnsavedChanges = true
        )
        changesSinceLastSave++
    }

    fun saveNote() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val state = _uiState.value
                val currentNote = _currentNote.value

                val noteToSave = if (currentNote != null) {
                    // Update existing note
                    currentNote.copy(
                        title = state.title,
                        content = state.content,
                        tags = state.tags.joinToString(","),
                        category = state.category ?: "General",
                        isFavorite = state.isFavorite,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    // Create new note
                    val newNoteId = System.currentTimeMillis() // Simple ID generation
                    NoteEntity(
                        id = newNoteId,
                        title = state.title,
                        content = state.content,
                        tags = state.tags.joinToString(","),
                        category = state.category ?: "General",
                        isFavorite = state.isFavorite,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }

                // Save to repository
                if (currentNote != null) {
                    noteRepository.updateNote(noteToSave)
                } else {
                    val noteId = noteRepository.insertNote(noteToSave)
                    _currentNote.value = noteToSave.copy(id = noteId)
                }

                _uiState.value = _uiState.value.copy(
                    hasUnsavedChanges = false,
                    lastSaved = System.currentTimeMillis()
                )
                
                lastSaveTime = System.currentTimeMillis()
                changesSinceLastSave = 0

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to save note: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNote() {
        // Stub implementation - just clear the current note
        _currentNote.value = null
        _uiState.value = NoteEditorUiState()
    }

    fun createNewNote() {
        _currentNote.value = null
        _uiState.value = NoteEditorUiState()
        editStartTime = System.currentTimeMillis()
        changesSinceLastSave = 0
    }

    fun insertTextAtCursor(text: String, cursorPosition: Int) {
        val currentContent = _uiState.value.content
        val newContent = currentContent.substring(0, cursorPosition) + 
                        text + 
                        currentContent.substring(cursorPosition)
        updateContent(newContent)
    }

    fun formatText(formatType: TextFormatType, selection: TextSelection) {
        val content = _uiState.value.content
        val newContent = when (formatType) {
            TextFormatType.BOLD -> applyBoldFormat(content, selection)
            TextFormatType.ITALIC -> applyItalicFormat(content, selection)
            TextFormatType.HEADER -> applyHeaderFormat(content, selection)
            TextFormatType.BULLET_LIST -> applyBulletListFormat(content, selection)
            TextFormatType.NUMBERED_LIST -> applyNumberedListFormat(content, selection)
            TextFormatType.CHECKLIST -> applyChecklistFormat(content, selection)
        }
        updateContent(newContent)
    }

    fun insertChecklistItem(text: String, position: Int) {
        val currentContent = _uiState.value.content
        val checklistItem = "- [ ] $text\n"
        val newContent = currentContent.substring(0, position) + 
                         checklistItem + 
                         currentContent.substring(position)
        updateContent(newContent)
    }

    fun toggleChecklistItem(lineNumber: Int) {
        val lines = _uiState.value.content.split("\n").toMutableList()
        if (lineNumber < lines.size) {
            val line = lines[lineNumber]
            when {
                line.contains("- [ ]") -> {
                    lines[lineNumber] = line.replace("- [ ]", "- [x]")
                }
                line.contains("- [x]") -> {
                    lines[lineNumber] = line.replace("- [x]", "- [ ]")
                }
            }
            updateContent(lines.joinToString("\n"))
        }
    }

    fun addTaskFromText(text: String) {
        viewModelScope.launch {
            _currentNote.value?.let { note ->
                val task = Task(
                    id = 0,
                    noteId = note.id,
                    title = text,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    position = 0
                )
                taskRepository.addTask(task)
                loadNoteTasks(note.id)
            }
        }
    }

    // Remove duplicate helper below if present

    fun performAIAnalysis(content: String = _uiState.value.content) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAnalyzing = true)
                
                val tempNote = NoteEntity(
                    title = _uiState.value.title,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                val analysis = try { com.ainotebuddy.app.ai.AIAnalysisEngine().analyzeNote(tempNote) } catch (_: Throwable) { com.ainotebuddy.app.ai.AIAnalysisResult(
                    sentiment = com.ainotebuddy.app.ai.SentimentResult(com.ainotebuddy.app.ai.Sentiment.NEUTRAL, 0.5f, 0.3f, 0.2f, 0.5f),
                    topics = emptyList(),
                    entities = emptyList(),
                    actionItems = emptyList(),
                    keyPhrases = emptyList(),
                    insights = emptyList(),
                    contextualTags = emptyList(),
                    confidence = 0.0f
                ) }
                val suggestions = emptyList<com.ainotebuddy.app.ai.AISuggestion>()
                
                _uiState.value = _uiState.value.copy(
                    aiAnalysis = analysis,
                    aiSuggestions = suggestions,
                    isAnalyzing = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = "AI analysis failed: ${e.message}"
                )
            }
        }
    }

    fun applySuggestion(suggestion: com.ainotebuddy.app.ai.AISuggestion) {
        when (suggestion.type) {
            com.ainotebuddy.app.ai.SuggestionType.TAGGING -> {
                val suggestedTags = suggestion.metadata["suggested_tags"] as? List<String>
                suggestedTags?.forEach { tag -> addTag(tag) }
            }
            com.ainotebuddy.app.ai.SuggestionType.ORGANIZATION -> {
                val suggestedCategory = suggestion.metadata["suggested_category"] as? String
                suggestedCategory?.let { updateCategory(it) }
            }
            com.ainotebuddy.app.ai.SuggestionType.PRODUCTIVITY -> {
                val actionItems = suggestion.metadata["action_items"] as? List<String>
                actionItems?.forEach { item ->
                    addTaskFromText(item)
                }
            }
            com.ainotebuddy.app.ai.SuggestionType.FORMATTING -> {
                // Example: apply simple formatting suggestion
                val format = suggestion.metadata["format"] as? String
                if (format == "bold") {
                    val content = _uiState.value.content
                    updateContent("**$content**")
                }
            }
            else -> {
                // Handle other suggestion types
            }
        }
    }

    fun enableAutoSave(enabled: Boolean) {
        _autoSaveEnabled.value = enabled
        if (enabled && _uiState.value.hasUnsavedChanges) {
            scheduleAutoSave()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun scheduleAutoSave() {
        viewModelScope.launch {
            delay(2000) // Auto-save after 2 seconds of inactivity
            if (_uiState.value.hasUnsavedChanges && _autoSaveEnabled.value) {
                saveNote()
            }
        }
    }

    private fun loadNoteTasks(noteId: Long) {
        viewModelScope.launch {
            taskRepository.getTasksForNote(noteId).collect { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks)
            }
        }
    }

    private fun trackContentChange(oldLength: Int, newLength: Int) {
        // Track significant content changes for analytics
        if (kotlin.math.abs(newLength - oldLength) > 20) {
            // Significant change detected
        }
    }

    // Text formatting helper functions
    private fun applyBoldFormat(content: String, selection: TextSelection): String {
        val selectedText = content.substring(selection.start, selection.end)
        val formattedText = "**$selectedText**"
        return content.replaceRange(selection.start, selection.end, formattedText)
    }

    private fun applyItalicFormat(content: String, selection: TextSelection): String {
        val selectedText = content.substring(selection.start, selection.end)
        val formattedText = "_${selectedText}_"
        return content.replaceRange(selection.start, selection.end, formattedText)
    }

    private fun applyHeaderFormat(content: String, selection: TextSelection): String {
        val selectedText = content.substring(selection.start, selection.end)
        val formattedText = "# $selectedText"
        return content.replaceRange(selection.start, selection.end, formattedText)
    }

    private fun applyBulletListFormat(content: String, selection: TextSelection): String {
        val selectedText = content.substring(selection.start, selection.end)
        val lines = selectedText.split("\n")
        val formattedLines = lines.map { line ->
            if (line.isNotBlank() && !line.startsWith("- ")) {
                "- $line"
            } else line
        }
        return content.replaceRange(selection.start, selection.end, formattedLines.joinToString("\n"))
    }

    private fun applyNumberedListFormat(content: String, selection: TextSelection): String {
        val selectedText = content.substring(selection.start, selection.end)
        val lines = selectedText.split("\n").filter { it.isNotBlank() }
        return finalizeNumberedListFormat(content, selection, lines)
    }

    // Duplicate removed: addTaskFromText(text: String) is defined earlier with repository.addTask

    private fun finalizeNumberedListFormat(content: String, selection: TextSelection, lines: List<String>): String {
        val formattedLines = lines.mapIndexed { index, line ->
            "${index + 1}. $line"
        }
        return content.replaceRange(selection.start, selection.end, formattedLines.joinToString("\n"))
    }

    private fun applyChecklistFormat(content: String, selection: TextSelection): String {
        val selectedText = content.substring(selection.start, selection.end)
        val lines = selectedText.split("\n")
        val formattedLines = lines.map { line ->
            if (line.isNotBlank() && !line.contains("- [ ]") && !line.contains("- [x]")) {
                "- [ ] $line"
            } else line
        }
        return content.replaceRange(selection.start, selection.end, formattedLines.joinToString("\n"))
    }
}

// Data classes and enums
data class NoteEditorUiState(
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val isFavorite: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val lastSaved: Long? = null,
    val error: String? = null,
    val isAnalyzing: Boolean = false,
    val aiAnalysis: com.ainotebuddy.app.ai.AIAnalysisResult? = null,
    val aiSuggestions: List<com.ainotebuddy.app.ai.AISuggestion> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val snackbarMessage: String? = null
)

data class TextSelection(
    val start: Int,
    val end: Int
)

enum class TextFormatType {
    BOLD, ITALIC, HEADER, BULLET_LIST, NUMBERED_LIST, CHECKLIST
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}