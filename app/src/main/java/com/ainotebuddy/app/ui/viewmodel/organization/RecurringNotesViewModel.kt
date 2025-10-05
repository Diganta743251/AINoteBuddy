package com.ainotebuddy.app.ui.viewmodel.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.defaultNoteTemplates
import com.ainotebuddy.app.data.model.organization.RecurrencePattern
import com.ainotebuddy.app.data.model.organization.RecurringNote
import com.ainotebuddy.app.data.repository.organization.RecurringNotesRepository
import com.ainotebuddy.app.data.repository.organization.OrganizationNoteRepository
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.service.RecurringNotesScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for managing Recurring Notes
 */
@HiltViewModel
class RecurringNotesViewModel @Inject constructor(
    private val repository: RecurringNotesRepository,
    private val recurringNotesScheduler: RecurringNotesScheduler,
    private val organizationNoteRepository: OrganizationNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecurringNotesUiState>(RecurringNotesUiState.Loading)
    val uiState: StateFlow<RecurringNotesUiState> = _uiState.asStateFlow()

    private val _selectedPatterns = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPatterns: StateFlow<Set<Long>> = _selectedPatterns.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _availableTemplates = MutableStateFlow<List<NoteTemplate>>(emptyList())
    val availableTemplates: StateFlow<List<NoteTemplate>> = _availableTemplates.asStateFlow()
    
    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()
    
    private val _editingPattern = MutableStateFlow<RecurrencePattern?>(null)
    val editingPattern: StateFlow<RecurrencePattern?> = _editingPattern.asStateFlow()
    
    private val _showPatternEditor = MutableStateFlow(false)
    val showPatternEditor: StateFlow<Boolean> = _showPatternEditor.asStateFlow()
    
    private val _selectedTemplate = MutableStateFlow<NoteTemplate?>(null)
    val selectedTemplate: StateFlow<NoteTemplate?> = _selectedTemplate.asStateFlow()

    init {
        loadRecurrencePatterns()
        loadTemplates()
        loadNotes()
    }

    /**
     * Load all recurrence patterns
     */
    private fun loadRecurrencePatterns() {
        viewModelScope.launch {
            try {
                repository.getActiveRecurrencePatterns()
                    .map { patterns ->
                        patterns.map { pattern ->
                            // UI model using basic fields; note title lookup omitted to avoid cross-repo dependency
                            RecurrencePatternUiModel(
                                id = pattern.id,
                                noteId = pattern.noteId,
                                noteTitle = "Recurring Note",
                                startDate = pattern.startDate,
                                endDate = pattern.endDate,
                                repeatType = pattern.repeatType,
                                interval = pattern.interval,
                                daysOfWeek = pattern.daysOfWeek,
                                dayOfMonth = pattern.dayOfMonth,
                                monthDay = pattern.monthDay,
                                timeOfDay = pattern.timeOfDay,
                                lastRun = pattern.lastRun,
                                isActive = pattern.isActive
                            )
                        }
                    }
                    .collect { uiModels ->
                        _uiState.value = RecurringNotesUiState.Success(uiModels)
                    }
            } catch (e: Exception) {
                _uiState.value = RecurringNotesUiState.Error(e.message ?: "Failed to load recurrence patterns")
            }
        }
    }

    private fun mapRule(rule: RecurringNote.RecurrenceRule): Pair<RecurrencePattern.RepeatType, Int> = when (rule) {
        RecurringNote.RecurrenceRule.DAILY -> RecurrencePattern.RepeatType.DAILY to 1
        RecurringNote.RecurrenceRule.WEEKLY -> RecurrencePattern.RepeatType.WEEKLY to 1
        RecurringNote.RecurrenceRule.BIWEEKLY -> RecurrencePattern.RepeatType.WEEKLY to 2
        RecurringNote.RecurrenceRule.MONTHLY -> RecurrencePattern.RepeatType.MONTHLY to 1
        RecurringNote.RecurrenceRule.QUARTERLY -> RecurrencePattern.RepeatType.MONTHLY to 3
        RecurringNote.RecurrenceRule.YEARLY -> RecurrencePattern.RepeatType.YEARLY to 1
    }

    fun createRecurringForExistingNote(
        noteId: Long,
        rule: RecurringNote.RecurrenceRule,
        startMillis: Long,
        endMillis: Long?
    ) {
        val zone = java.time.ZoneId.systemDefault()
        val startZdt = java.time.Instant.ofEpochMilli(startMillis).atZone(zone)
        val endZdt = endMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(zone) }
        val (repeatType, interval) = mapRule(rule)
        createRecurrencePattern(
            noteId = noteId,
            repeatType = repeatType,
            interval = interval,
            startDate = startZdt.toLocalDate(),
            endDate = endZdt?.toLocalDate(),
            timeOfDay = startZdt.toLocalTime().withSecond(0).withNano(0)
        )
    }

    /**
     * Create the same recurring pattern for all existing notes
     */
    fun createRecurringForAllNotes(
        rule: RecurringNote.RecurrenceRule,
        startMillis: Long,
        endMillis: Long?
    ) {
        viewModelScope.launch {
            val notes = allNotes.value
            notes.forEach { note ->
                createRecurringForExistingNote(note.id, rule, startMillis, endMillis)
            }
            _events.emit(UiEvent.ShowMessage("Patterns created for all notes"))
        }
    }

    fun createRecurringFromTemplate(
        templateId: String,
        title: String,
        variables: Map<String, String>,
        rule: RecurringNote.RecurrenceRule,
        startMillis: Long,
        endMillis: Long?
    ) {
        viewModelScope.launch {
            try {
                val template = _availableTemplates.value.firstOrNull { it.id == templateId }
                    ?: throw IllegalArgumentException("Template not found")
                val newNoteId = organizationNoteRepository.createNoteFromTemplate(template, variables, title)
                createRecurringForExistingNote(newNoteId, rule, startMillis, endMillis)
                _events.emit(UiEvent.ShowMessage("Pattern created", noteId = newNoteId))
                _showCreateDialog.value = false
            } catch (e: Exception) {
                _uiState.value = RecurringNotesUiState.Error(
                    e.message ?: "Failed to create recurring note from template"
                )
            }
        }
    }

    /**
     * Load available templates for creating recurring notes
     */
    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                // Use default templates for now; replace with DAO-backed templates when available
                _availableTemplates.value = defaultNoteTemplates
            } catch (e: Exception) {
                _uiState.value = RecurringNotesUiState.Error("Failed to load templates: ${e.message}")
            }
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            try {
                organizationNoteRepository.getAllNotes().collect { notes ->
                    _allNotes.value = notes
                }
            } catch (e: Exception) {
                // do not surface as fatal UI error
            }
        }
    }
    
    /**
     * Set the currently selected template
     */
    fun selectTemplate(template: NoteTemplate?) {
        _selectedTemplate.value = template
    }

    /**
     * Toggle pattern selection
     */
    fun togglePatternSelection(patternId: Long) {
        _selectedPatterns.update { current ->
            if (current.contains(patternId)) {
                current - patternId
            } else {
                current + patternId
            }
        }
    }

    /**
     * Create a new recurrence pattern
     */
    fun createRecurrencePattern(
        noteId: Long,
        repeatType: RecurrencePattern.RepeatType,
        interval: Int,
        daysOfWeek: Set<DayOfWeek> = emptySet(),
        dayOfMonth: Int? = null,
        monthDay: Int? = null,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate? = null,
        timeOfDay: LocalTime = LocalTime.NOON
    ) {
        viewModelScope.launch {
            try {
                val create = com.ainotebuddy.app.data.model.organization.RecurrencePatternCreate(
                    noteId = noteId,
                    startDate = startDate,
                    endDate = endDate,
                    repeatType = repeatType,
                    interval = interval,
                    daysOfWeek = daysOfWeek,
                    dayOfMonth = dayOfMonth,
                    monthDay = monthDay,
                    timeOfDay = timeOfDay
                )
                
                val newId = repository.createRecurrencePattern(create)
                _showPatternEditor.value = false
                loadRecurrencePatterns()
                _events.emit(UiEvent.ShowMessage("Pattern created", noteId = newId))
            } catch (e: Exception) {
                _uiState.value = RecurringNotesUiState.Error(
                    e.message ?: "Failed to create recurrence pattern"
                )
            }
        }
    }
    
    /**
     * Update an existing recurrence pattern
     */
    fun updateRecurrencePattern(
        id: Long,
        isActive: Boolean? = null,
        endDate: LocalDate? = null,
        interval: Int? = null,
        daysOfWeek: Set<DayOfWeek>? = null,
        dayOfMonth: Int? = null,
        monthDay: Int? = null,
        timeOfDay: LocalTime? = null
    ) {
        viewModelScope.launch {
            try {
                val update = com.ainotebuddy.app.data.model.organization.RecurrencePatternUpdate(
                    id = id,
                    endDate = endDate,
                    interval = interval,
                    daysOfWeek = daysOfWeek,
                    dayOfMonth = dayOfMonth,
                    monthDay = monthDay,
                    timeOfDay = timeOfDay,
                    isActive = isActive
                )
                
                val ok = repository.updateRecurrencePattern(update)
                loadRecurrencePatterns()
                if (ok) {
                    _events.emit(UiEvent.ShowMessage("Pattern updated", noteId = id))
                }
            } catch (e: Exception) {
                _uiState.value = RecurringNotesUiState.Error(
                    e.message ?: "Failed to update recurrence pattern"
                )
            }
        }
    }

    /**
     * Toggle pattern active state
     */
    fun togglePatternActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            // Optimistic UI update
            val previous = _uiState.value
            if (previous is RecurringNotesUiState.Success) {
                val updated = previous.patterns.map { p ->
                    if (p.id == id) p.copy(isActive = isActive) else p
                }
                _uiState.value = RecurringNotesUiState.Success(updated)
            }
            try {
                repository.setActiveStatus(id, isActive)
                _events.emit(UiEvent.ShowUndoToggle(
                    message = "Recurrence updated",
                    patternId = id,
                    previousState = !isActive
                ))
                // Also emit a message with id for highlight
                _events.emit(UiEvent.ShowMessage("Recurrence updated", noteId = id))
            } catch (e: Exception) {
                // Rollback on error
                if (previous is RecurringNotesUiState.Success) {
                    val rolledBack = previous.patterns.map { p ->
                        if (p.id == id) p.copy(isActive = !isActive) else p
                    }
                    _uiState.value = RecurringNotesUiState.Success(rolledBack)
                }
                _events.emit(UiEvent.ShowMessage("Failed to update recurrence", noteId = id))
            }
        }
    }

    /**
     * Delete selected patterns
     */
    fun deleteSelectedPatterns() {
        viewModelScope.launch {
            try {
                val ids = _selectedPatterns.value
                // Fetch full objects for undo payloads
                val deletedPatterns = ids.mapNotNull { repository.getRecurrencePatternById(it) }
                ids.forEach { id -> repository.deleteRecurrencePattern(id) }
                _selectedPatterns.value = emptySet()
                loadRecurrencePatterns()
                // Emit undo event per deleted pattern (keeps logic simple and consistent)
                deletedPatterns.forEach { deleted ->
                    _events.emit(UiEvent.ShowUndoDelete(
                        message = "Pattern deleted",
                        pattern = deleted
                    ))
                }
            } catch (e: Exception) {
                _uiState.value = RecurringNotesUiState.Error(
                    e.message ?: "Failed to delete patterns"
                )
            }
        }
    }

    fun undoDeletePattern(pattern: RecurrencePattern) {
        viewModelScope.launch {
            try {
                val create = com.ainotebuddy.app.data.model.organization.RecurrencePatternCreate(
                    noteId = pattern.noteId,
                    startDate = pattern.startDate,
                    endDate = pattern.endDate,
                    repeatType = pattern.repeatType,
                    interval = pattern.interval,
                    daysOfWeek = pattern.daysOfWeek,
                    dayOfMonth = pattern.dayOfMonth,
                    monthDay = pattern.monthDay,
                    timeOfDay = pattern.timeOfDay,
                    timeZone = pattern.timeZone,
                    occurrences = pattern.occurrences
                )
                val newId = repository.createRecurrencePattern(create)
                loadRecurrencePatterns()
                _events.emit(UiEvent.ShowMessage("Pattern restored", noteId = newId))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("Failed to restore pattern"))
            }
        }
    }

    /**
     * Refresh recurrence patterns (used by UI retry)
     */
    fun refresh() {
        loadRecurrencePatterns()
    }

    /**
     * Clear current selection (used by UI when exiting selection mode)
     */
    fun clearSelection() {
        _selectedPatterns.value = emptySet()
    }

    /**
     * Select all current patterns in UI
     */
    fun selectAll() {
        val ids = (uiState.value as? RecurringNotesUiState.Success)
            ?.patterns
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()
        _selectedPatterns.value = ids
    }

    /**
     * Show/hide create dialog
     */
    fun setShowCreateDialog(show: Boolean) {
        _showCreateDialog.value = show
    }
    
    /**
     * Show/hide pattern editor
     */
    fun setShowPatternEditor(show: Boolean, pattern: RecurrencePattern? = null) {
        _editingPattern.value = pattern
        _showPatternEditor.value = show
    }

    /**
     * Process due recurring notes
     */
    fun processDueNotes() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val patterns = repository.getRecurrencePatternsForDate(today)
                
                patterns.forEach { pattern ->
                    if (pattern.shouldRunOn(today)) {
                        // Create the note based on the template
                        createNoteFromPattern(pattern)
                        
                        // Update last run time
                        repository.updateLastRun(pattern.id, today)
                        // Optional: feedback that a note was generated could be added later
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RecurringNotesUiState.Error(
                    e.message ?: "Failed to process due notes"
                )
            }
        }
    }
    
    /**
     * Create a new note from a recurrence pattern
     */
    private suspend fun createNoteFromPattern(pattern: RecurrencePattern) {
        try {
            // TODO: Implement actual note creation from template when template system is wired
            // For now, no-op to avoid unresolved references
            return
            
        } catch (e: Exception) {
            _uiState.value = RecurringNotesUiState.Error(
                "Failed to create note from pattern: ${e.message}"
            )
        }
    }
    
    /**
     * Get the next occurrence date for a pattern
     */
    suspend fun getNextOccurrence(patternId: Long): LocalDate? {
        return repository.getNextOccurrence(patternId)
    }

    /**
     * UI State for Recurring Notes screen
     */
    sealed class RecurringNotesUiState {
        object Loading : RecurringNotesUiState()
        data class Success(val patterns: List<RecurrencePatternUiModel>) : RecurringNotesUiState()
        data class Error(val message: String) : RecurringNotesUiState()
    }

    // One-shot UI events
    sealed class UiEvent {
        data class ShowMessage(val message: String, val noteId: Long? = null) : UiEvent()
        data class ShowUndoToggle(val message: String, val patternId: Long, val previousState: Boolean) : UiEvent()
        data class ShowUndoDelete(val message: String, val pattern: RecurrencePattern) : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    /**
     * UI Model for Recurrence Pattern
     */
    data class RecurrencePatternUiModel(
        val id: Long,
        val noteId: Long,
        val noteTitle: String,
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val repeatType: RecurrencePattern.RepeatType,
        val interval: Int,
        val daysOfWeek: Set<DayOfWeek>,
        val dayOfMonth: Int?,
        val monthDay: Int?,
        val timeOfDay: LocalTime,
        val lastRun: LocalDate?,
        val isActive: Boolean,
        val isSelected: Boolean = false,
        val nextOccurrence: LocalDate? = null
    ) {
        // Build user-friendly recurrence label
        fun formatRecurrence(): String {
            val base = when (repeatType) {
                RecurrencePattern.RepeatType.DAILY -> "day"
                RecurrencePattern.RepeatType.WEEKLY -> "week"
                RecurrencePattern.RepeatType.MONTHLY -> "month"
                RecurrencePattern.RepeatType.YEARLY -> "year"
                RecurrencePattern.RepeatType.CUSTOM -> "custom"
            }
            return if (interval > 1) "Every $interval ${base}s" else "Every $base"
        }
    }
}

/**
 * Extension function to convert domain model to UI model
 */
private fun RecurrencePattern.toUiModel(
    noteTitle: String,
    isSelected: Boolean = false
): RecurringNotesViewModel.RecurrencePatternUiModel {
    return RecurringNotesViewModel.RecurrencePatternUiModel(
        id = id,
        noteId = noteId,
        noteTitle = noteTitle,
        startDate = startDate,
        endDate = endDate,
        repeatType = repeatType,
        interval = interval,
        daysOfWeek = daysOfWeek,
        dayOfMonth = dayOfMonth,
        monthDay = monthDay,
        timeOfDay = timeOfDay,
        lastRun = lastRun,
        isActive = isActive,
        isSelected = isSelected,
        nextOccurrence = nextOccurrence()
    )
}

// Next occurrence label formatting
private fun RecurringNotesViewModel.RecurrencePatternUiModel.formatNextOccurrence(): String {
    val formatterDate = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
    val formatterTime = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
    return nextOccurrence?.let { date ->
        val dateStr = date.format(formatterDate)
        timeOfDay.let { time ->
            val timeStr = time.format(formatterTime)
            "$dateStr at $timeStr"
        }
    } ?: "Not scheduled"
}
