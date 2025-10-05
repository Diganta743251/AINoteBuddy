package com.ainotebuddy.app.collaboration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity

import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel managing collaborative editing state and operations
 */
@HiltViewModel
class CollaborativeEditingViewModel @Inject constructor(
    private val noteRepository: com.ainotebuddy.app.repository.NoteRepository,
    private val firebaseService: FirebaseCollaborationService,
    private val presenceManager: PresenceManager,
    private val operationalTransformEngine: OperationalTransformEngine
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    
    // Current collaborative session
    private val _currentSession = MutableStateFlow<CollaborativeSession?>(null)
    val currentSession: StateFlow<CollaborativeSession?> = _currentSession.asStateFlow()
    
    // Current note being edited
    private val _currentNote = MutableStateFlow<NoteEntity?>(null)
    val currentNote: StateFlow<NoteEntity?> = _currentNote.asStateFlow()
    
    // Collaborative operations queue
    private val _pendingOperations = MutableStateFlow<List<CollaborativeOperation>>(emptyList())
    val pendingOperations: StateFlow<List<CollaborativeOperation>> = _pendingOperations.asStateFlow()
    
    // Conflict resolution state
    private val _conflictToResolve = MutableStateFlow<ConflictResolutionState?>(null)
    val conflictToResolve: StateFlow<ConflictResolutionState?> = _conflictToResolve.asStateFlow()
    
    // Version history state
    private val _versionHistory = MutableStateFlow<List<VersionedContent>>(emptyList())
    val versionHistory: StateFlow<List<VersionedContent>> = _versionHistory.asStateFlow()
    
    // Conflict resolution callbacks
    private var conflictResolutionCallback: ((ConflictResolution) -> Unit)? = null
    
    // Comments for the current note
    private val _comments = MutableStateFlow<List<CollaborativeComment>>(emptyList())
    val comments: StateFlow<List<CollaborativeComment>> = _comments.asStateFlow()
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Collaboration status
    val isCollaborating: StateFlow<Boolean> = currentSession.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val collaboratorCount: StateFlow<Int> = combine(
        currentSession,
        presenceManager.activeUsersCount
    ) { session, activeCount ->
        if (session != null) activeCount else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // Expose presence manager flows
    val presenceList = presenceManager.allPresence
    val typingUsers = presenceManager.typingUsers
    val activeCursorPositions = presenceManager.allPresence.map { presenceList ->
        presenceManager.getActiveCursorPositions()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private var operationsJob: Job? = null
    private var commentsJob: Job? = null
    
    /**
     * Start collaborative editing for a note
     */
    fun startCollaboration(noteId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Load the note
                val note = noteRepository.getNoteById(noteId)
                if (note == null) {
                    _errorMessage.value = "Note not found"
                    return@launch
                }
                
                _currentNote.value = note
                
                // Check if note already has a collaborative session
                val existingSessionId = note.shareId
                val session = if (existingSessionId != null) {
                    // Join existing session
                    firebaseService.joinSession(existingSessionId).getOrNull()
                } else {
                    // Create new session
                    val newSession = firebaseService.createSession(noteId).getOrNull()
                    if (newSession != null) {
                        // Update note with session ID
                        noteRepository.updateNote(note.copy(shareId = newSession.sessionId))
                    }
                    newSession
                }
                
                if (session != null) {
                    _currentSession.value = session
                    startCollaborativeFeatures(session)
                } else {
                    _errorMessage.value = "Failed to start collaboration"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Error starting collaboration: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Stop collaborative editing
     */
    fun stopCollaboration() {
        viewModelScope.launch {
            try {
                _currentSession.value?.let { session ->
                    firebaseService.leaveSession(session.sessionId)
                }
                
                presenceManager.stopPresenceTracking()
                operationsJob?.cancel()
                commentsJob?.cancel()
                
                _currentSession.value = null
                _currentNote.value = null
                _pendingOperations.value = emptyList()
                _comments.value = emptyList()
                _conflictToResolve.value = null
                
            } catch (e: Exception) {
                _errorMessage.value = "Error stopping collaboration: ${e.message}"
            }
        }
    }
    
    /**
     * Apply a text edit operation
     */
    fun applyTextEdit(
        position: Int,
        deletedText: String,
        insertedText: String
    ) {
        val currentUser = auth.currentUser ?: return
        val session = _currentSession.value ?: return
        
        viewModelScope.launch {
            try {
                // Create collaborative operations
                val operations = mutableListOf<CollaborativeOperation>()
                
                // Delete operation if text was deleted
                if (deletedText.isNotEmpty()) {
                    operations.add(
                        CollaborativeOperation.Delete(
                            operationId = java.util.UUID.randomUUID().toString(),
                            userId = currentUser.uid,
                            timestamp = System.currentTimeMillis(),
                            version = 0,
                            position = position,
                            length = deletedText.length
                        )
                    )
                }
                
                // Insert operation if text was inserted
                if (insertedText.isNotEmpty()) {
                    operations.add(
                        CollaborativeOperation.Insert(
                            operationId = java.util.UUID.randomUUID().toString(),
                            userId = currentUser.uid,
                            timestamp = System.currentTimeMillis(),
                            version = 0,
                            position = position,
                            content = insertedText
                        )
                    )
                }
                
                // Send operations to other collaborators
                operations.forEach { operation ->
                    // fire-and-forget
                    viewModelScope.launch {
                        firebaseService.sendOperation(session.sessionId, operation)
                    }
                }
                
                // Update local note immediately
                updateLocalNote(operations)
                
            } catch (e: Exception) {
                _errorMessage.value = "Error applying edit: ${e.message}"
            }
        }
    }
    
    /**
     * Update cursor position
     */
    fun updateCursorPosition(startIndex: Int, endIndex: Int = startIndex) {
        val currentUser = auth.currentUser ?: return
        
        val cursorPosition = CursorPosition(
            offset = startIndex,
            line = 0,
            column = startIndex,
            timestamp = System.currentTimeMillis()
        )
        
        presenceManager.updateCursorPosition(cursorPosition)
    }
    
    /**
     * Update typing status
     */
    fun updateTypingStatus(isTyping: Boolean) {
        presenceManager.updateTypingStatus(isTyping)
    }
    
    /**
     * Add a comment
     */
    fun addComment(
        content: String,
        startIndex: Int,
        endIndex: Int,
        selectedText: String
    ) {
        val currentUser = auth.currentUser ?: return
        val session = _currentSession.value ?: return
        val note = _currentNote.value ?: return
        
        viewModelScope.launch {
            try {
                val comment = CollaborativeComment(
                    commentId = java.util.UUID.randomUUID().toString(),
                    noteId = note.id.toString(),
                    sessionId = session.sessionId,
                    userId = currentUser.uid,
                    content = content,
                    position = CommentPosition(
                        startIndex = startIndex,
                        endIndex = endIndex,
                        selectedText = selectedText,
                        contextBefore = getContextBefore(startIndex),
                        contextAfter = getContextAfter(endIndex)
                    ),
                    timestamp = System.currentTimeMillis()
                )
                
                // fire-and-forget
                viewModelScope.launch {
                    firebaseService.sendComment(session.sessionId, comment)
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Error adding comment: ${e.message}"
            }
        }
    }
    
    /**
     * Check if an operation conflicts with the current state
     */
    private fun hasConflict(operation: CollaborativeOperation, currentVersion: Int): Boolean {
        // Simple version-based conflict detection
        return operation.version <= currentVersion
    }
    
    /**
     * Handle a detected conflict
     */
    private suspend fun handleConflict(operation: CollaborativeOperation, currentNote: NoteEntity) {
        val currentContent = currentNote.content
        val remoteContent = operationalTransformEngine.applyOperations(currentContent, listOf(operation))
        
        // Create conflict state
        val conflictState = ConflictResolutionState(
            conflictId = generateChangeId(),
            localContent = currentContent,
            remoteContent = remoteContent,
            remoteOperation = operation,
            remoteUserId = operation.userId,
            timestamp = System.currentTimeMillis()
        )
        
        // Try AI-assisted merge first
        if (shouldAttemptAutoMerge(conflictState)) {
            try {
                val mergedContent = attemptAIMerge(conflictState)
                if (mergedContent != null) {
                    // Auto-merge successful
                    val updatedNote = currentNote.copy(
                        content = mergedContent,
                        version = maxOf(currentNote.version, operation.version) + 1,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    updateVersionHistory(updatedNote, "auto-merge")
                    _currentNote.value = updatedNote
                    noteRepository.updateNote(updatedNote)
                    
                    // Notify user of successful auto-merge
                    _errorMessage.value = "Changes automatically merged successfully"
                    return
                }
            } catch (e: Exception) {
                // Fall through to manual resolution
                _errorMessage.value = "Auto-merge failed: ${e.message}"
            }
        }
        
        // Fall back to manual resolution
        _conflictToResolve.value = conflictState
    }
    
    /**
     * Determine if we should attempt an automatic merge
     */
    private fun shouldAttemptAutoMerge(conflict: ConflictResolutionState): Boolean {
        // Simple heuristic: auto-merge if changes are in different parts of the document
        val localChanges = getChangeRanges(conflict.localContent, conflict.remoteContent)
        val remoteChanges = getChangeRanges(conflict.remoteContent, conflict.localContent)
        
        // If changes don't overlap, we can auto-merge
        return !hasOverlappingChanges(localChanges, remoteChanges)
    }
    
    /**
     * Get the ranges of changes between two versions of content
     */
    private fun getChangeRanges(oldContent: String, newContent: String): List<IntRange> {
        // Simple diff implementation - in production, use a more sophisticated algorithm
        val changes = mutableListOf<IntRange>()
        var i = 0
        var j = 0
        
        while (i < oldContent.length && j < newContent.length) {
            if (oldContent[i] == newContent[j]) {
                i++
                j++
            } else {
                val start = i
                while (i < oldContent.length && j < newContent.length && 
                       oldContent[i] != newContent[j]) {
                    i++
                    j++
                }
                changes.add(start until i)
            }
        }
        
        // Handle remaining content
        if (i < oldContent.length) {
            changes.add(i until oldContent.length)
        }
        
        return changes
    }
    
    /**
     * Check if two sets of change ranges overlap
     */
    private fun hasOverlappingChanges(ranges1: List<IntRange>, ranges2: List<IntRange>): Boolean {
        for (range1 in ranges1) {
            for (range2 in ranges2) {
                if (range1.overlapsWith(range2)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Extension to check if two ranges overlap
     */
    private fun IntRange.overlapsWith(other: IntRange): Boolean {
        return !(this.last < other.first || this.first > other.last)
    }
    
    /**
     * Attempt to merge changes using AI
     */
    private suspend fun attemptAIMerge(conflict: ConflictResolutionState): String? {
        // In a real implementation, this would call an AI service to merge the changes
        // For now, we'll use a simple merge strategy
        val localChanges = getChangeRanges(conflict.localContent, conflict.remoteContent)
        val remoteChanges = getChangeRanges(conflict.remoteContent, conflict.localContent)
        
        if (localChanges.isEmpty()) return conflict.remoteContent
        if (remoteChanges.isEmpty()) return conflict.localContent
        
        // Simple merge: prefer local changes over remote for overlapping regions
        val merged = StringBuilder(conflict.localContent)
        
        // Apply non-conflicting remote changes
        remoteChanges.forEach { range ->
            if (range.first < conflict.remoteContent.length) {
                val end = minOf(range.last + 1, conflict.remoteContent.length)
                val remoteText = conflict.remoteContent.substring(range.first, end)
                val replaceEnd = minOf(range.last + 1, merged.length)
                if (range.first <= merged.length) {
                    merged.replace(range.first, replaceEnd, remoteText)
                }
            }
        }
        
        return merged.toString()
    }
    
    /**
     * Update version history with the current state
     */
    private fun updateVersionHistory(note: NoteEntity, authorId: String) {
        val version = VersionedContent(
            id = generateChangeId(),
            noteId = note.id,
            content = note.content,
            version = note.version,
            authorId = authorId,
            timestamp = System.currentTimeMillis(),
            isConflictResolution = false
        )
        
        _versionHistory.update { current ->
            (current + version).sortedByDescending { it.version }.take(50) // Keep last 50 versions
        }
    }
    
    /**
     * Resolve a conflict with the specified resolution
     */
    fun resolveConflict(resolution: ConflictResolution) {
        val conflict = _conflictToResolve.value ?: return
        
        viewModelScope.launch {
            try {
                val currentNote = _currentNote.value ?: return@launch
                
                when (resolution) {
                    is ConflictResolution.AcceptLocal -> {
                        // Keep local changes, update version to resolve conflict
                        val updatedNote = currentNote.copy(
                            version = maxOf(currentNote.version, conflict.remoteOperation.version) + 1,
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        updateVersionHistory(updatedNote, auth.currentUser?.uid ?: "local")
                        _currentNote.value = updatedNote
                        noteRepository.updateNote(updatedNote)
                    }
                    is ConflictResolution.AcceptRemote -> {
                        // Apply remote changes
                        val remoteContent = operationalTransformEngine.applyOperations(
                            conflict.localContent, 
                            listOf(conflict.remoteOperation)
                        )
                        
                        val updatedNote = currentNote.copy(
                            content = remoteContent,
                            version = conflict.remoteOperation.version + 1,
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        updateVersionHistory(updatedNote, conflict.remoteUserId)
                        _currentNote.value = updatedNote
                        noteRepository.updateNote(updatedNote)
                    }
                    is ConflictResolution.Merge -> {
                        // Apply merged content
                        val updatedNote = currentNote.copy(
                            content = resolution.mergedContent,
                            version = maxOf(currentNote.version, conflict.remoteOperation.version) + 1,
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        updateVersionHistory(updatedNote, "merged:${auth.currentUser?.uid ?: "local"}")
                        _currentNote.value = updatedNote
                        noteRepository.updateNote(updatedNote)
                    }
                }
                
                _conflictToResolve.value = null
                _errorMessage.value = "Conflict resolved successfully"
                
            } catch (e: Exception) {
                _errorMessage.value = "Error resolving conflict: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    // Private helper methods
    
    private fun startCollaborativeFeatures(session: CollaborativeSession) {
        val currentUser = auth.currentUser ?: return
        
        // Start presence tracking
        presenceManager.startPresenceTracking(session.sessionId, currentUser.uid)
        
        // Start listening for operations
        operationsJob = viewModelScope.launch {
            firebaseService.observeOperations(session.sessionId)
                .collect { operation ->
                    if (operation.userId != currentUser.uid) {
                        handleRemoteOperation(operation)
                    }
                }
        }
        
        // Start listening for comments
        commentsJob = viewModelScope.launch {
            firebaseService.observeComments(session.sessionId)
                .collect { commentsList ->
                    _comments.value = commentsList
                }
        }
    }
    
    private suspend fun handleRemoteOperation(operation: CollaborativeOperation) {
        try {
            val currentNote = _currentNote.value ?: return
            val currentContent = currentNote.content
            
            // Transform the operation against any pending local operations
            val transformedOperation = transformOperation(operation)
            
            // Check for potential conflicts
            if (hasConflict(operation, currentNote.version)) {
                handleConflict(operation, currentNote)
                return
            }
            
            // Apply the operation to the note content
            val newContent = operationalTransformEngine.applyOperation(currentContent, transformedOperation)
            
            // Update the note
            val updatedNote = currentNote.copy(
                content = newContent,
                version = operation.version,
                updatedAt = System.currentTimeMillis()
            )
            
            // Update version history
            updateVersionHistory(updatedNote, operation.userId)
            
            _currentNote.value = updatedNote
            noteRepository.updateNote(updatedNote)
            
        } catch (e: Exception) {
            _errorMessage.value = "Error handling remote operation: ${e.message}"
        }
    }
    
    private fun transformOperation(operation: CollaborativeOperation): CollaborativeOperation {
        // Transform the operation against pending local operations
        val pendingOps = _pendingOperations.value
        
        return pendingOps.fold(operation) { op, pendingOp ->
            operationalTransformEngine.transform(op, pendingOp).first
        }
    }
    
    private suspend fun updateLocalNote(operations: List<CollaborativeOperation>) {
        val currentNote = _currentNote.value ?: return
        var content = currentNote.content
        
        operations.forEach { operation ->
            content = operationalTransformEngine.applyOperations(content, listOf(operation))
        }
        
        val updatedNote = currentNote.copy(
            content = content,
            version = operations.maxOfOrNull { it.version } ?: currentNote.version
        )
        
        _currentNote.value = updatedNote
        noteRepository.updateNote(updatedNote)
    }
    
    private suspend fun applyRemoteChange(change: CollaborativeChange) {
        val currentNote = _currentNote.value ?: return
        
        // Apply changes by flattening operations; changeType at change-level isn’t granular enough
        val newContent = change.operations.fold(currentNote.content) { acc, op ->
            when (op) {
                is CollaborativeOperation.Insert -> {
                    val pos = op.position.coerceIn(0, acc.length)
                    acc.substring(0, pos) + op.content + acc.substring(pos)
                }
                is CollaborativeOperation.Delete -> {
                    val pos = op.position.coerceIn(0, acc.length)
                    val end = (pos + op.length).coerceAtMost(acc.length)
                    acc.substring(0, pos) + acc.substring(end)
                }
                else -> acc
            }
        }
        
        val updatedNote = currentNote.copy(
            content = newContent,
            version = change.version
        )
        
        _currentNote.value = updatedNote
        noteRepository.updateNote(updatedNote)
    }
    
    private suspend fun attemptMerge(change: CollaborativeChange) {
        // Simple merge strategy - could be enhanced with more sophisticated algorithms
        val currentNote = _currentNote.value ?: return
        val localContent = currentNote.content
        // Build a simple remote content representation from operations
        val remoteContentSnippet = buildString {
            change.operations.forEach { op ->
                when (op) {
                    is CollaborativeOperation.Insert -> append(op.content)
                    is CollaborativeOperation.Delete -> { /* skip, cannot reconstruct removed text */ }
                    is CollaborativeOperation.Retain -> { /* no content change */ }
                    is CollaborativeOperation.Format -> { /* style change, ignore for content */ }
                }
            }
        }
        
        // For now, just concatenate with a separator
        val mergedContent = "$localContent\n--- MERGED ---\n$remoteContentSnippet"
        
        val updatedNote = currentNote.copy(
            content = mergedContent,
            version = change.version
        )
        
        _currentNote.value = updatedNote
        noteRepository.updateNote(updatedNote)
    }
    
    private fun getContextBefore(position: Int): String {
        val content = _currentNote.value?.content ?: ""
        val start = (position - 50).coerceAtLeast(0)
        return content.substring(start, position)
    }
    
    private fun getContextAfter(position: Int): String {
        val content = _currentNote.value?.content ?: ""
        val end = (position + 50).coerceAtMost(content.length)
        return content.substring(position, end)
    }
    
    private fun getNextVersion(): Int {
        return (_currentNote.value?.version ?: 0) + 1
    }
    
    private fun generateOperationId(): String {
        return "op_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun generateCommentId(): String {
        return "comment_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun generateChangeId(): String {
        return "change_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCollaboration()
    }
    
    /**
     * State representing a conflict that needs resolution
     */
    data class ConflictResolutionState(
        val conflictId: String,
        val localContent: String,
        val remoteContent: String,
        val remoteOperation: CollaborativeOperation,
        val remoteUserId: String,
        val timestamp: Long,
        val autoMergeAttempted: Boolean = false,
        val autoMergeSucceeded: Boolean = false
    )
    
    /**
     * Represents a version of the note's content
     */
    data class VersionedContent(
        val id: String,
        val noteId: Long,
        val content: String,
        val version: Int,
        val authorId: String,
        val timestamp: Long,
        val isConflictResolution: Boolean
    )
    
    /**
     * Result of a conflict resolution
     */
    sealed class ConflictResolution {
        object AcceptLocal : ConflictResolution()
        object AcceptRemote : ConflictResolution()
        data class Merge(val mergedContent: String) : ConflictResolution()
    }
}
