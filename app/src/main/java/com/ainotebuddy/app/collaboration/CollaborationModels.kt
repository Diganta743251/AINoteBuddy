package com.ainotebuddy.app.collaboration

import kotlinx.serialization.Serializable

/**
 * Core data models for real-time collaboration
 */

/**
 * Represents a collaborative session for a note
 */
@Serializable
data class CollaborativeSession(
    val sessionId: String,
    val noteId: String,
    val ownerId: String,
    val participants: List<CollaborativeUser>,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val permissions: SessionPermissions = SessionPermissions()
)

/**
 * Represents a user in a collaborative context
 */
@Serializable
data class CollaborativeUser(
    val userId: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val color: String, // Unique color for this user's cursors/highlights
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val currentPosition: CursorSelection? = null,
    val permissions: UserPermissions = UserPermissions()
)

/**
 * Represents cursor/selection position in collaborative editing
 */
@Serializable
data class CursorSelection(
    val userId: String,
    val startIndex: Int,
    val endIndex: Int = startIndex,
    val timestamp: Long = System.currentTimeMillis(),
    val isSelection: Boolean = endIndex != startIndex
)

/**
 * Operational Transform operation for collaborative editing
 */
@Serializable
sealed class CollaborativeOperation {
    abstract val operationId: String
    abstract val userId: String
    abstract val timestamp: Long
    abstract val version: Int
    
    @Serializable
    data class Insert(
        override val operationId: String,
        override val userId: String,
        override val timestamp: Long,
        override val version: Int,
        val position: Int,
        val content: String,
        val attributes: Map<String, String> = emptyMap()
    ) : CollaborativeOperation()
    
    @Serializable
    data class Delete(
        override val operationId: String,
        override val userId: String,
        override val timestamp: Long,
        override val version: Int,
        val position: Int,
        val length: Int
    ) : CollaborativeOperation()
    
    @Serializable
    data class Retain(
        override val operationId: String,
        override val userId: String,
        override val timestamp: Long,
        override val version: Int,
        val length: Int,
        val attributes: Map<String, String> = emptyMap()
    ) : CollaborativeOperation()
    
    @Serializable
    data class Format(
        override val operationId: String,
        override val userId: String,
        override val timestamp: Long,
        override val version: Int,
        val startPosition: Int,
        val endPosition: Int,
        val formatType: String,
        val formatValue: String
    ) : CollaborativeOperation()
}

/**
 * Represents a collaborative change to a note
 */
@Serializable
data class CollaborativeChange(
    val changeId: String,
    val noteId: String,
    val sessionId: String,
    val userId: String,
    val operations: List<CollaborativeOperation>,
    val timestamp: Long = System.currentTimeMillis(),
    val version: Int,
    val previousVersion: Int,
    val changeType: ChangeType = ChangeType.CONTENT_EDIT,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of collaborative changes
 */
enum class ChangeType {
    CONTENT_EDIT,
    TITLE_CHANGE,
    FORMAT_CHANGE,
    METADATA_UPDATE,
    PERMISSION_CHANGE,
    STRUCTURE_CHANGE
}

/**
 * Session-level permissions
 */
@Serializable
data class SessionPermissions(
    val allowNewParticipants: Boolean = true,
    val requireApprovalForJoin: Boolean = false,
    val allowGuestAccess: Boolean = false,
    val maxParticipants: Int = 10,
    val sessionTimeout: Long = 24 * 60 * 60 * 1000 // 24 hours
)

/**
 * User-level permissions within a session
 */
@Serializable
data class UserPermissions(
    val canEdit: Boolean = true,
    val canComment: Boolean = true,
    val canShare: Boolean = false,
    val canManagePermissions: Boolean = false,
    val canViewHistory: Boolean = true,
    val canExport: Boolean = true,
    val accessLevel: AccessLevel = AccessLevel.EDITOR
)

/**
 * Access levels for collaborative users
 */
enum class AccessLevel {
    OWNER,      // Full control
    ADMIN,      // Can manage permissions and users
    EDITOR,     // Can edit content
    COMMENTER,  // Can only comment
    VIEWER      // Read-only access
}

/**
 * Real-time presence information
 */
@Serializable
data class PresenceInfo(
    val sessionId: String,
    val userId: String,
    val isActive: Boolean,
    val lastActivity: Long = System.currentTimeMillis(),
    val currentSection: String? = null, // Which part of the note they're viewing
    val isTyping: Boolean = false,
    val cursorPosition: CursorSelection? = null,
    val selectedText: String? = null
)

/**
 * Conflict resolution information
 */
@Serializable
data class ConflictResolution(
    val conflictId: String,
    val noteId: String,
    val conflictingChanges: List<CollaborativeChange>,
    val resolutionStrategy: ResolutionStrategy,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val finalVersion: Int? = null
)

/**
 * Strategies for resolving conflicts
 */
enum class ResolutionStrategy {
    LAST_WRITER_WINS,
    OPERATIONAL_TRANSFORM,
    MANUAL_RESOLUTION,
    AI_ASSISTED_MERGE,
    VERSION_FORK
}

/**
 * Comment system for collaborative feedback
 */
@Serializable
data class CollaborativeComment(
    val commentId: String,
    val noteId: String,
    val sessionId: String,
    val userId: String,
    val content: String,
    val position: CommentPosition,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val replies: List<CommentReply> = emptyList(),
    val mentions: List<String> = emptyList() // User IDs mentioned in comment
)

/**
 * Position of a comment within the note
 */
@Serializable
data class CommentPosition(
    val startIndex: Int,
    val endIndex: Int,
    val selectedText: String,
    val contextBefore: String = "",
    val contextAfter: String = ""
)

/**
 * Reply to a collaborative comment
 */
@Serializable
data class CommentReply(
    val replyId: String,
    val userId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mentions: List<String> = emptyList()
)

/**
 * Sharing configuration for a note
 */
@Serializable
data class SharingConfig(
    val noteId: String,
    val shareId: String,
    val isPublic: Boolean = false,
    val allowedUsers: List<String> = emptyList(),
    val allowedDomains: List<String> = emptyList(),
    val expiresAt: Long? = null,
    val requiresPassword: Boolean = false,
    val passwordHash: String? = null,
    val defaultPermissions: UserPermissions = UserPermissions(canEdit = false),
    val trackingEnabled: Boolean = true,
    val downloadEnabled: Boolean = true
)

/**
 * Analytics for collaborative sessions
 */
@Serializable
data class CollaborationAnalytics(
    val sessionId: String,
    val noteId: String,
    val totalParticipants: Int,
    val totalEdits: Int,
    val totalComments: Int,
    val sessionDuration: Long,
    val mostActiveUser: String,
    val editsByUser: Map<String, Int>,
    val commentsByUser: Map<String, Int>,
    val conflictsResolved: Int,
    val averageResponseTime: Long
)
