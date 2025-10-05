package com.ainotebuddy.app.usecase

import com.ainotebuddy.app.ai.local.AINlp
import com.ainotebuddy.app.repository.NoteRepository

class AutoTagNote(
    private val noteRepository: NoteRepository,
    private val nlp: AINlp
) {
    suspend operator fun invoke(noteId: Long): List<String> {
        val note = noteRepository.getNoteById(noteId) ?: return emptyList()
        val tags = nlp.keyphrases(note.content).take(5)
        // Persist tags via repository if needed; existing repo exposes updateNote
        val updated = note.copy(tags = tags.joinToString(","))
        noteRepository.updateNote(updated)
        return tags
    }
}