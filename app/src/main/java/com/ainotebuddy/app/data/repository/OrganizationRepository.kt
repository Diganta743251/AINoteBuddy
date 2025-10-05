package com.ainotebuddy.app.data.repository

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for organization operations
 */
interface OrganizationRepository {
    // Smart Folders
    suspend fun getSmartFolders(): List<com.ainotebuddy.app.data.model.organization.SmartFolder>
    fun observeSmartFolders(): Flow<List<com.ainotebuddy.app.data.model.organization.SmartFolder>>
    suspend fun getSmartFolderById(id: String): com.ainotebuddy.app.data.model.organization.SmartFolder?
    suspend fun createSmartFolder(folder: com.ainotebuddy.app.data.model.organization.SmartFolder): Result<com.ainotebuddy.app.data.model.organization.SmartFolder>
    suspend fun updateSmartFolder(folder: com.ainotebuddy.app.data.model.organization.SmartFolder): Result<Unit>
    suspend fun deleteSmartFolder(id: String): Result<Unit>
    suspend fun getNotesForSmartFolder(folderId: String): List<String>

    // Templates
    suspend fun getTemplates(): List<com.ainotebuddy.app.data.model.organization.NoteTemplate>
    fun observeTemplates(): Flow<List<com.ainotebuddy.app.data.model.organization.NoteTemplate>>
    suspend fun getTemplateById(id: String): com.ainotebuddy.app.data.model.organization.NoteTemplate?
    suspend fun createTemplate(template: com.ainotebuddy.app.data.model.organization.NoteTemplate): Result<com.ainotebuddy.app.data.model.organization.NoteTemplate>
    suspend fun updateTemplate(template: com.ainotebuddy.app.data.model.organization.NoteTemplate): Result<Unit>
    suspend fun deleteTemplate(id: String): Result<Unit>
    suspend fun getTemplatesByCategory(category: String): List<com.ainotebuddy.app.data.model.organization.NoteTemplate>

    // Recurring Notes
    suspend fun getRecurringNotes(): List<com.ainotebuddy.app.data.model.organization.RecurringNote>
    fun observeRecurringNotes(): Flow<List<com.ainotebuddy.app.data.model.organization.RecurringNote>>
    suspend fun getRecurringNoteById(id: String): com.ainotebuddy.app.data.model.organization.RecurringNote?
    suspend fun createRecurringNote(note: com.ainotebuddy.app.data.model.organization.RecurringNote): Result<com.ainotebuddy.app.data.model.organization.RecurringNote>
    suspend fun updateRecurringNote(note: com.ainotebuddy.app.data.model.organization.RecurringNote): Result<Unit>
    suspend fun deleteRecurringNote(id: String): Result<Unit>
    suspend fun getDueRecurringNotes(currentTime: Long): List<com.ainotebuddy.app.data.model.organization.RecurringNote>
    suspend fun markRecurringNoteTriggered(id: String, nextTrigger: Long): Result<Unit>

    // AI Org helpers
    suspend fun analyzeAndCategorizeNote(noteId: String): Result<Unit>
    suspend fun generateSmartFolderSuggestions(): List<com.ainotebuddy.app.data.model.organization.SmartFolder>

    // Initialization
    suspend fun initializeDefaultContent()
}