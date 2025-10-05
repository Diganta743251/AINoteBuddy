package com.ainotebuddy.app.data.repository.organization

import com.ainotebuddy.app.data.local.dao.organization.RecurrencePatternDao
import com.ainotebuddy.app.data.model.organization.RecurrencePattern
import com.ainotebuddy.app.data.model.organization.RecurrencePatternCreate
import com.ainotebuddy.app.data.model.organization.RecurrencePatternUpdate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringNotesRepository @Inject constructor(
    private val recurrencePatternDao: RecurrencePatternDao
) {
    /**
     * Get a recurrence pattern by ID
     */
    suspend fun getRecurrencePatternById(id: Long): RecurrencePattern? {
        return recurrencePatternDao.getById(id)
    }
    
    /**
     * Get all recurrence patterns for a note
     */
    fun getRecurrencePatternsForNote(noteId: Long): Flow<List<RecurrencePattern>> {
        return recurrencePatternDao.getByNoteId(noteId)
    }
    
    /**
     * Get all active recurrence patterns
     */
    fun getActiveRecurrencePatterns(): Flow<List<RecurrencePattern>> {
        return recurrencePatternDao.getActivePatterns()
    }
    
    /**
     * Get recurrence patterns that should run on a specific date
     */
    suspend fun getRecurrencePatternsForDate(date: LocalDate): List<RecurrencePattern> {
        return recurrencePatternDao.getActivePatternsForDate(
            date
        )
    }
    
    /**
     * Create a new recurrence pattern
     */
    suspend fun createRecurrencePattern(create: RecurrencePatternCreate): Long {
        val pattern = RecurrencePattern(
            noteId = create.noteId,
            startDate = create.startDate,
            endDate = create.endDate,
            repeatType = create.repeatType,
            interval = create.interval,
            daysOfWeek = create.daysOfWeek,
            dayOfMonth = create.dayOfMonth,
            monthDay = create.monthDay,
            timeOfDay = create.timeOfDay,
            timeZone = create.timeZone,
            occurrences = create.occurrences
        )
        
        return recurrencePatternDao.insert(pattern)
    }
    
    /**
     * Update an existing recurrence pattern
     */
    suspend fun updateRecurrencePattern(update: RecurrencePatternUpdate): Boolean {
        val existing = recurrencePatternDao.getById(update.id) ?: return false
        
        val updated = existing.copy(
            endDate = update.endDate ?: existing.endDate,
            interval = update.interval ?: existing.interval,
            daysOfWeek = update.daysOfWeek ?: existing.daysOfWeek,
            dayOfMonth = update.dayOfMonth ?: existing.dayOfMonth,
            monthDay = update.monthDay ?: existing.monthDay,
            timeOfDay = update.timeOfDay ?: existing.timeOfDay,
            timeZone = update.timeZone ?: existing.timeZone,
            isActive = update.isActive ?: existing.isActive,
            updatedAt = java.util.Date()
        )
        
        recurrencePatternDao.update(updated)
        return true
    }
    
    /**
     * Delete a recurrence pattern
     */
    suspend fun deleteRecurrencePattern(id: Long): Boolean {
        val pattern = recurrencePatternDao.getById(id) ?: return false
        recurrencePatternDao.delete(pattern)
        return true
    }
    
    /**
     * Update the last run time for a recurrence pattern
     */
    suspend fun updateLastRun(id: Long, date: LocalDate) {
        val pattern = recurrencePatternDao.getById(id) ?: return
        val updated = pattern.withLastRun(date)
        recurrencePatternDao.update(updated)
    }
    
    /**
     * Set the active status of a recurrence pattern
     */
    suspend fun setActiveStatus(id: Long, active: Boolean): Boolean {
        val pattern = recurrencePatternDao.getById(id) ?: return false
        val updated = pattern.withActiveStatus(active)
        recurrencePatternDao.update(updated)
        return true
    }
    
    /**
     * Get the next occurrence date for a recurrence pattern
     */
    suspend fun getNextOccurrence(id: Long): LocalDate? {
        val pattern = recurrencePatternDao.getById(id) ?: return null
        return pattern.nextOccurrence()
    }
}
