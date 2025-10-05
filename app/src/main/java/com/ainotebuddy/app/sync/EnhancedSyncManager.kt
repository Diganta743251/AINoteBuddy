package com.ainotebuddy.app.sync

import android.content.Context
import android.util.Log
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.offline.OfflineOperationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class SyncStatus(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val pendingOperations: Int = 0,
    val syncProgress: Float = 0f,
    val error: String? = null
)

data class ConflictResolution(
    val noteId: Long,
    val localVersion: NoteEntity,
    val serverVersion: NoteEntity,
    val resolvedVersion: NoteEntity? = null,
    val strategy: ConflictStrategy = ConflictStrategy.MANUAL
)

enum class ConflictStrategy {
    LOCAL_WINS,
    SERVER_WINS,
    MERGE,
    MANUAL
}

@Singleton
class EnhancedSyncManager @Inject constructor(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val preferencesManager: PreferencesManager,
    private val offlineOperationManager: OfflineOperationManager
) {
    private val tag = "EnhancedSyncManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val activeConflicts = ConcurrentHashMap<Long, ConflictResolution>()
    private val _conflicts = MutableStateFlow<List<ConflictResolution>>(emptyList())
    val conflicts: StateFlow<List<ConflictResolution>> = _conflicts.asStateFlow()
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    
    // Auto-sync configuration
    private val autoSyncInterval = 30_000L // 30 seconds
    private var autoSyncJob: Job? = null
    
    init {
        startAutoSync()
        observeNetworkChanges()
    }
    
    fun startSync(force: Boolean = false) {
        if (_syncStatus.value.isSyncing && !force) {
            Log.d(tag, "Sync already in progress")
            return
        }
        
        syncJob?.cancel()
        syncJob = syncScope.launch {
            performSync()
        }
    }
    
    private suspend fun uploadNoteToFirestore(note: NoteEntity) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        
        val noteData = mapOf(
            "id" to note.id,
            "title" to note.title,
            "content" to note.content,
            "createdAt" to note.createdAt,
            "updatedAt" to note.updatedAt,
            "category" to note.category,
            "tags" to note.tags,
            "isPinned" to note.isPinned,
            "isFavorite" to note.isFavorite,
            "isArchived" to note.isArchived,
            "isDeleted" to note.isDeleted,
            "version" to note.version
        )
        
        firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(note.id.toString())
            .set(noteData)
            .await()
    }
    
    fun stopSync() {
        syncJob?.cancel()
        _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
    }
    
    private fun startAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = syncScope.launch {
            while (isActive) {
                delay(autoSyncInterval)
                if (!_syncStatus.value.isSyncing && 
                    auth.currentUser != null &&
                    _syncStatus.value.isOnline) {
                    performSync()
                }
            }
        }
    }
    
    private suspend fun performSync() {
        try {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                error = null,
                syncProgress = 0f
            )
            
            Log.d(tag, "Starting sync process")
            
            // Step 1: Process offline operations
            _syncStatus.value = _syncStatus.value.copy(syncProgress = 0.1f)
            processOfflineOperations()
            
            // Step 2: Upload local changes
            _syncStatus.value = _syncStatus.value.copy(syncProgress = 0.3f)
            uploadLocalChanges()
            
            // Step 3: Download server changes
            _syncStatus.value = _syncStatus.value.copy(syncProgress = 0.6f)
            downloadServerChanges()
            
            // Step 4: Resolve conflicts
            _syncStatus.value = _syncStatus.value.copy(syncProgress = 0.8f)
            resolveConflicts()
            
            // Step 5: Finalize
            _syncStatus.value = _syncStatus.value.copy(syncProgress = 1.0f)
            finalizeSyncProcess()
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                syncProgress = 0f
            )
            
            Log.d(tag, "Sync completed successfully")
            
        } catch (e: Exception) {
            Log.e(tag, "Sync failed", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message,
                syncProgress = 0f
            )
        }
    }
    
    private suspend fun finalizeSyncProcess() {
        // Clean up temporary data if any and log basic stats
        val totalNotes = try {
            noteRepository.getAllNotes().first().size
        } catch (_: Exception) { 0 }
        Log.d(tag, "Sync finalized. Total notes: $totalNotes")
    }
    
    private suspend fun processOfflineOperations() {
        try {
            val pendingOps = offlineOperationManager.getPendingOperations().first()
            _syncStatus.value = _syncStatus.value.copy(pendingOperations = pendingOps.size)
            // Delegate execution to the offline manager
            offlineOperationManager.forceSyncAll()
        } catch (e: Exception) {
            Log.e(tag, "Failed to process offline operations", e)
        }
    }
    
    private suspend fun uploadLocalChanges() {
        val localNotes = noteRepository.getAllNotes().first()
        for (note in localNotes) {
            try {
                if (note.isDeleted) {
                    deleteNoteFromFirestore(note.id.toString())
                } else {
                    uploadNoteToFirestore(note)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to upload note: ${note.id}", e)
                // Continue with other notes
            }
        }
    }
    
    private suspend fun downloadServerChanges() {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("notes")
                .get()
                .await()
            
            for (doc in snapshot.documents) {
                val serverNote = doc.toNoteEntity()
                val localNote = noteRepository.getNoteById(serverNote.id)
                
                when {
                    localNote == null -> {
                        // New note from server
                        noteRepository.insertNote(serverNote)
                    }
                    localNote.updatedAt < serverNote.updatedAt -> {
                        // Server version is newer; update local copy
                        noteRepository.updateNote(serverNote)
                    }
                    // Local version is newer or same - no action needed
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to download server changes", e)
            throw e
        }
    }
    
    private suspend fun resolveConflicts() {
        val conflicts = _conflicts.value
        for (conflict in conflicts) {
            when (conflict.strategy) {
                ConflictStrategy.LOCAL_WINS -> {
                    // Upload local version and keep local
                    uploadNoteToFirestore(conflict.localVersion)
                    noteRepository.updateNote(conflict.localVersion)
                    removeConflict(conflict.noteId)
                }
                ConflictStrategy.SERVER_WINS -> {
                    // Accept server version
                    noteRepository.updateNote(conflict.serverVersion)
                    removeConflict(conflict.noteId)
                }
                ConflictStrategy.MERGE -> {
                    conflict.resolvedVersion?.let { resolved ->
                        uploadNoteToFirestore(resolved)
                        noteRepository.updateNote(resolved)
                        removeConflict(conflict.noteId)
                    }
                }
                ConflictStrategy.MANUAL -> {
                    // Keep conflict for manual resolution
                    continue
                }
            }
        }
    }

    private suspend fun deleteNoteFromFirestore(noteId: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)
            .delete()
            .await()
    }
    
    private fun addConflict(localNote: NoteEntity, serverNote: NoteEntity) {
        val conflict = ConflictResolution(
            noteId = localNote.id,
            localVersion = localNote,
            serverVersion = serverNote
        )
        
        activeConflicts[localNote.id] = conflict
        _conflicts.value = activeConflicts.values.toList()
    }
    
    private fun removeConflict(noteId: Long) {
        activeConflicts.remove(noteId)
        _conflicts.value = activeConflicts.values.toList()
    }
    
    fun resolveConflict(noteId: Long, strategy: ConflictStrategy, resolvedNote: NoteEntity? = null) {
        activeConflicts[noteId]?.let { conflict ->
            activeConflicts[noteId] = conflict.copy(
                strategy = strategy,
                resolvedVersion = resolvedNote
            )
            _conflicts.value = activeConflicts.values.toList()
        }
    }
    
    private fun observeNetworkChanges() {
        // Implementation would depend on network monitoring library
        // For now, assume online
        _syncStatus.value = _syncStatus.value.copy(isOnline = true)
    }
    
    fun cleanup() {
        autoSyncJob?.cancel()
        syncJob?.cancel()
        syncScope.cancel()
    }
}

// Extension function to convert Firestore document to NoteEntity
private fun com.google.firebase.firestore.DocumentSnapshot.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = getLong("id") ?: 0L,
        title = getString("title") ?: "",
        content = getString("content") ?: "",
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
        category = getString("category") ?: "General",
        tags = getString("tags") ?: "",
        isPinned = getBoolean("isPinned") ?: false,
        isFavorite = getBoolean("isFavorite") ?: false,
        isDeleted = getBoolean("isDeleted") ?: false
    )
}

// Extension function for Firestore tasks
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.exception != null) {
                cont.resumeWithException(task.exception!!)
            } else {
                cont.resume(task.result)
            }
        }
    }
}