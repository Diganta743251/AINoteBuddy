package com.ainotebuddy.app.repository

import com.ainotebuddy.app.ai.AIService
import com.ainotebuddy.app.data.*
import com.ainotebuddy.app.offline.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Offline-First Note Repository
 * Integrates the Enhanced Offline-First Architecture with existing repository functionality
 * Provides seamless offline operations with intelligent sync and conflict resolution
 */
@Singleton
class OfflineFirstNoteRepository @Inject constructor(
    private val baseRepository: AdvancedNoteRepository,
    private val offlineOperationManager: OfflineOperationManager,
    private val networkStateManager: NetworkStateManager,
    private val dataIntegrityManager: DataIntegrityManager,
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val templateDao: TemplateDao,
    private val checklistItemDao: ChecklistItemDao,
    private val aiService: AIService
) {
    
    // Expose base repository flows with offline-first enhancements
    val allNotes: Flow<List<NoteEntity>> = baseRepository.allNotes
    val favoriteNotes: Flow<List<NoteEntity>> = baseRepository.favoriteNotes
    val pinnedNotes: Flow<List<NoteEntity>> = baseRepository.pinnedNotes
    val archivedNotes: Flow<List<NoteEntity>> = baseRepository.archivedNotes
    val vaultNotes: Flow<List<NoteEntity>> = baseRepository.vaultNotes
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val noteCategories: Flow<List<String>> = baseRepository.noteCategories
    val allTags: Flow<List<TagEntity>> = baseRepository.allTags
    val allTemplates: Flow<List<TemplateEntity>> = baseRepository.allTemplates
    
    // Offline-first operation status
    val operationStatus: StateFlow<Map<String, OperationStatus>> = offlineOperationManager.operationStatus
    val queueStatistics: StateFlow<QueueStatistics> = offlineOperationManager.queueStatistics
    val networkState: StateFlow<NetworkState> = networkStateManager.networkState
    val syncRecommendation: StateFlow<SyncRecommendation> = networkStateManager.syncRecommendation
    
    /**
     * Enhanced note creation with offline-first support
     */
    suspend fun createNote(note: NoteEntity): OfflineOperationResult = withContext(Dispatchers.IO) {
        try {
            // Validate data integrity before creating
            val validationResult = dataIntegrityManager.validateNoteIntegrity(note)
            if (!validationResult.isValid) {
                return@withContext OfflineOperationResult.failure("Note validation failed: ${validationResult.errors.joinToString()}")
            }
            
            val networkState = networkStateManager.getCurrentNetworkState()
            
            if (networkState.isConnected && networkStateManager.getSyncRecommendation() != SyncRecommendation.WAIT) {
                // Online - try direct creation first
                try {
                    val noteId = baseRepository.insert(note)
                    val createdNote = note.copy(id = noteId)
                    
                    // Update sync state
                    updateSyncStateAfterSuccess("note", noteId.toString(), createdNote)
                    
                    OfflineOperationResult.success(noteId.toString(), "Note created successfully")
                } catch (e: Exception) {
                    // Failed online - queue for offline processing
                    queueCreateNoteOperation(note)
                }
            } else {
                // Offline - queue operation
                queueCreateNoteOperation(note)
            }
        } catch (e: Exception) {
            OfflineOperationResult.failure("Failed to create note: ${e.message}")
        }
    }
    
    /**
     * Enhanced note update with conflict resolution
     */
    suspend fun updateNote(note: NoteEntity, changes: Map<String, Any> = emptyMap()): OfflineOperationResult = withContext(Dispatchers.IO) {
        try {
            // Validate data integrity
            val validationResult = dataIntegrityManager.validateNoteIntegrity(note)
            if (!validationResult.isValid) {
                // Apply automatic corrections if possible
                val correctionResult = dataIntegrityManager.applyAutomaticCorrections(note, validationResult.correctionSuggestions)
                if (!correctionResult.success) {
                    return@withContext OfflineOperationResult.failure("Note validation and correction failed")
                }
            }
            
            val networkState = networkStateManager.getCurrentNetworkState()
            
            if (networkState.isConnected && networkStateManager.getSyncRecommendation() != SyncRecommendation.WAIT) {
                // Online - try direct update with conflict detection
                try {
                    val currentNote = baseRepository.getNoteById(note.id)
                    if (currentNote != null && currentNote.version > note.version) {
                        // Version conflict detected - queue for conflict resolution
                        return@withContext queueUpdateNoteOperation(note, changes, currentNote)
                    }
                    
                    baseRepository.update(note)
                    updateSyncStateAfterSuccess("note", note.id.toString(), note)
                    
                    OfflineOperationResult.success(note.id.toString(), "Note updated successfully")
                } catch (e: Exception) {
                    // Failed online - queue for offline processing
                    queueUpdateNoteOperation(note, changes)
                }
            } else {
                // Offline - queue operation
                queueUpdateNoteOperation(note, changes)
            }
        } catch (e: Exception) {
            OfflineOperationResult.failure("Failed to update note: ${e.message}")
        }
    }
    
    /**
     * Enhanced note deletion with soft delete support
     */
    suspend fun deleteNote(noteId: Long, softDelete: Boolean = true): OfflineOperationResult = withContext(Dispatchers.IO) {
        try {
            val networkState = networkStateManager.getCurrentNetworkState()
            
            if (networkState.isConnected && networkStateManager.getSyncRecommendation() != SyncRecommendation.WAIT) {
                // Online - try direct deletion
                try {
                    val note = baseRepository.getNoteById(noteId)
                    if (note != null) {
                        if (softDelete) {
                            val deletedNote = note.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
                            baseRepository.update(deletedNote)
                        } else {
                            baseRepository.delete(note)
                        }
                        updateSyncStateAfterSuccess("note", noteId.toString(), note)
                    }
                    OfflineOperationResult.success(noteId.toString(), "Note deleted successfully")
                } catch (e: Exception) {
                    // Failed online - queue for offline processing
                    queueDeleteNoteOperation(noteId, softDelete)
                }
            } else {
                // Offline - queue operation
                queueDeleteNoteOperation(noteId, softDelete)
            }
        } catch (e: Exception) {
            OfflineOperationResult.failure("Failed to delete note: ${e.message}")
        }
    }
    
    /**
     * Enhanced note creation with AI assistance
     */
    suspend fun createNoteWithAI(title: String, content: String): OfflineOperationResult = withContext(Dispatchers.IO) {
        try {
            val networkState = networkStateManager.getCurrentNetworkState()
            
            // Check if we can use AI (online or local AI available)
            val canUseAI = networkState.isConnected || hasLocalAICapabilities()
            
            if (canUseAI) {
                try {
                    val noteId = baseRepository.insertWithAI(title, content)
                    val note = baseRepository.getNoteById(noteId)
                    if (note != null) {
                        updateSyncStateAfterSuccess("note", noteId.toString(), note)
                    }
                    OfflineOperationResult.success(noteId.toString(), "Note created with AI assistance")
                } catch (e: Exception) {
                    // AI failed - queue for later processing
                    queueAIAnalysisOperation(title, content)
                }
            } else {
                // No AI available - create basic note and queue AI analysis
                val basicNote = NoteEntity(
                    title = title,
                    content = content,
                    category = "General",
                    wordCount = content.split("\\s+").filter { it.isNotBlank() }.size,
                    readTime = maxOf(1, content.split("\\s+").filter { it.isNotBlank() }.size / 200)
                )
                
                val result = createNote(basicNote)
                if (result.success) {
                    // Queue AI analysis for when network becomes available
                    queueAIAnalysisOperation(title, content, result.operationId)
                }
                result
            }
        } catch (e: Exception) {
            OfflineOperationResult.failure("Failed to create note with AI: ${e.message}")
        }
    }
    
    /**
     * Batch operations with offline support
     */
    suspend fun batchUpdateNotes(updates: List<Pair<Long, Map<String, Any>>>): BatchOperationResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<OfflineOperationResult>()
        val networkState = networkStateManager.getCurrentNetworkState()
        
        for ((noteId, changes) in updates) {
            try {
                val note = baseRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = applyChangesToNote(note, changes)
                    val result = updateNote(updatedNote, changes)
                    results.add(result)
                } else {
                    results.add(OfflineOperationResult.failure("Note not found: $noteId"))
                }
            } catch (e: Exception) {
                results.add(OfflineOperationResult.failure("Failed to update note $noteId: ${e.message}"))
            }
        }
        
        val successCount = results.count { it.success }
        val failureCount = results.count { !it.success }
        
        BatchOperationResult(
            success = failureCount == 0,
            totalOperations = updates.size,
            successfulOperations = successCount,
            failedOperations = failureCount,
            results = results
        )
    }
    
    /**
     * Force sync all pending operations
     */
    suspend fun forceSyncAll(): SyncResult = withContext(Dispatchers.IO) {
        try {
            offlineOperationManager.forceSyncAll()
            
            // Wait for sync completion with timeout
            var attempts = 0
            val maxAttempts = 30
            
            while (attempts < maxAttempts) {
                val stats = queueStatistics.value
                if (stats.pendingOperations == 0) {
                    break
                }
                kotlinx.coroutines.delay(1000)
                attempts++
            }
            
            val finalStats = queueStatistics.value
            SyncResult(
                success = finalStats.pendingOperations == 0,
                operationId = "sync_all",
                syncedAt = System.currentTimeMillis(),
                conflicts = emptyList(),
                errorMessage = if (finalStats.pendingOperations > 0) "Sync timeout - ${finalStats.pendingOperations} operations still pending" else null
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                operationId = "sync_all",
                syncedAt = System.currentTimeMillis(),
                errorMessage = "Sync failed: ${e.message}"
            )
        }
    }
    
    /**
     * Get sync status for a specific note
     */
    suspend fun getNoteSyncStatus(noteId: Long): NoteSyncStatus = withContext(Dispatchers.IO) {
        try {
            val syncState = offlineOperationManager.getSyncState("note", noteId.toString())
            val pendingOperations = offlineOperationManager.getPendingOperationsForEntity("note", noteId.toString())
            
            NoteSyncStatus(
                noteId = noteId,
                syncStatus = syncState?.syncStatus ?: "UNKNOWN",
                lastSyncedAt = syncState?.lastSyncedAt ?: 0,
                hasPendingOperations = pendingOperations.isNotEmpty(),
                pendingOperationCount = pendingOperations.size,
                hasConflicts = syncState?.syncStatus == "CONFLICT"
            )
        } catch (e: Exception) {
            NoteSyncStatus(
                noteId = noteId,
                syncStatus = "ERROR",
                lastSyncedAt = 0,
                hasPendingOperations = false,
                pendingOperationCount = 0,
                hasConflicts = false,
                errorMessage = e.message
            )
        }
    }
    
    // Delegate other operations to base repository
    suspend fun getNoteById(noteId: Long): NoteEntity? = baseRepository.getNoteById(noteId)
    fun searchNotes(query: String): Flow<List<NoteEntity>> = baseRepository.searchNotes(query)
    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> = baseRepository.getNotesByCategory(category)
    fun getNotesByTag(tag: String): Flow<List<NoteEntity>> = baseRepository.getNotesByTag(tag)
    fun getChecklistItems(noteId: Long): Flow<List<ChecklistItemEntity>> = baseRepository.getChecklistItems(noteId)
    
    // Quick actions with offline support
    suspend fun togglePin(noteId: Long, isPinned: Boolean): OfflineOperationResult = 
        updateNoteField(noteId, "isPinned", isPinned)
    
    suspend fun toggleFavorite(noteId: Long, isFavorite: Boolean): OfflineOperationResult = 
        updateNoteField(noteId, "isFavorite", isFavorite)
    
    suspend fun toggleArchive(noteId: Long, isArchived: Boolean): OfflineOperationResult = 
        updateNoteField(noteId, "isArchived", isArchived)
    
    suspend fun updateCategory(noteId: Long, category: String): OfflineOperationResult = 
        updateNoteField(noteId, "category", category)
    
    suspend fun updateTags(noteId: Long, tags: String): OfflineOperationResult = 
        updateNoteField(noteId, "tags", tags)
    
    // Private helper methods
    private suspend fun queueCreateNoteOperation(note: NoteEntity): OfflineOperationResult {
        val tempId = "temp_${System.currentTimeMillis()}_${Random().nextInt(1000)}"
        val noteJson = com.google.gson.Gson().toJson(note.copy(id = 0))
        val operation = OfflineOperation.CreateNote(
            id = UUID.randomUUID().toString(),
            noteData = noteJson, // Store JSON instead of entity
            tempId = tempId,
            timestamp = System.currentTimeMillis()
        )
        
        val operationId = offlineOperationManager.queueOperation(operation)
        return OfflineOperationResult.queued(operationId, "Note creation queued for sync")
    }
    
    private suspend fun queueUpdateNoteOperation(
        note: NoteEntity, 
        changes: Map<String, Any>, 
        conflictingNote: NoteEntity? = null
    ): OfflineOperationResult {
        val changesMap = changes.mapValues { it.value.toString() }
        val operation = OfflineOperation.UpdateNote(
            id = UUID.randomUUID().toString(),
            noteId = note.id,
            changes = changesMap,
            previousVersion = (conflictingNote?.version ?: note.version).toLong(),
            timestamp = System.currentTimeMillis()
        )
        
        val operationId = offlineOperationManager.queueOperation(operation)
        return OfflineOperationResult.queued(operationId, "Note update queued for sync")
    }
    
    private suspend fun queueDeleteNoteOperation(noteId: Long, softDelete: Boolean): OfflineOperationResult {
        val operation = OfflineOperation.DeleteNote(
            id = UUID.randomUUID().toString(),
            noteId = noteId,
            softDelete = softDelete,
            timestamp = System.currentTimeMillis()
        )
        
        val operationId = offlineOperationManager.queueOperation(operation)
        return OfflineOperationResult.queued(operationId, "Note deletion queued for sync")
    }
    
    private suspend fun queueAIAnalysisOperation(title: String, content: String, noteId: String? = null): OfflineOperationResult {
        val operation = OfflineOperation.AIAnalysis(
            id = UUID.randomUUID().toString(),
            noteId = noteId?.toLongOrNull() ?: 0L,
            analysisType = "FULL_ANALYSIS",
            content = "$title\n\n$content",
            timestamp = System.currentTimeMillis()
        )
        
        val operationId = offlineOperationManager.queueOperation(operation)
        return OfflineOperationResult.queued(operationId, "AI analysis queued for processing")
    }
    
    private suspend fun updateNoteField(noteId: Long, field: String, value: Any): OfflineOperationResult {
        val note = getNoteById(noteId)
        return if (note != null) {
            val changes = mapOf(field to value)
            updateNote(note, changes)
        } else {
            OfflineOperationResult.failure("Note not found: $noteId")
        }
    }
    
    private fun applyChangesToNote(note: NoteEntity, changes: Map<String, Any>): NoteEntity {
        return note.copy(
            title = changes["title"]?.toString() ?: note.title,
            content = changes["content"]?.toString() ?: note.content,
            category = changes["category"]?.toString() ?: note.category,
            tags = changes["tags"]?.toString() ?: note.tags,
            isPinned = changes["isPinned"] as? Boolean ?: note.isPinned,
            isFavorite = changes["isFavorite"] as? Boolean ?: note.isFavorite,
            isArchived = changes["isArchived"] as? Boolean ?: note.isArchived,
            color = changes["color"] as? Int ?: note.color,
            format = changes["format"]?.toString() ?: note.format,
            updatedAt = System.currentTimeMillis(),
            version = note.version + 1
        )
    }
    
    private suspend fun updateSyncStateAfterSuccess(entityType: String, entityId: String, entity: NoteEntity) {
        // Update sync state to reflect successful operation
        // This would integrate with the sync state management system
    }
    
    private fun hasLocalAICapabilities(): Boolean {
        // Check if local AI processing is available
        // This would depend on the AI service implementation
        return false // Placeholder
    }
}

// Data classes for offline-first repository results
data class OfflineOperationResult(
    val success: Boolean,
    val operationId: String,
    val message: String,
    val isQueued: Boolean = false,
    val errorDetails: String? = null
) {
    companion object {
        fun success(operationId: String, message: String) = OfflineOperationResult(true, operationId, message)
        fun failure(message: String, errorDetails: String? = null) = OfflineOperationResult(false, "", message, errorDetails = errorDetails)
        fun queued(operationId: String, message: String) = OfflineOperationResult(true, operationId, message, isQueued = true)
    }
}

data class BatchOperationResult(
    val success: Boolean,
    val totalOperations: Int,
    val successfulOperations: Int,
    val failedOperations: Int,
    val results: List<OfflineOperationResult>
)

data class NoteSyncStatus(
    val noteId: Long,
    val syncStatus: String,
    val lastSyncedAt: Long,
    val hasPendingOperations: Boolean,
    val pendingOperationCount: Int,
    val hasConflicts: Boolean,
    val errorMessage: String? = null
)
