package com.ainotebuddy.app.integration.calendar.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.ainotebuddy.app.integration.calendar.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Repository for handling calendar-related operations.
 */
class CalendarRepository @Inject constructor(
    private val context: Context
) {
    private val calendarProjection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.ACCOUNT_TYPE
    )

    private val eventProjection = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.ALL_DAY,
        CalendarContract.Events.EVENT_LOCATION,
        CalendarContract.Events.CALENDAR_ID
    )

    /**
     * Gets all available calendars on the device.
     */
    suspend fun getAvailableCalendars(): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<Pair<Long, String>>()
        val uri = CalendarContract.Calendars.CONTENT_URI
        
        context.contentResolver.query(
            uri,
            calendarProjection,
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameColumn = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                calendars.add(id to name)
            }
        }
        
        return@withContext calendars
    }

    /**
     * Creates a new calendar event.
     */
    suspend fun createEvent(event: CalendarEvent): Long? = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.DTEND, event.endTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1) // Enable reminders
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return@withContext uri?.lastPathSegment?.toLongOrNull()
    }

    /**
     * Updates an existing calendar event.
     */
    suspend fun updateEvent(event: CalendarEvent): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.DTEND, event.endTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
        }

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
        val rowsUpdated = context.contentResolver.update(uri, values, null, null)
        return@withContext rowsUpdated > 0
    }

    /**
     * Deletes a calendar event.
     */
    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rowsDeleted = context.contentResolver.delete(uri, null, null)
        return@withContext rowsDeleted > 0
    }

    /**
     * Gets events for a specific note.
     */
    suspend fun getEventsForNote(noteId: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEvent>()
        val selection = "${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val selectionArgs = arrayOf("%noteId:$noteId%")

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            eventProjection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(CalendarContract.Events._ID)
            val titleColumn = cursor.getColumnIndex(CalendarContract.Events.TITLE)
            val descColumn = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val startColumn = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
            val endColumn = cursor.getColumnIndex(CalendarContract.Events.DTEND)
            val allDayColumn = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
            val locationColumn = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
            val calendarIdColumn = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID)

            while (cursor.moveToNext()) {
                val startTime = cursor.getLong(startColumn)
                val endTime = cursor.getLong(endColumn)
                
                events.add(
                    CalendarEvent(
                        id = cursor.getLong(idColumn),
                        title = cursor.getString(titleColumn),
                        description = cursor.getString(descColumn),
                        startTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(startTime),
                            ZoneId.systemDefault()
                        ),
                        endTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(endTime),
                            ZoneId.systemDefault()
                        ),
                        allDay = cursor.getInt(allDayColumn) == 1,
                        location = cursor.getString(locationColumn),
                        calendarId = cursor.getLong(calendarIdColumn),
                        noteId = noteId,
                        isSynced = true
                    )
                )
            }
        }
        
        return@withContext events
    }
}
