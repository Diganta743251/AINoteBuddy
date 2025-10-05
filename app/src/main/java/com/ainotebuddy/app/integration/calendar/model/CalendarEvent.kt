package com.ainotebuddy.app.integration.calendar.model

import java.time.LocalDateTime

/**
 * Represents a calendar event that can be linked to a note.
 *
 * @property id The unique ID of the calendar event
 * @property title The title of the event
 * @property description The description of the event
 * @property startTime The start time of the event
 * @property endTime The end time of the event
 * @property allDay Whether the event is an all-day event
 * @property location The location of the event
 * @property calendarId The ID of the calendar this event belongs to
 * @property noteId The ID of the linked note, if any
 * @property isSynced Whether the event is synced with the device calendar
 */
data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val allDay: Boolean = false,
    val location: String = "",
    val calendarId: Long = 0,
    val noteId: Long? = null,
    val isSynced: Boolean = false
) 
