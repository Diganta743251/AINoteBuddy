package com.ainotebuddy.app.collaboration

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the collaboration session including presence, cursor positions, and real-time updates.
 */
@Singleton
class CollaborationManager @Inject constructor(
    private val firebaseService: FirebaseCollaborationService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Current session state
    private var currentSessionId: String? = null
    private var currentUserId: String? = null
    
    // Presence manager for tracking user presence and awareness
    val presenceManager = PresenceManager(firebaseService)
    
    // Cursor positions of other users
    private val _cursorPositions = MutableStateFlow<Map<String, CursorPosition>>(emptyMap())
    val cursorPositions: StateFlow<Map<String, CursorPosition>> = _cursorPositions
    
    // Selection ranges of other users
    private val _selectionRanges = MutableStateFlow<Map<String, SelectionRange>>(emptyMap())
    val selectionRanges: StateFlow<Map<String, SelectionRange>> = _selectionRanges
    
    // Whether collaboration is active
    private val _isCollaborating = mutableStateOf(false)
    val isCollaborating: Boolean
        get() = _isCollaborating.value
    
    /**
     * Start a new collaboration session
     */
    fun startSession(noteId: String, userId: String) {
        currentSessionId = noteId
        currentUserId = userId
        _isCollaborating.value = true
        
        // Initialize presence tracking
        presenceManager.startPresenceTracking(
            sessionId = noteId,
            userId = userId
        )
        
        // Start observing cursor positions from other users
        startObservingCursorPositions()
        startObservingSelections()
    }
    
    /**
     * Stop the current collaboration session
     */
    fun stopSession() {
        currentSessionId?.let { sessionId ->
            presenceManager.stopPresenceTracking()
            firebaseService.stopObservingCursorPositions(sessionId)
            firebaseService.stopObservingSelections(sessionId)
            
            currentSessionId = null
            currentUserId = null
            _isCollaborating.value = false
            _cursorPositions.value = emptyMap()
            _selectionRanges.value = emptyMap()
        }
    }
    
    /**
     * Update the current user's cursor position
     */
    fun updateCursorPosition(position: CursorPosition) {
        currentSessionId?.let { sessionId ->
            scope.launch {
                firebaseService.updateCursorPosition(sessionId, currentUserId ?: return@launch, position)
                presenceManager.updateCursorPosition(position)
            }
        }
    }
    
    /**
     * Update the current user's selection range
     */
    fun updateSelection(range: SelectionRange) {
        currentSessionId?.let { sessionId ->
            scope.launch {
                firebaseService.updateSelection(sessionId, currentUserId ?: return@launch, range)
            }
        }
    }
    
    /**
     * Send a chat message to the collaboration session
     */
    fun sendChatMessage(message: String) {
        currentSessionId?.let { sessionId ->
            scope.launch {
                firebaseService.sendChatMessage(
                    sessionId = sessionId,
                    userId = currentUserId ?: return@launch,
                    message = message
                )
            }
        }
    }
    
    /**
     * Start observing cursor positions from other users
     */
    private fun startObservingCursorPositions() {
        currentSessionId?.let { sessionId ->
            scope.launch {
                firebaseService.observeCursorPositions(sessionId).collect { positions ->
                    // Filter out current user's cursor
                    _cursorPositions.value = positions.filterKeys { it != currentUserId }
                }
            }
        }
    }
    
    /**
     * Start observing selection ranges from other users
     */
    private fun startObservingSelections() {
        currentSessionId?.let { sessionId ->
            scope.launch {
                firebaseService.observeSelections(sessionId).collect { selections ->
                    // Filter out current user's selection
                    _selectionRanges.value = selections.filterKeys { it != currentUserId }
                }
            }
        }
    }
    
    /**
     * Invite a user to collaborate on the current note
     */
    fun inviteUser(email: String, role: CollaborationRole = CollaborationRole.EDITOR) {
        currentSessionId?.let { noteId ->
            scope.launch {
                firebaseService.inviteUserToNote(
                    noteId = noteId,
                    email = email,
                    role = role
                )
            }
        }
    }
    
    /**
     * Accept an invitation to collaborate on a note
     */
    fun acceptInvitation(invitationId: String) {
        scope.launch {
            firebaseService.acceptInvitation(invitationId)
        }
    }
    
    /**
     * Reject an invitation to collaborate on a note
     */
    fun rejectInvitation(invitationId: String) {
        scope.launch {
            firebaseService.rejectInvitation(invitationId)
        }
    }
    
    /**
     * Get the current user's role in the collaboration
     */
    suspend fun getUserRole(userId: String): CollaborationRole {
        return currentSessionId?.let { noteId ->
            firebaseService.getUserRole(noteId, userId)
        } ?: CollaborationRole.VIEWER
    }
    
    /**
     * Check if the current user has permission to edit the note
     */
    suspend fun canEdit(): Boolean {
        return currentUserId?.let { userId ->
            getUserRole(userId).canEdit()
        } ?: false
    }
    
    /**
     * Clean up resources when the collaboration manager is no longer needed
     */
    fun dispose() {
        stopSession()
        scope.cancel()
    }
}

/**
 * Represents a user's cursor position in the document
 */
data class CursorPosition(
    val offset: Int,
    val line: Int,
    val column: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a text selection range in the document
 */
data class SelectionRange(
    val start: Int,
    val end: Int,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a user's role in a collaboration session
 */
enum class CollaborationRole {
    OWNER,
    EDITOR,
    COMMENTATOR,
    VIEWER;
    
    fun canEdit(): Boolean = this == OWNER || this == EDITOR
    fun canComment(): Boolean = this != VIEWER
    
    companion object {
        fun fromString(value: String): CollaborationRole {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                VIEWER
            }
        }
    }
}
