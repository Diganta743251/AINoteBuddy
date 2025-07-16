package com.ainotebuddy.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId AND isDeleted = 0")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE category = :category AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByCategory(category: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE tags LIKE '%' || :tag || '%' AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByTag(tag: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT DISTINCT category FROM notes WHERE isDeleted = 0 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT tags FROM notes WHERE tags != '' AND isDeleted = 0")
    suspend fun getAllTags(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1 WHERE id = :noteId")
    suspend fun softDelete(noteId: Long)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :noteId")
    suspend fun togglePin(noteId: Long, isPinned: Boolean)

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :noteId")
    suspend fun toggleFavorite(noteId: Long, isFavorite: Boolean)

    @Query("UPDATE notes SET isArchived = :isArchived WHERE id = :noteId")
    suspend fun toggleArchive(noteId: Long, isArchived: Boolean)

    @Query("UPDATE notes SET category = :category WHERE id = :noteId")
    suspend fun updateCategory(noteId: Long, category: String)

    @Query("UPDATE notes SET tags = :tags WHERE id = :noteId")
    suspend fun updateTags(noteId: Long, tags: String)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun deleteAllDeleted()

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0")
    suspend fun getNoteCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isFavorite = 1 AND isDeleted = 0")
    suspend fun getFavoriteCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isPinned = 1 AND isDeleted = 0")
    suspend fun getPinnedCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 1 AND isDeleted = 0")
    suspend fun getArchivedCount(): Int

    @Query("SELECT * FROM notes WHERE isInVault = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getVaultNotes(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isInVault = :inVault WHERE id = :noteId")
    suspend fun setNoteInVault(noteId: Long, inVault: Boolean)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC LIMIT 1")
    fun getMostRecentNote(): NoteEntity?

    @Query("UPDATE notes SET reminderTime = :reminderTime WHERE id = :noteId")
    suspend fun setReminder(noteId: Long, reminderTime: Long?)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)
}
