package com.ainotebuddy.app.data.repository.organization

import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Organization-specific note repository interface
 */
interface OrganizationNoteRepository {
    /**
     * Create a new note from a template with variable replacement and return the created note ID.
     */
    suspend fun createNoteFromTemplate(template: NoteTemplate, variables: Map<String, String>, titleOverride: String? = null): Long
    suspend fun getNoteById(id: String): Note?
    suspend fun insertNote(note: Note)
    suspend fun updateNote(note: Note)
    fun getAllNotes(): Flow<List<Note>>
}

/**
 * Implementation of OrganizationNoteRepository
 */
@Singleton  
class OrganizationNoteRepositoryImpl @Inject constructor(
    private val baseRepository: NoteRepository,
    private val daoRepository: com.ainotebuddy.app.repository.NoteRepository
) : OrganizationNoteRepository {

    private fun renderTemplate(content: String, variables: Map<String, String>): String {
        var result = content
        variables.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    override suspend fun createNoteFromTemplate(template: NoteTemplate, variables: Map<String, String>, titleOverride: String?): Long {
        val title = titleOverride?.takeIf { it.isNotBlank() } ?: template.name
        val content = renderTemplate(template.content, variables)
        val entity = NoteEntity(
            title = title,
            content = content,
            category = template.category
        )
        return daoRepository.insertNote(entity)
    }
    
    override suspend fun getNoteById(id: String): Note? {
        return baseRepository.getNoteById(id)
    }
    
    override suspend fun insertNote(note: Note) {
        return baseRepository.insertNote(note)
    }
    
    override suspend fun updateNote(note: Note) {
        return baseRepository.updateNote(note)
    }
    
    override fun getAllNotes(): Flow<List<Note>> {
        return baseRepository.getAllNotes()
    }
}