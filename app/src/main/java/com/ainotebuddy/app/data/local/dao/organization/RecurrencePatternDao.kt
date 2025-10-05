package com.ainotebuddy.app.data.local.dao.organization

import androidx.room.*
import com.ainotebuddy.app.data.model.organization.RecurrencePattern
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for RecurrencePattern entity
 */
@Dao
interface RecurrencePatternDao {
    /**
     * Get a recurrence pattern by ID
     */
    @Query("SELECT * FROM recurrence_patterns WHERE id = :id")
    suspend fun getById(id: Long): RecurrencePattern?
    
    /**
     * Get all recurrence patterns for a specific note
     */
    @Query("SELECT * FROM recurrence_patterns WHERE noteId = :noteId")
    fun getByNoteId(noteId: Long): Flow<List<RecurrencePattern>>
    
    /**
     * Get all active recurrence patterns
     */
    @Query("SELECT * FROM recurrence_patterns WHERE isActive = 1")
    fun getActivePatterns(): Flow<List<RecurrencePattern>>
    
    /**
     * Get recurrence patterns that should run on a specific date
     */
    @Query("""
        SELECT * FROM recurrence_patterns 
        WHERE isActive = 1 
        AND (endDate IS NULL OR endDate >= :date)
        AND (lastRun IS NULL OR lastRun < :date)
        AND (
            -- Daily pattern
            (repeatType = 'DAILY' AND 
             (julianday(:date) - julianday(startDate)) % interval = 0) OR
            
            -- Weekly pattern
            (repeatType = 'WEEKLY' AND 
             (julianday(:date) - julianday(startDate)) % (interval * 7) = 0) OR
            
            -- Monthly pattern (day of month)
            (repeatType = 'MONTHLY' AND 
             strftime('%d', :date) = dayOfMonth AND
             (strftime('%Y', :date) * 12 + strftime('%m', :date) - 
              strftime('%Y', startDate) * 12 - strftime('%m', startDate)) % interval = 0) OR
            
            -- Monthly pattern (weekday of month, e.g., 2nd Tuesday)
            (repeatType = 'MONTHLY' AND 
             monthDay IS NOT NULL AND
             strftime('%Y-%m', :date) = strftime('%Y-%m', :date) AND
             (strftime('%Y', :date) * 12 + strftime('%m', :date) - 
              strftime('%Y', startDate) * 12 - strftime('%m', startDate)) % interval = 0 AND
             strftime('%w', :date) = strftime('%w', startDate) AND
             (strftime('%d', :date) - 1) / 7 + 1 = monthDay) OR
            
            -- Yearly pattern
            (repeatType = 'YEARLY' AND 
             strftime('%m-%d', :date) = strftime('%m-%d', startDate) AND
             (strftime('%Y', :date) - strftime('%Y', startDate)) % interval = 0)
        )
    """)
    suspend fun getActivePatternsForDate(
        date: LocalDate
    ): List<RecurrencePattern>
    
    /**
     * Insert a new recurrence pattern
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: RecurrencePattern): Long
    
    /**
     * Update an existing recurrence pattern
     */
    @Update
    suspend fun update(pattern: RecurrencePattern)
    
    /**
     * Delete a recurrence pattern
     */
    @Delete
    suspend fun delete(pattern: RecurrencePattern)
    
    /**
     * Update the last run time for a recurrence pattern
     */
    @Query("UPDATE recurrence_patterns SET lastRun = :date, updatedAt = CURRENT_TIMESTAMP WHERE id = :id")
    suspend fun updateLastRun(id: Long, date: LocalDate)
    
    companion object {
        // Raw query helpers removed to avoid unresolved references and compile-time constants in annotations
    }
}
