package com.ainotebuddy.app.repository

import com.ainotebuddy.app.data.NoteDao
import com.ainotebuddy.app.data.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for note operations - properly uses DI
 * This is the main repository used throughout the app
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()
    
    fun getSearchResults(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    
    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)
    
    suspend fun getNoteById(id: String): NoteEntity? = try {
        noteDao.getNoteById(id.toLong())
    } catch (e: NumberFormatException) {
        null
    }
    
    suspend fun insertNote(note: NoteEntity): Long = noteDao.insert(note)
    
    suspend fun updateNote(note: NoteEntity) = noteDao.update(note)
    
    suspend fun deleteNote(note: NoteEntity) = noteDao.delete(note)
    
    suspend fun deleteNoteById(id: Long) = noteDao.deleteById(id)
    
    fun getFavoriteNotes(): Flow<List<NoteEntity>> = noteDao.getFavoriteNotes()
    
    fun getPinnedNotes(): Flow<List<NoteEntity>> = noteDao.getPinnedNotes()
    
    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> = noteDao.getNotesByCategory(category)
    
    suspend fun markAsFavorite(id: Long, isFavorite: Boolean) = noteDao.toggleFavorite(id, isFavorite)
    
    suspend fun markAsPinned(id: Long, isPinned: Boolean) = noteDao.togglePin(id, isPinned)

    suspend fun setReminder(noteId: Long, timeMillis: Long?) = noteDao.setReminder(noteId, timeMillis)
    
    suspend fun getAllCategories(): Flow<List<String>> = noteDao.getAllCategories()
    
    suspend fun getAllTags(): List<String> = noteDao.getAllTags()
}
