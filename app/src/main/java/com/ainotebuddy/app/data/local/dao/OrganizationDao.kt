package com.ainotebuddy.app.data.local.dao

import androidx.room.*
import com.ainotebuddy.app.data.local.entity.organization.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for organization-related database operations
 */
@Dao
interface OrganizationDao {
    // Smart Folders
    @Query("SELECT * FROM smart_folders ORDER BY name ASC")
    fun observeSmartFolders(): Flow<List<SmartFolderEntity>>

    @Query("SELECT * FROM smart_folders ORDER BY name ASC")
    suspend fun getSmartFolders(): List<SmartFolderEntity>

    @Query("SELECT * FROM smart_folders WHERE id = :id")
    suspend fun getSmartFolderById(id: String): SmartFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartFolder(folder: SmartFolderEntity): Long

    @Update
    suspend fun updateSmartFolder(folder: SmartFolderEntity): Int

    @Query("DELETE FROM smart_folders WHERE id = :id")
    suspend fun deleteSmartFolder(id: String)

    // Note Templates
    @Query("SELECT * FROM note_templates WHERE is_enabled = 1 ORDER BY name ASC")
    fun observeTemplates(): Flow<List<NoteTemplateEntity>>

    @Query("SELECT * FROM note_templates WHERE is_enabled = 1 ORDER BY name ASC")
    suspend fun getTemplates(): List<NoteTemplateEntity>

    @Query("SELECT * FROM note_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): NoteTemplateEntity?

    @Query("SELECT * FROM note_templates WHERE category = :category AND is_enabled = 1 ORDER BY name ASC")
    suspend fun getTemplatesByCategory(category: String): List<NoteTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: NoteTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: NoteTemplateEntity): Int

    @Query("DELETE FROM note_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    // Recurring Notes
    @Query(
        """
        SELECT * FROM recurring_notes 
        WHERE is_active = 1 
        ORDER BY next_trigger ASC
        """
    )
    fun observeRecurringNotes(): Flow<List<RecurringNoteEntity>>

    @Query(
        """
        SELECT * FROM recurring_notes 
        WHERE is_active = 1 
        ORDER BY next_trigger ASC
        """
    )
    suspend fun getRecurringNotes(): List<RecurringNoteEntity>

    @Query(
        """
        SELECT * FROM recurring_notes 
        WHERE is_active = 1 
        AND next_trigger <= :currentTime
        ORDER BY next_trigger ASC
        """
    )
    suspend fun getDueRecurringNotes(currentTime: Long): List<RecurringNoteEntity>

    @Query("SELECT * FROM recurring_notes WHERE id = :id")
    suspend fun getRecurringNoteById(id: String): RecurringNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringNote(note: RecurringNoteEntity): Long

    @Update
    suspend fun updateRecurringNote(note: RecurringNoteEntity): Int

    @Query("DELETE FROM recurring_notes WHERE id = :id")
    suspend fun deleteRecurringNote(id: String)

    @Query(
        """
        UPDATE recurring_notes 
        SET last_triggered = :currentTime, 
            next_trigger = :nextTrigger,
            updated_at = :currentTime
        WHERE id = :id
        """
    )
    suspend fun markRecurringNoteTriggered(id: String, nextTrigger: Long, currentTime: Long = System.currentTimeMillis())
}
