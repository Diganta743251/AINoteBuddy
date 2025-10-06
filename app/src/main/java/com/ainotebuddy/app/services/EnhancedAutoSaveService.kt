package com.ainotebuddy.app.services

import android.content.Context
import androidx.work.*
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.hilt.work.HiltWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced auto-save service with better error handling, conflict resolution,
 * and save optimization features
 */
@Singleton
class EnhancedAutoSaveService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NoteRepository
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Auto-save configuration
    data class AutoSaveConfig(
        val isEnabled: Boolean = true,
        val saveIntervalSeconds: Long = 3, // Auto-save every 3 seconds
        val maxRetries: Int = 3,
        val backupEnabled: Boolean = true,
        val conflictResolution: ConflictResolution = ConflictResolution.MERGE_LATEST
    )
    
    enum class ConflictResolution {
        OVERWRITE_LOCAL, // Overwrite local changes
        KEEP_LOCAL,      // Keep local changes
        MERGE_LATEST,    // Merge with latest timestamp wins
        PROMPT_USER      // Show conflict dialog
    }
    
    enum class SaveStatus {
        IDLE, SAVING, SAVED, ERROR, CONFLICT
    }
    
    data class SaveResult(
        val status: SaveStatus,
        val timestamp: Long = System.currentTimeMillis(),
        val error: String? = null,
        val conflictInfo: ConflictInfo? = null
    )
    
    data class ConflictInfo(
        val localVersion: NoteEntity,
        val remoteVersion: NoteEntity,
        val conflictType: ConflictType
    )
    
    enum class ConflictType {
        CONTENT_MODIFIED, TITLE_MODIFIED, METADATA_MODIFIED, FULL_CONFLICT
    }
    
    // Active save states for each note
    private val saveStates = mutableMapOf<Long, MutableStateFlow<SaveResult>>()
    private val pendingSaves = mutableMapOf<Long, Job>()
    private val saveQueue = mutableMapOf<Long, NoteEntity>()
    
    private val _config = MutableStateFlow(AutoSaveConfig())
    val config: StateFlow<AutoSaveConfig> = _config.asStateFlow()
    
    /**
     * Get save status for a specific note
     */
    fun getSaveStatus(noteId: Long): StateFlow<SaveResult> {
        return saveStates.getOrPut(noteId) {
            MutableStateFlow(SaveResult(SaveStatus.IDLE))
        }
    }
    
    /**
     * Configure auto-save settings
     */
    fun configure(config: AutoSaveConfig) {
        _config.value = config
    }
    
    /**
     * Start auto-saving for a note with enhanced features
     */
    fun startAutoSave(noteId: Long, getNoteData: suspend () -> NoteEntity?) {
        if (!_config.value.isEnabled) return
        
        // Cancel existing auto-save for this note
        stopAutoSave(noteId)
        
        val job = serviceScope.launch {
            var lastSavedContent = ""
            var lastSavedTitle = ""
            var saveAttempts = 0
            
            while (isActive) {
                try {
                    val note = getNoteData()
                    if (note != null) {
                        val hasContentChanged = note.content != lastSavedContent
                        val hasTitleChanged = note.title != lastSavedTitle
                        
                        if (hasContentChanged || hasTitleChanged) {
                            // Update save status to saving
                            updateSaveStatus(noteId, SaveResult(SaveStatus.SAVING))
                            
                            // Perform save with conflict detection
                            val saveResult = saveWithConflictDetection(note)
                            
                            when (saveResult.status) {
                                SaveStatus.SAVED -> {
                                    lastSavedContent = note.content
                                    lastSavedTitle = note.title
                                    saveAttempts = 0
                                    updateSaveStatus(noteId, saveResult)
                                }
                                SaveStatus.ERROR -> {
                                    saveAttempts++
                                    if (saveAttempts >= _config.value.maxRetries) {
                                        updateSaveStatus(noteId, saveResult)
                                        // Schedule background retry
                                        scheduleBackgroundSave(note)
                                        break
                                    } else {
                                        // Wait and retry
                        delay(1000L * saveAttempts)
                                    }
                                }
                                SaveStatus.CONFLICT -> {
                                    updateSaveStatus(noteId, saveResult)
                                    handleConflict(saveResult.conflictInfo!!)
                                    break
                                }
                                else -> {
                                    updateSaveStatus(noteId, saveResult)
                                }
                            }
                        } else {
                            // No changes, update status to saved if previously saving
                            val currentStatus = getSaveStatus(noteId).value.status
                            if (currentStatus == SaveStatus.SAVING) {
                                updateSaveStatus(noteId, SaveResult(SaveStatus.SAVED))
                            }
                        }
                    }
                } catch (e: Exception) {
                    updateSaveStatus(
                        noteId, 
                        SaveResult(SaveStatus.ERROR, error = e.message)
                    )
                }
                
                delay(_config.value.saveIntervalSeconds * 1000)
            }
        }
        
        pendingSaves[noteId] = job
    }
    
    /**
     * Stop auto-saving for a note
     */
    fun stopAutoSave(noteId: Long) {
        pendingSaves[noteId]?.cancel()
        pendingSaves.remove(noteId)
    }
    
    /**
     * Manually save a note immediately
     */
    suspend fun saveImmediately(note: NoteEntity): SaveResult {
        updateSaveStatus(note.id, SaveResult(SaveStatus.SAVING))
        
        return try {
            val result = saveWithConflictDetection(note)
            updateSaveStatus(note.id, result)
            result
        } catch (e: Exception) {
            val errorResult = SaveResult(SaveStatus.ERROR, error = e.message)
            updateSaveStatus(note.id, errorResult)
            errorResult
        }
    }
    
    /**
     * Enhanced save with conflict detection and resolution
     */
    private suspend fun saveWithConflictDetection(note: NoteEntity): SaveResult {
        return try {
            // Check if note exists and has been modified by another source
            val existingNote = repository.getNoteById(note.id)
            
            if (existingNote != null && note.id != 0L) {
                val hasConflict = detectConflict(note, existingNote)
                
                if (hasConflict != null) {
                    when (_config.value.conflictResolution) {
                        ConflictResolution.OVERWRITE_LOCAL -> {
                            // Keep the existing note, ignore local changes
                            SaveResult(SaveStatus.SAVED)
                        }
                        ConflictResolution.KEEP_LOCAL -> {
                            // Force save local changes
                            repository.updateNote(note)
                            SaveResult(SaveStatus.SAVED)
                        }
                        ConflictResolution.MERGE_LATEST -> {
                            val mergedNote = mergeNotes(note, existingNote)
                            repository.updateNote(mergedNote)
                            SaveResult(SaveStatus.SAVED)
                        }
                        ConflictResolution.PROMPT_USER -> {
                            SaveResult(
                                status = SaveStatus.CONFLICT,
                                conflictInfo = hasConflict
                            )
                        }
                    }
                } else {
                    // No conflict, save normally
                    if (note.id == 0L) {
                        repository.insertNote(note)
                    } else {
                        repository.updateNote(note)
                    }
                    
                    // Create backup if enabled
                    if (_config.value.backupEnabled) {
                        createBackup(note)
                    }
                    
                    SaveResult(SaveStatus.SAVED)
                }
            } else {
                // New note or no existing version
                if (note.id == 0L) {
                    repository.insertNote(note)
                } else {
                    repository.updateNote(note)
                }
                
                if (_config.value.backupEnabled) {
                    createBackup(note)
                }
                
                SaveResult(SaveStatus.SAVED)
            }
        } catch (e: Exception) {
            SaveResult(SaveStatus.ERROR, error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Detect conflicts between local and remote versions
     */
    private fun detectConflict(localNote: NoteEntity, remoteNote: NoteEntity): ConflictInfo? {
        // Simple conflict detection based on update timestamps
        if (localNote.updatedAt < remoteNote.updatedAt) {
            val conflictType = when {
                localNote.content != remoteNote.content && localNote.title != remoteNote.title -> 
                    ConflictType.FULL_CONFLICT
                localNote.content != remoteNote.content -> 
                    ConflictType.CONTENT_MODIFIED
                localNote.title != remoteNote.title -> 
                    ConflictType.TITLE_MODIFIED
                else -> 
                    ConflictType.METADATA_MODIFIED
            }
            
            return ConflictInfo(localNote, remoteNote, conflictType)
        }
        
        return null
    }
    
    /**
     * Merge two conflicting note versions
     */
    private fun mergeNotes(localNote: NoteEntity, remoteNote: NoteEntity): NoteEntity {
        return when {
            localNote.updatedAt > remoteNote.updatedAt -> localNote
            remoteNote.updatedAt > localNote.updatedAt -> remoteNote
            else -> {
                // Same timestamp, merge content intelligently
                val mergedContent = if (localNote.content.length > remoteNote.content.length) {
                    localNote.content
                } else {
                    remoteNote.content
                }
                
                localNote.copy(
                    content = mergedContent,
                    title = if (localNote.title.isNotBlank()) localNote.title else remoteNote.title,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * Handle conflict resolution
     */
    private suspend fun handleConflict(conflictInfo: ConflictInfo) {
        // In a real implementation, this would show a dialog or notification
        // For now, just log the conflict
        println("Conflict detected: ${conflictInfo.conflictType}")
    }
    
    /**
     * Create a backup of the note
     */
    private suspend fun createBackup(note: NoteEntity) {
        try {
            val backupNote = note.copy(
                id = 0, // New backup entry
                title = "[BACKUP] ${note.title}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertNote(backupNote)
        } catch (e: Exception) {
            // Backup failed, but don't fail the main save
            println("Backup creation failed: ${e.message}")
        }
    }
    
    /**
     * Schedule background save using WorkManager for failed saves
     */
    private fun scheduleBackgroundSave(note: NoteEntity) {
        val saveData = workDataOf(
            "noteId" to note.id,
            "title" to note.title,
            "content" to note.content,
            "tags" to note.tags,
            "isFavorite" to note.isFavorite
        )
        
        val saveRequest = OneTimeWorkRequestBuilder<BackgroundSaveWorker>()
            .setInputData(saveData)
            .setInitialDelay(30, TimeUnit.SECONDS) // Retry after 30 seconds
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(saveRequest)
    }
    
    /**
     * Update save status for UI
     */
    private fun updateSaveStatus(noteId: Long, result: SaveResult) {
        saveStates.getOrPut(noteId) {
            MutableStateFlow(result)
        }.value = result
    }
    
    /**
     * Get save statistics
     */
    fun getSaveStatistics(): SaveStatistics {
        val totalNotes = saveStates.size
        val savedCount = saveStates.values.count { it.value.status == SaveStatus.SAVED }
        val errorCount = saveStates.values.count { it.value.status == SaveStatus.ERROR }
        val savingCount = saveStates.values.count { it.value.status == SaveStatus.SAVING }
        
        return SaveStatistics(
            totalNotes = totalNotes,
            savedCount = savedCount,
            errorCount = errorCount,
            savingCount = savingCount
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        pendingSaves.values.forEach { it.cancel() }
        pendingSaves.clear()
        saveStates.clear()
        serviceScope.cancel()
    }
}

/**
 * Data class for save statistics
 */
data class SaveStatistics(
    val totalNotes: Int,
    val savedCount: Int,
    val errorCount: Int,
    val savingCount: Int
) {
    val successRate: Float = if (totalNotes > 0) savedCount.toFloat() / totalNotes else 0f
}

/**
 * Background worker for failed saves with Hilt DI
 */
@HiltWorker
class BackgroundSaveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: NoteRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val noteId = inputData.getLong("noteId", 0)
            val title = inputData.getString("title") ?: ""
            val content = inputData.getString("content") ?: ""
            val tags = inputData.getString("tags") ?: ""
            val isFavorite = inputData.getBoolean("isFavorite", false)
            
            val note = NoteEntity(
                id = noteId,
                title = title,
                content = content,
                tags = tags,
                isFavorite = isFavorite,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            repository.updateNote(note)
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}