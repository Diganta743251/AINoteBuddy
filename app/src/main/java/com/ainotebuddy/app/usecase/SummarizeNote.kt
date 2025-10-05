package com.ainotebuddy.app.usecase

import com.ainotebuddy.app.ai.local.AISummarizer
import com.ainotebuddy.app.repository.NoteRepository

class SummarizeNote(
    private val noteRepository: NoteRepository,
    private val summarizer: AISummarizer
) {
    suspend operator fun invoke(noteId: Long): String {
        val note = noteRepository.getNoteById(noteId) ?: return ""
        return summarizer.summarize(note.content)
    }
}