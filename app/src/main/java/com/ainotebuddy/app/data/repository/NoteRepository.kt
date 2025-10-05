package com.ainotebuddy.app.data.repository

import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.data.toDomain
import com.ainotebuddy.app.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for note repository operations
 */
interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    suspend fun insertNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    fun searchNotes(query: String): Flow<List<Note>>
}

/**
 * Implementation of NoteRepository
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val delegateRepository: com.ainotebuddy.app.repository.NoteRepository
) : NoteRepository {
    
    override fun getAllNotes(): Flow<List<Note>> {
        return delegateRepository.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getNoteById(id: String): Note? {
        return try {
            val longId = id.toLongOrNull() ?: return null
            delegateRepository.getNoteById(longId)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun insertNote(note: Note) {
        delegateRepository.insertNote(note.toEntity())
    }
    
    override suspend fun updateNote(note: Note) {
        delegateRepository.updateNote(note.toEntity())
    }
    
    override suspend fun deleteNote(note: Note) {
        delegateRepository.deleteNote(note.toEntity())
    }
    
    override fun searchNotes(query: String): Flow<List<Note>> {
        return getAllNotes().map { notes ->
            notes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.tags.any { it.contains(query, ignoreCase = true) }
            }
        }
    }
}