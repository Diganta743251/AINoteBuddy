package com.ainotebuddy.app.repository

import com.ainotebuddy.app.data.NoteDao
import com.ainotebuddy.app.data.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()

    suspend fun insert(note: NoteEntity) = dao.insert(note)
    suspend fun update(note: NoteEntity) = dao.update(note)
    suspend fun delete(note: NoteEntity) = dao.delete(note)
    suspend fun deleteAll() = dao.deleteAllDeleted()
    suspend fun countNotes() = dao.getNoteCount()
    suspend fun getNoteById(noteId: Long) = dao.getNoteById(noteId)
    suspend fun addNote(note: NoteEntity) {
        dao.insert(note)
    }
    suspend fun updateNote(note: NoteEntity) {
        dao.update(note)
    }
    suspend fun deleteNote(noteId: Long) {
        dao.deleteById(noteId)
    }
}
