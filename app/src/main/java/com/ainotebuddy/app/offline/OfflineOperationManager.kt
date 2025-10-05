package com.ainotebuddy.app.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.*
import com.ainotebuddy.app.data.*
import com.ainotebuddy.app.repository.AdvancedNoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow
import com.google.gson.Gson

/**
 * Core offline operation manager for Enhanced Offline-First Architecture
 * Handles queuing, execution, retry logic, and coordination of all offline operations
 */
@Singleton
class OfflineOperationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineOperationDao: OfflineOperationDao,
    private val syncStateDao: SyncStateDao,
    private val conflictHistoryDao: ConflictHistoryDao,
    private val dataIntegrityDao: DataIntegrityDao,
    private val noteRepository: AdvancedNoteRepository,
    private val networkStateManager: NetworkStateManager,
    private val conflictResolutionEngine: ConflictResolutionEngine,
    private val dataIntegrityManager: DataIntegrityManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val workManager = WorkManager.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _operationStatus = MutableStateFlow<Map<String, OperationStatus>>(emptyMap())
    val operationStatus: StateFlow<Map<String, OperationStatus>> = _operationStatus.asStateFlow()
    
    private val _queueStatistics = MutableStateFlow(QueueStatistics())
    val queueStatistics: StateFlow<QueueStatistics> = _queueStatistics.asStateFlow()
    
    private var isProcessing = false
    private val processingOperations = mutableSetOf<String>()
    
    init {
        // Start monitoring network state and processing operations
        startNetworkMonitoring()
        startOperationProcessing()
        startStatisticsUpdating()
        schedulePeriodicCleanup()
    }
    
    /**
     * Queue a new offline operation
     */
    suspend fun queueOperation(operation: OfflineOperation): String {
        val entity = operation.toEntity()
        offlineOperationDao.insert(entity)
        
        // Update operation status
        updateOperationStatus(operation.id, OperationStatus.PENDING)
        
        // Trigger immediate processing if network allows
        if (canExecuteOperation(operation)) {
            triggerOperationProcessing()
        }
        
        return operation.id
    }
    
    /**
     * Queue multiple operations with dependency handling
     */
    suspend fun queueOperations(operations: List<OfflineOperation>, dependencies: Map<String, List<String>> = emptyMap()): List<String> {
        val entities = operations.map { operation ->
            val dependencyList = dependencies[operation.id] ?: emptyList()
            operation.toEntity().copy(
                dependencies = dependencyList
            )
        }
        
        offlineOperationDao.insertAll(entities)
        
        // Update statuses
        operations.forEach { operation ->
            updateOperationStatus(operation.id, OperationStatus.PENDING)
        }
        
        triggerOperationProcessing()
        return operations.map { it.id }
    }
    
    /**
     * Cancel a pending operation
     */
    suspend fun cancelOperation(operationId: String): Boolean {
        val operation = offlineOperationDao.getById(operationId)
        return if (operation != null && operation.status == "PENDING") {
            offlineOperationDao.updateOperationResult(operationId, "CANCELLED", "Cancelled by user")
            updateOperationStatus(operationId, OperationStatus.CANCELLED)
            true
        } else {
            false
        }
    }
    
    /**
     * Retry a failed operation
     */
    suspend fun retryOperation(operationId: String): Boolean {
        val operation = offlineOperationDao.getById(operationId)
        return if (operation != null && operation.status == "FAILED" && operation.retryCount < operation.maxRetries) {
            val updatedOperation = operation.copy(
                status = "PENDING",
                scheduledAt = System.currentTimeMillis(),
                errorMessage = null
            )
            offlineOperationDao.update(updatedOperation)
            updateOperationStatus(operationId, OperationStatus.PENDING)
            triggerOperationProcessing()
            true
        } else {
            false
        }
    }
    
    /**
     * Get operation details
     */
    suspend fun getOperation(operationId: String): OfflineOperationEntity? {
        return offlineOperationDao.getById(operationId)
    }
    
    /**
     * Get all pending operations
     */
    fun getPendingOperations(): Flow<List<OfflineOperationEntity>> {
        return offlineOperationDao.getPendingOperationsFlow()
    }
    
    /**
     * Get operations by status
     */
    fun getOperationsByStatus(status: OperationStatus): Flow<List<OfflineOperationEntity>> {
        return offlineOperationDao.getOperationsByStatus(status.name)
    }
    
    /**
     * Get sync state for a specific entity
     */
    suspend fun getSyncState(entityType: String, entityId: String): SyncStateEntity? {
        return syncStateDao.getSyncState(entityType, entityId)
    }
    
    /**
     * Get pending operations for a specific entity
     */
    suspend fun getPendingOperationsForEntity(entityType: String, entityId: String): List<OfflineOperationEntity> {
        return offlineOperationDao
            .getPendingOperationsByEntityId(entityId)
            .filter { it.entityType == entityType }
    }
    
    /**
     * Force sync all pending operations (when network becomes available)
     */
    suspend fun forceSyncAll() {
        triggerOperationProcessing()
        
        // Also trigger background sync worker
        val syncWorkRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueue(syncWorkRequest)
    }
    
    /**
     * Start network monitoring
     */
    private fun startNetworkMonitoring() {
        scope.launch {
            networkStateManager.networkState.collect { networkState ->
                if (networkState.isConnected) {
                    // Network became available, trigger processing
                    triggerOperationProcessing()
                }
            }
        }
    }
    
    /**
     * Start continuous operation processing
     */
    private fun startOperationProcessing() {
        scope.launch {
            while (true) {
                try {
                    processOperations()
                    delay(5000) // Process every 5 seconds
                } catch (e: Exception) {
                    // Log error and continue
                    delay(10000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Process pending operations
     */
    private suspend fun processOperations() {
        if (isProcessing) return
        
        isProcessing = true
        try {
            val networkState = networkStateManager.getCurrentNetworkState()
            val currentTime = System.currentTimeMillis()
            
            // Get executable operations based on network state and dependencies
            val executableOps = offlineOperationDao.getExecutableOperations(
                currentNetwork = when {
                    !networkState.isConnected -> "NONE"
                    networkState.connectionType == ConnectionType.WIFI -> "WIFI_ONLY"
                    networkState.connectionType == ConnectionType.MOBILE_DATA -> "MOBILE_DATA_OK"
                    else -> "ANY"
                },
                currentTime = currentTime,
                limit = 10
            )
            
            // Process operations in parallel with limited concurrency
            executableOps.chunked(3).forEach { batch ->
                coroutineScope {
                    val deferreds = batch.map { operation ->
                        async {
                            if (!processingOperations.contains(operation.id)) {
                                processOperation(operation)
                            }
                        }
                    }
                    deferreds.awaitAll()
                }
            }
            
            // Process retry operations
            processRetryOperations(currentTime)
            
        } finally {
            isProcessing = false
        }
    }
    
    /**
     * Process a single operation
     */
    private suspend fun processOperation(operationEntity: OfflineOperationEntity) {
        val operationId = operationEntity.id
        processingOperations.add(operationId)
        
        try {
            updateOperationStatus(operationId, OperationStatus.PROCESSING)
            
            // Check dependencies
            if (!areDependenciesSatisfied(operationEntity)) {
                // Dependencies not met, reschedule
                val rescheduledOp = operationEntity.copy(
                    scheduledAt = System.currentTimeMillis() + 30000 // Reschedule in 30 seconds
                )
                offlineOperationDao.update(rescheduledOp)
                updateOperationStatus(operationId, OperationStatus.PENDING)
                return
            }
            
            // Convert to operation object
            val operation = operationEntity.toOperation()
            if (operation == null) {
                markOperationFailed(operationId, "Invalid operation data")
                return
            }
            
            // Execute the operation
            val result = executeOperation(operation)
            
            if (result.success) {
                // Mark as successful
                offlineOperationDao.updateOperationResult(operationId, "SUCCESS", null)
                updateOperationStatus(operationId, OperationStatus.SUCCESS)
                
                // Update sync state if applicable
                updateSyncStateAfterSuccess(operation, result)
                
            } else {
                // Handle failure
                handleOperationFailure(operationEntity, result.errorMessage ?: "Unknown error")
            }
            
        } catch (e: Exception) {
            handleOperationFailure(operationEntity, e.message ?: "Exception occurred")
        } finally {
            processingOperations.remove(operationId)
        }
    }
    
    /**
     * Execute an operation based on its type
     */
    private suspend fun executeOperation(operation: OfflineOperation): SyncResult {
        return when (operation) {
            is OfflineOperation.CreateNote -> executeCreateNote(operation)
            is OfflineOperation.UpdateNote -> executeUpdateNote(operation)
            is OfflineOperation.DeleteNote -> executeDeleteNote(operation)
            is OfflineOperation.CreateCategory -> executeCreateCategory(operation)
            is OfflineOperation.AIAnalysis -> executeAIAnalysis(operation)
            is OfflineOperation.SyncCollaborativeSession -> executeSyncCollaborativeSession(operation)
        }
    }
    
    /**
     * Execute note creation operation
     */
    private suspend fun executeCreateNote(operation: OfflineOperation.CreateNote): SyncResult {
        return try {
            val note = parseNoteFromData(operation.noteData)
            if (note == null) {
                return createSyncResult(false, operation.id, "Invalid note data")
            }
            // Check for conflicts with existing notes
            val conflictCheck = checkForNoteConflicts(note)
            
            if (conflictCheck.hasConflicts) {
                // Handle conflict
                val resolution = conflictResolutionEngine.resolveCreateNoteConflict(
                    note,
                    conflictCheck.conflictingNotes
                )
                
                when (resolution.strategy) {
                    ConflictResolutionStrategy.AUTO_MERGE -> {
                        val mergedNote = resolution.resolvedNote!!
                        val noteId = noteRepository.insert(mergedNote)
                        createSyncResult(true, operation.id, conflicts = listOf(resolution.conflictData!!))
                    }
                    ConflictResolutionStrategy.USER_CHOICE -> {
                        // Queue for user resolution
                        queueConflictForUserResolution(operation, conflictCheck)
                        createSyncResult(false, operation.id, "Requires user conflict resolution")
                    }
                    else -> {
                        val noteId = noteRepository.insert(note)
                        createSyncResult(true, operation.id)
                    }
                }
            } else {
                // No conflicts, create note normally
                val noteId = noteRepository.insert(note)
                createSyncResult(true, operation.id)
            }
        } catch (e: Exception) {
            createSyncResult(false, operation.id, e.message)
        }
    }
    
    /**
     * Execute note update operation
     */
    private suspend fun executeUpdateNote(operation: OfflineOperation.UpdateNote): SyncResult {
        return try {
            // Get current note state
            val currentNote = noteRepository.getNoteById(operation.noteId)
            if (currentNote == null) {
                return createSyncResult(false, operation.id, "Note not found")
            }
            
            // Check for version conflicts
            if (currentNote.version > operation.previousVersion) {
                // Version conflict detected
                val conflictData = ConflictData(
                    conflictType = ConflictType.VERSION,
                    localVersion = serializeNoteChanges(operation.changes),
                    remoteVersion = serializeNote(currentNote),
                    suggestedResolution = ConflictResolutionStrategy.THREE_WAY_MERGE
                )
                
                val resolution = conflictResolutionEngine.resolveUpdateConflict(
                    currentNote,
                    operation.changes,
                    conflictData
                )
                
                when (resolution.strategy) {
                    ConflictResolutionStrategy.AUTO_MERGE -> {
                        val updatedNote = applyChangesToNote(currentNote, resolution.resolvedChanges!!)
                        noteRepository.update(updatedNote)
                        createSyncResult(true, operation.id, conflicts = listOf(conflictData))
                    }
                    else -> {
                        // Queue for user resolution
                        queueUpdateConflictForUserResolution(operation, currentNote, conflictData)
                        createSyncResult(false, operation.id, "Requires user conflict resolution")
                    }
                }
            } else {
                // No conflict, apply changes
                val updatedNote = applyChangesToNote(currentNote, operation.changes)
                noteRepository.update(updatedNote)
                createSyncResult(true, operation.id)
            }
        } catch (e: Exception) {
            createSyncResult(false, operation.id, e.message)
        }
    }
    
    /**
     * Execute note deletion operation
     */
    private suspend fun executeDeleteNote(operation: OfflineOperation.DeleteNote): SyncResult {
        return try {
            if (operation.softDelete) {
                // Soft delete - mark as deleted
                val note = noteRepository.getNoteById(operation.noteId)
                if (note != null) {
                    val deletedNote = note.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
                    noteRepository.update(deletedNote)
                }
            } else {
                // Hard delete
                deleteNoteById(operation.noteId)
            }
            createSyncResult(true, operation.id)
        } catch (e: Exception) {
            createSyncResult(false, operation.id, e.message)
        }
    }
    
    /**
     * Execute category creation operation
     */
    private suspend fun executeCreateCategory(operation: OfflineOperation.CreateCategory): SyncResult {
        return try {
            // Implementation depends on category repository
            // For now, return success
            createSyncResult(true, operation.id)
        } catch (e: Exception) {
            createSyncResult(false, operation.id, e.message)
        }
    }
    
    /**
     * Execute AI analysis operation
     */
    private suspend fun executeAIAnalysis(operation: OfflineOperation.AIAnalysis): SyncResult {
        return try {
            // Implementation depends on AI service integration
            // For now, return success
            createSyncResult(true, operation.id)
        } catch (e: Exception) {
            createSyncResult(false, operation.id, e.message)
        }
    }
    
    /**
     * Execute collaborative session sync operation
     */
    private suspend fun executeSyncCollaborativeSession(operation: OfflineOperation.SyncCollaborativeSession): SyncResult {
        return try {
            // Implementation depends on collaborative service integration
            // For now, return success
            createSyncResult(true, operation.id)
        } catch (e: Exception) {
            createSyncResult(false, operation.id, e.message)
        }
    }
    
    /**
     * Handle operation failure with retry logic
     */
    private suspend fun handleOperationFailure(operation: OfflineOperationEntity, errorMessage: String) {
        val newRetryCount = operation.retryCount + 1
        
        if (newRetryCount < operation.maxRetries) {
            // Schedule for retry with exponential backoff
            val backoffDelay = calculateBackoffDelay(newRetryCount)
            val scheduledAt = System.currentTimeMillis() + backoffDelay
            
            val updatedOperation = operation.copy(
                status = "PENDING",
                retryCount = newRetryCount,
                lastAttemptAt = System.currentTimeMillis(),
                scheduledAt = scheduledAt,
                errorMessage = errorMessage
            )
            
            offlineOperationDao.update(updatedOperation)
            updateOperationStatus(operation.id, OperationStatus.RETRYING)
        } else {
            // Max retries reached, mark as failed
            markOperationFailed(operation.id, errorMessage)
        }
    }
    
    /**
     * Process retry operations
     */
    private suspend fun processRetryOperations(currentTime: Long) {
        val retryAfterTime = currentTime - 60000 // Don't retry more than once per minute
        val retryOperations = offlineOperationDao.getOperationsForRetry(retryAfterTime, 5)
        
        retryOperations.forEach { operation ->
            if (!processingOperations.contains(operation.id)) {
                processOperation(operation)
            }
        }
    }
    
    /**
     * Check if operation dependencies are satisfied
     */
    private suspend fun areDependenciesSatisfied(operation: OfflineOperationEntity): Boolean {
        if (operation.dependencies.isEmpty()) return true
        
        val dependencyIds = operation.dependencies
        
        for (dependencyId in dependencyIds) {
            val dependency = offlineOperationDao.getById(dependencyId)
            if (dependency?.status != "SUCCESS") {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Check if operation can be executed based on network requirements
     */
    private suspend fun canExecuteOperation(operation: OfflineOperation): Boolean {
        val networkState = networkStateManager.getCurrentNetworkState()
        
        return when (operation.networkRequirement) {
            NetworkRequirement.ANY -> true
            NetworkRequirement.WIFI_ONLY -> networkState.isConnected && networkState.connectionType == ConnectionType.WIFI
            NetworkRequirement.MOBILE_DATA_OK -> networkState.isConnected
        }
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        val baseDelay = 1000L // 1 second
        val maxDelay = 300000L // 5 minutes
        val delay = (baseDelay * 2.0.pow(retryCount.toDouble())).toLong()
        return min(delay, maxDelay)
    }
    
    /**
     * Update operation status
     */
    private fun updateOperationStatus(operationId: String, status: OperationStatus) {
        val currentStatuses = _operationStatus.value.toMutableMap()
        currentStatuses[operationId] = status
        _operationStatus.value = currentStatuses
    }
    
    /**
     * Mark operation as failed
     */
    private suspend fun markOperationFailed(operationId: String, errorMessage: String) {
        offlineOperationDao.updateOperationResult(operationId, "FAILED", errorMessage)
        updateOperationStatus(operationId, OperationStatus.FAILED)
    }
    
    /**
     * Trigger operation processing
     */
    private fun triggerOperationProcessing() {
        scope.launch {
            processOperations()
        }
    }
    
    /**
     * Start statistics updating
     */
    private fun startStatisticsUpdating() {
        scope.launch {
            while (true) {
                try {
                    updateQueueStatistics()
                    delay(10000) // Update every 10 seconds
                } catch (e: Exception) {
                    delay(30000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Update queue statistics
     */
    private suspend fun updateQueueStatistics() {
        val pendingCount = offlineOperationDao.getPendingOperationCount()
        val failedCount = offlineOperationDao.getFailedOperationCount()
        val pendingSize = offlineOperationDao.getPendingDataSize() ?: 0L
        val conflictCount = syncStateDao.getConflictCount()
        
        val statistics = QueueStatistics(
            pendingOperations = pendingCount,
            failedOperations = failedCount,
            pendingDataSize = pendingSize,
            conflictsToResolve = conflictCount,
            lastUpdated = System.currentTimeMillis()
        )
        
        _queueStatistics.value = statistics
    }
    
    /**
     * Schedule periodic cleanup
     */
    private fun schedulePeriodicCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<OfflineCleanupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "offline_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
    
    // Helper functions for conflict resolution and data manipulation
    private suspend fun checkForNoteConflicts(note: NoteEntity): ConflictCheckResult {
        return try {
            // Check for notes with similar titles or content
            val similarNotes = noteRepository.searchNotes(note.title).first()
                .filter { it.id != note.id }
                .filter { existingNote ->
                    val titleSimilarity = calculateSimilarity(note.title, existingNote.title)
                    val contentSimilarity = calculateSimilarity(note.content, existingNote.content)
                    titleSimilarity > 0.8f || contentSimilarity > 0.7f
                }
            
            ConflictCheckResult(similarNotes.isNotEmpty(), similarNotes)
        } catch (e: Exception) {
            ConflictCheckResult(false, emptyList())
        }
    }
    
    private fun calculateSimilarity(str1: String, str2: String): Float {
        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f
        
        val words1 = str1.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        val words2 = str2.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) intersection.toFloat() / union.toFloat() else 0.0f
    }
    
    private suspend fun queueConflictForUserResolution(operation: OfflineOperation.CreateNote, conflictCheck: ConflictCheckResult) {
        val conflictData = ConflictData(
            conflictType = ConflictType.CONTENT,
            localVersion = operation.noteData,
            remoteVersion = serializeNote(conflictCheck.conflictingNotes.first())
        )
        
        conflictHistoryDao.insert(
            ConflictHistoryEntity(
                id = UUID.randomUUID().toString(),
                entityType = "note",
                entityId = operation.tempId,
                conflictType = "DATA_CONFLICT",
                conflictDetails = OfflineOperationSerializer.serializeConflictData(conflictData),
                localData = operation.noteData,
                remoteData = serializeNote(conflictCheck.conflictingNotes.first()),
                resolution = "USER_CHOICE",
                isResolved = false
            )
        )
    }
    
    private suspend fun queueUpdateConflictForUserResolution(operation: OfflineOperation.UpdateNote, currentNote: NoteEntity, conflictData: ConflictData) {
        conflictHistoryDao.insert(
            ConflictHistoryEntity(
                id = UUID.randomUUID().toString(),
                entityType = "note",
                entityId = operation.noteId.toString(),
                conflictType = "DATA_CONFLICT",
                conflictDetails = OfflineOperationSerializer.serializeConflictData(conflictData),
                localData = serializeNote(currentNote),
                resolution = "USER_CHOICE",
                isResolved = false
            )
        )
    }
    
    private fun applyChangesToNote(note: NoteEntity, changes: Map<String, String>): NoteEntity {
        // Implementation for applying changes to note
        return note.copy(
            title = changes["title"] ?: note.title,
            content = changes["content"] ?: note.content,
            category = changes["category"] ?: note.category,
            tags = changes["tags"] ?: note.tags,
            updatedAt = System.currentTimeMillis(),
            version = note.version + 1
        )
    }
    
    private fun serializeNote(note: NoteEntity): String {
        // Manual minimal JSON to avoid requiring kotlinx.serialization on NoteEntity
        fun esc(s: String): String = s.replace("\"", "\\\"")
        return "{" +
            "\"id\":${note.id}," +
            "\"title\":\"${esc(note.title)}\"," +
            "\"category\":\"${esc(note.category)}\"," +
            "\"version\":${note.version}" +
        "}"
    }
    
    private fun serializeNoteChanges(changes: Map<String, String>): String {
        return try {
            json.encodeToString(changes)
        } catch (e: Exception) {
            "{}"
        }
    }
    
    private suspend fun updateSyncStateAfterSuccess(operation: OfflineOperation, result: SyncResult) {
        try {
            when (operation) {
                is OfflineOperation.CreateNote -> {
                    val syncState = SyncStateEntity(
                        entityType = "note",
                        entityId = operation.tempId,
                        localVersion = 1,
                        remoteVersion = 1,
                        lastSyncedAt = System.currentTimeMillis(),
                        syncStatus = "SYNCED",
                        checksum = parseNoteFromData(operation.noteData)?.let { dataIntegrityManager.calculateNoteChecksum(it) } ?: ""
                    )
                    syncStateDao.insert(syncState)
                }
                is OfflineOperation.UpdateNote -> {
                    syncStateDao.updateLocalVersion(
                        "note",
                        operation.noteId.toString(),
                        (operation.previousVersion + 1)
                    )
                    syncStateDao.updateSyncStatus("note", operation.noteId.toString(), "SYNCED")
                }
                is OfflineOperation.DeleteNote -> {
                    syncStateDao.updateSyncStatus("note", operation.noteId.toString(), "SYNCED")
                }
                else -> {
                    // Handle other operation types
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }

    private fun parseNoteFromData(noteData: String): NoteEntity? {
        return try {
            Gson().fromJson(noteData, NoteEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createSyncResult(success: Boolean, operationId: String, errorMessage: String? = null, conflicts: List<ConflictData> = emptyList()): SyncResult {
        return SyncResult(
            success = success,
            operationId = operationId,
            syncedAt = System.currentTimeMillis(),
            conflicts = conflicts,
            errorMessage = errorMessage,
            dataTransferred = 0,
            syncDuration = 0
        )
    }
    
    /**
     * Helper methods for repository integration
     */
    suspend fun deleteNoteById(noteId: Long) {
        val note = noteRepository.getNoteById(noteId)
        if (note != null) {
            val deletedNote = note.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
            noteRepository.update(deletedNote)
        }
    }
    
    suspend fun createNoteWithOfflineSupport(note: NoteEntity): NoteEntity {
        return try {
            if (networkStateManager.networkState.value.isConnected) {
                val id = noteRepository.insert(note)
                note.copy(id = id)
            } else {
                queueOperation(OfflineOperation.CreateNote(
                    id = UUID.randomUUID().toString(),
                    noteData = Gson().toJson(note),
                    tempId = note.id.toString(),
                    timestamp = System.currentTimeMillis()
                ))
                note
            }
        } catch (e: Exception) {
            queueOperation(OfflineOperation.CreateNote(
                id = UUID.randomUUID().toString(),
                noteData = Gson().toJson(note),
                tempId = note.id.toString(),
                timestamp = System.currentTimeMillis()
            ))
            note
        }
    }
    
    suspend fun updateNoteWithOfflineSupport(note: NoteEntity): NoteEntity {
        return try {
            if (networkStateManager.networkState.value.isConnected) {
                noteRepository.update(note)
                note
            } else {
                queueOperation(OfflineOperation.UpdateNote(
                    id = UUID.randomUUID().toString(),
                    noteId = note.id,
                    changes = mapOf(
                        "title" to note.title,
                        "content" to note.content,
                        "category" to note.category,
                        "tags" to note.tags
                    ),
                    previousVersion = note.version.toLong(),
                    timestamp = System.currentTimeMillis()
                ))
                note.copy(version = note.version + 1, updatedAt = System.currentTimeMillis())
            }
        } catch (e: Exception) {
            queueOperation(OfflineOperation.UpdateNote(
                id = UUID.randomUUID().toString(),
                noteId = note.id,
                changes = mapOf(
                    "title" to note.title,
                    "content" to note.content,
                    "category" to note.category,
                    "tags" to note.tags
                ),
                previousVersion = note.version.toLong(),
                timestamp = System.currentTimeMillis()
            ))
            note.copy(version = note.version + 1, updatedAt = System.currentTimeMillis())
        }
    }
    
    suspend fun deleteNoteWithOfflineSupport(noteId: Long) {
        try {
            if (networkStateManager.networkState.value.isConnected) {
                deleteNoteById(noteId)
            } else {
                queueOperation(OfflineOperation.DeleteNote(
                    id = UUID.randomUUID().toString(),
                    noteId = noteId,
                    timestamp = System.currentTimeMillis()
                ))
            }
        } catch (e: Exception) {
            queueOperation(OfflineOperation.DeleteNote(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                timestamp = System.currentTimeMillis()
            ))
        }
    }
    
    suspend fun getSyncStatusForNote(noteId: Long): SyncStateEntity? {
        return syncStateDao.getSyncState("note", noteId.toString())
    }
    
    suspend fun getPendingOperationsForNote(noteId: Long): List<OfflineOperationEntity> {
        return offlineOperationDao.getPendingOperationsByEntityId(noteId.toString())
    }
    
    suspend fun getConflictsForNote(noteId: Long): List<ConflictHistoryEntity> {
        return conflictHistoryDao.getUnresolvedConflictsByEntityId(noteId.toString())
    }
}

// Data classes for internal use
data class ConflictCheckResult(
    val hasConflicts: Boolean,
    val conflictingNotes: List<NoteEntity>
)

data class QueueStatistics(
    val pendingOperations: Int = 0,
    val failedOperations: Int = 0,
    val processingOperations: Int = 0,
    val pendingDataSize: Long = 0,
    val conflictsToResolve: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

// SyncResult is defined in OfflineOperationModels.kt
