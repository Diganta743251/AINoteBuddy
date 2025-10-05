package com.ainotebuddy.app.collaboration

import android.content.Context
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-based real-time collaboration service
 * Handles real-time synchronization of collaborative editing operations
 */
@Singleton
class FirebaseCollaborationService @Inject constructor(
    private val context: Context,
    private val operationalTransformEngine: OperationalTransformEngine
) {
    
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
    fun getCurrentUserName(): String = auth.currentUser?.displayName ?: "Unknown User"
    private val json = Json { ignoreUnknownKeys = true }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Active sessions cache
    private val activeSessions = mutableMapOf<String, CollaborativeSession>()
    private val sessionListeners = mutableMapOf<String, ValueEventListener>()
    
    /**
     * Create a new collaborative session for a note
     */
    suspend fun createSession(
        noteId: String,
        permissions: SessionPermissions = SessionPermissions()
    ): Result<CollaborativeSession> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            val sessionId = generateSessionId(noteId)
            val session = CollaborativeSession(
                sessionId = sessionId,
                noteId = noteId,
                ownerId = currentUser.uid,
                participants = listOf(
                    CollaborativeUser(
                        userId = currentUser.uid,
                        displayName = currentUser.displayName ?: "Unknown User",
                        email = currentUser.email ?: "",
                        color = generateUserColor(currentUser.uid),
                        isOnline = true,
                        permissions = UserPermissions(
                            canEdit = true,
                            canComment = true,
                            canShare = true,
                            canManagePermissions = true,
                            accessLevel = AccessLevel.OWNER
                        )
                    )
                ),
                permissions = permissions
            )
            
            // Store session in Firebase
            val sessionRef = database.getReference("sessions").child(sessionId)
            sessionRef.setValueSuspending(session.toMap())
            
            // Cache locally
            activeSessions[sessionId] = session
            
            Result.success(session)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Join an existing collaborative session
     */
    suspend fun joinSession(
        sessionId: String,
        requestedPermissions: UserPermissions = UserPermissions()
    ): Result<CollaborativeSession> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            val sessionRef = database.getReference("sessions").child(sessionId)
            val sessionSnapshot = sessionRef.get()
            
            if (sessionSnapshot == null) {
                return@withContext Result.failure(Exception("Session not found"))
            }
            
            val session = (sessionSnapshot as DataSnapshot).toCollaborativeSession()
            
            // Check if user can join
            if (!canUserJoinSession(currentUser.uid, session)) {
                return@withContext Result.failure(Exception("Access denied"))
            }
            
            // Add user to participants
            val newParticipant = CollaborativeUser(
                userId = currentUser.uid,
                displayName = currentUser.displayName ?: "Unknown User",
                email = currentUser.email ?: "",
                color = generateUserColor(currentUser.uid),
                isOnline = true,
                permissions = requestedPermissions
            )
            
            val updatedParticipants = session.participants.toMutableList()
            val existingIndex = updatedParticipants.indexOfFirst { it.userId == currentUser.uid }
            
            if (existingIndex >= 0) {
                // Update existing participant
                updatedParticipants[existingIndex] = newParticipant
            } else {
                // Add new participant
                updatedParticipants.add(newParticipant)
            }
            
            val updatedSession = session.copy(
                participants = updatedParticipants,
                lastActivity = System.currentTimeMillis()
            )
            
            // Update session in Firebase
            sessionRef.setValueSuspending(updatedSession.toMap())
            
            // Cache locally
            activeSessions[sessionId] = updatedSession
            
            Result.success(updatedSession)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Leave a collaborative session
     */
    suspend fun leaveSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            val sessionRef = database.getReference("sessions").child(sessionId)
            val sessionSnapshot = sessionRef.get()
            
            if (sessionSnapshot != null) {
                val session = (sessionSnapshot as DataSnapshot).toCollaborativeSession()
                val updatedParticipants = session.participants.map { participant ->
                    if (participant.userId == currentUser.uid) {
                        participant.copy(isOnline = false, lastSeen = System.currentTimeMillis())
                    } else {
                        participant
                    }
                }
                
                val updatedSession = session.copy(participants = updatedParticipants)
                sessionRef.setValue(updatedSession.toMap())
            }
            
            // Remove from local cache
            activeSessions.remove(sessionId)
            
            // Remove listener
            sessionListeners[sessionId]?.let { listener ->
                sessionRef.removeEventListener(listener)
                sessionListeners.remove(sessionId)
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a collaborative operation to other participants
     */
    suspend fun sendOperation(
        sessionId: String,
        operation: CollaborativeOperation
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val operationRef = database.getReference("operations")
                .child(sessionId)
                .child(operation.operationId)
            
            operationRef.setValue(operation.toMap())
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Listen for collaborative operations in real-time
     */
    fun observeOperations(sessionId: String): Flow<CollaborativeOperation> = callbackFlow {
        val operationsRef = database.getReference("operations").child(sessionId)
        
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val operation = snapshot.toCollaborativeOperation()
                    trySend(operation)
                } catch (e: Exception) {
                    // Log error but don't close the flow
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Operations don't typically change after creation
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle operation removal if needed
            }
            
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Not relevant for operations
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        operationsRef.addChildEventListener(listener)
        
        awaitClose {
            operationsRef.removeEventListener(listener)
        }
    }
    
    /**
     * Update user presence information
     */
    suspend fun updatePresence(
        sessionId: String,
        presenceInfo: PresenceInfo
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val presenceRef = database.getReference("presence")
                .child(sessionId)
                .child(presenceInfo.userId)
            
            presenceRef.setValue(presenceInfo.toMap())
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe presence information for all users in a session
     */
    fun observePresence(sessionId: String): Flow<List<PresenceInfo>> = callbackFlow {
        val presenceRef = database.getReference("presence").child(sessionId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val presenceList = mutableListOf<PresenceInfo>()
                
                for (childSnapshot in snapshot.children) {
                    try {
                        val presence = childSnapshot.toPresenceInfo()
                        presenceList.add(presence)
                    } catch (e: Exception) {
                        // Log error but continue processing other presence info
                    }
                }
                
                trySend(presenceList)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        presenceRef.addValueEventListener(listener)
        
        awaitClose {
            presenceRef.removeEventListener(listener)
        }
    }
    
    /**
     * Send a collaborative comment
     */
    suspend fun sendComment(
        sessionId: String,
        comment: CollaborativeComment
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val commentRef = database.getReference("comments")
                .child(sessionId)
                .child(comment.commentId)
            
            commentRef.setValueSuspending(comment.toMap())
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe comments in real-time
     */
    fun observeComments(sessionId: String): Flow<List<CollaborativeComment>> = callbackFlow {
        val commentsRef = database.getReference("comments").child(sessionId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = mutableListOf<CollaborativeComment>()
                
                for (childSnapshot in snapshot.children) {
                    try {
                        val comment = childSnapshot.toCollaborativeComment()
                        comments.add(comment)
                    } catch (e: Exception) {
                        // Log error but continue processing other comments
                    }
                }
                
                trySend(comments.sortedBy { it.timestamp })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        commentsRef.addValueEventListener(listener)
        
        awaitClose {
            commentsRef.removeEventListener(listener)
        }
    }
    
    // Helper functions
    
    private fun generateSessionId(noteId: String): String {
        return "session_${noteId}_${System.currentTimeMillis()}"
    }
    
    private fun generateUserColor(userId: String): String {
        val colors = listOf(
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
        )
        return colors[userId.hashCode().rem(colors.size).let { if (it < 0) it + colors.size else it }]
    }
    
    private fun canUserJoinSession(userId: String, session: CollaborativeSession): Boolean {
        // Check if user is already a participant
        if (session.participants.any { it.userId == userId }) {
            return true
        }
        
        // Check session permissions
        if (!session.permissions.allowNewParticipants) {
            return false
        }
        
        // Check participant limit
        if (session.participants.size >= session.permissions.maxParticipants) {
            return false
        }
        
        return true
    }
    
    // Extension functions for Firebase data conversion
    
    private suspend fun DatabaseReference.setValueSuspending(value: Any): Unit = suspendCancellableCoroutine { cont ->
        setValue(value) { error, _ ->
            if (error != null) cont.resumeWithException(error.toException()) else cont.resume(Unit)
        }
    }
    
    private suspend fun DatabaseReference.get(): DataSnapshot {
        return suspendCancellableCoroutine { continuation ->
            get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
                }
            }
        }
    }
}

// Extension functions for data conversion
private fun CollaborativeSession.toMap(): Map<String, Any> {
    return mapOf(
        "sessionId" to sessionId,
        "noteId" to noteId,
        "ownerId" to ownerId,
        "participants" to participants.map { it.toMap() },
        "createdAt" to createdAt,
        "lastActivity" to lastActivity,
        "isActive" to isActive,
        "permissions" to permissions.toMap()
    )
}

private fun CollaborativeUser.toMap(): Map<String, Any> {
    return mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "email" to email,
        "avatarUrl" to (avatarUrl ?: ""),
        "color" to color,
        "isOnline" to isOnline,
        "lastSeen" to lastSeen,
        "currentPosition" to (currentPosition?.toMap() ?: emptyMap<String, Any>()),
        "permissions" to permissions.toMap()
    )
}

private fun SessionPermissions.toMap(): Map<String, Any> {
    return mapOf(
        "allowNewParticipants" to allowNewParticipants,
        "requireApprovalForJoin" to requireApprovalForJoin,
        "allowGuestAccess" to allowGuestAccess,
        "maxParticipants" to maxParticipants,
        "sessionTimeout" to sessionTimeout
    )
}

private fun UserPermissions.toMap(): Map<String, Any> {
    return mapOf(
        "canEdit" to canEdit,
        "canComment" to canComment,
        "canShare" to canShare,
        "canManagePermissions" to canManagePermissions,
        "canViewHistory" to canViewHistory,
        "canExport" to canExport,
        "accessLevel" to accessLevel.name
    )
}

private fun CursorSelection.toMap(): Map<String, Any> {
    return mapOf(
        "userId" to userId,
        "startIndex" to startIndex,
        "endIndex" to endIndex,
        "timestamp" to timestamp,
        "isSelection" to isSelection
    )
}

private fun CollaborativeOperation.toMap(): Map<String, Any> {
    return when (this) {
        is CollaborativeOperation.Insert -> mapOf(
            "type" to "insert",
            "operationId" to operationId,
            "userId" to userId,
            "timestamp" to timestamp,
            "version" to version,
            "position" to position,
            "content" to content,
            "attributes" to attributes
        )
        is CollaborativeOperation.Delete -> mapOf(
            "type" to "delete",
            "operationId" to operationId,
            "userId" to userId,
            "timestamp" to timestamp,
            "version" to version,
            "position" to position,
            "length" to length
        )
        is CollaborativeOperation.Retain -> mapOf(
            "type" to "retain",
            "operationId" to operationId,
            "userId" to userId,
            "timestamp" to timestamp,
            "version" to version,
            "length" to length,
            "attributes" to attributes
        )
        is CollaborativeOperation.Format -> mapOf(
            "type" to "format",
            "operationId" to operationId,
            "userId" to userId,
            "timestamp" to timestamp,
            "version" to version,
            "startPosition" to startPosition,
            "endPosition" to endPosition,
            "formatType" to formatType,
            "formatValue" to formatValue
        )
    }
}

private fun PresenceInfo.toMap(): Map<String, Any> {
    return mapOf(
        "sessionId" to sessionId,
        "userId" to userId,
        "isActive" to isActive,
        "lastActivity" to lastActivity,
        "currentSection" to (currentSection ?: ""),
        "isTyping" to isTyping,
        "cursorPosition" to (cursorPosition?.toMap() ?: emptyMap<String, Any>()),
        "selectedText" to (selectedText ?: "")
    )
}

private fun CollaborativeComment.toMap(): Map<String, Any> {
    return mapOf(
        "commentId" to commentId,
        "noteId" to noteId,
        "sessionId" to sessionId,
        "userId" to userId,
        "content" to content,
        "position" to position.toMap(),
        "timestamp" to timestamp,
        "isResolved" to isResolved,
        "resolvedBy" to (resolvedBy ?: ""),
        "resolvedAt" to (resolvedAt ?: 0),
        "replies" to replies.map { it.toMap() },
        "mentions" to mentions
    )
}

private fun CommentPosition.toMap(): Map<String, Any> {
    return mapOf(
        "startIndex" to startIndex,
        "endIndex" to endIndex,
        "selectedText" to selectedText,
        "contextBefore" to contextBefore,
        "contextAfter" to contextAfter
    )
}

private fun CommentReply.toMap(): Map<String, Any> {
    return mapOf(
        "replyId" to replyId,
        "userId" to userId,
        "content" to content,
        "timestamp" to timestamp,
        "mentions" to mentions
    )
}

// Extension functions for Firebase data parsing
private fun DataSnapshot.toCollaborativeSession(): CollaborativeSession {
    // Implementation would parse the DataSnapshot back to CollaborativeSession
    // For brevity, this is simplified
    throw NotImplementedError("DataSnapshot parsing not implemented in this example")
}

private fun DataSnapshot.toCollaborativeOperation(): CollaborativeOperation {
    // Implementation would parse the DataSnapshot back to CollaborativeOperation
    throw NotImplementedError("DataSnapshot parsing not implemented in this example")
}

private fun DataSnapshot.toPresenceInfo(): PresenceInfo {
    // Implementation would parse the DataSnapshot back to PresenceInfo
    throw NotImplementedError("DataSnapshot parsing not implemented in this example")
}

private fun DataSnapshot.toCollaborativeComment(): CollaborativeComment {
    // Implementation would parse the DataSnapshot back to CollaborativeComment
    throw NotImplementedError("DataSnapshot parsing not implemented in this example")
}
