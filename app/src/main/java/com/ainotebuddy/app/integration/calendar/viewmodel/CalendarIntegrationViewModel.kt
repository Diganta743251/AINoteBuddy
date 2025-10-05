package com.ainotebuddy.app.integration.calendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.integration.calendar.model.CalendarEvent
import com.ainotebuddy.app.integration.calendar.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for managing calendar integration.
 */
@HiltViewModel
class CalendarIntegrationViewModel @Inject constructor(
    application: Application,
    private val calendarRepository: CalendarRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /**
     * Loads available calendars and events for a note.
     */
    fun loadCalendars(noteId: Long? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val calendars = calendarRepository.getAvailableCalendars()
                val selectedCalendarId = calendars.firstOrNull()?.first ?: 0L
                val events = noteId?.let { calendarRepository.getEventsForNote(it) } ?: emptyList()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        calendars = calendars,
                        selectedCalendarId = selectedCalendarId,
                        events = events,
                        currentNoteId = noteId
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load calendars"
                    )
                }
            }
        }
    }

    /**
     * Creates a new calendar event linked to a note.
     */
    fun createEvent(
        title: String,
        description: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        allDay: Boolean = false,
        location: String = ""
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val noteId = _uiState.value.currentNoteId ?: throw IllegalStateException("No note selected")
                val calendarId = _uiState.value.selectedCalendarId
                
                // Add note ID to description for reference
                val eventDescription = "$description\n\n[Note ID: $noteId]"
                
                val event = CalendarEvent(
                    title = title,
                    description = eventDescription,
                    startTime = startTime,
                    endTime = endTime,
                    allDay = allDay,
                    location = location,
                    calendarId = calendarId,
                    noteId = noteId
                )
                
                val eventId = calendarRepository.createEvent(event)
                
                if (eventId != null) {
                    // Reload events
                    loadCalendars(noteId)
                } else {
                    throw IllegalStateException("Failed to create event")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create event"
                    )
                }
            }
        }
    }

    /**
     * Updates an existing calendar event.
     */
    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val success = calendarRepository.updateEvent(event)
                
                if (success) {
                    // Reload events
                    _uiState.value.currentNoteId?.let { loadCalendars(it) }
                } else {
                    throw IllegalStateException("Failed to update event")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update event"
                    )
                }
            }
        }
    }

    /**
     * Deletes a calendar event.
     */
    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val success = calendarRepository.deleteEvent(eventId)
                
                if (success) {
                    // Remove from local state
                    _uiState.update { state ->
                        state.copy(
                            events = state.events.filter { it.id != eventId },
                            isLoading = false
                        )
                    }
                } else {
                    throw IllegalStateException("Failed to delete event")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete event"
                    )
                }
            }
        }
    }

    /**
     * Selects a calendar.
     */
    fun selectCalendar(calendarId: Long) {
        _uiState.update { it.copy(selectedCalendarId = calendarId) }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * UI state for calendar integration.
     */
    data class CalendarUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val calendars: List<Pair<Long, String>> = emptyList(),
        val selectedCalendarId: Long = 0,
        val events: List<CalendarEvent> = emptyList(),
        val currentNoteId: Long? = null
    )
}
