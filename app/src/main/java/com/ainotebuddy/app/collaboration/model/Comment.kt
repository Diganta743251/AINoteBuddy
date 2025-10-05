package com.ainotebuddy.app.collaboration.model

import java.util.*

/**
 * Represents a comment in a collaborative document
 */
data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: CommentStatus = CommentStatus.ACTIVE,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val selection: SelectionRange? = null,
    val parentId: String? = null, // For threaded comments
    val mentions: List<String> = emptyList()
)

/**
 * Status of a comment
 */
enum class CommentStatus {
    ACTIVE,
    RESOLVED,
    DELETED
}

/**
 * Represents a range of selected text that a comment refers to
 */
data class SelectionRange(
    val start: Int,
    val end: Int,
    val text: String = ""
)

/**
 * Data class for creating a new comment
 */
data class NewComment(
    val content: String,
    val selection: SelectionRange? = null,
    val parentId: String? = null,
    val mentions: List<String> = emptyList()
)

/**
 * Data class for updating an existing comment
 */
data class CommentUpdate(
    val id: String,
    val content: String? = null,
    val status: CommentStatus? = null
)

/**
 * Represents a user mention in a comment
 */
data class Mention(
    val userId: String,
    val displayName: String,
    val start: Int,
    val end: Int
)

/**
 * Represents a comment thread (a comment and its replies)
 */
data class CommentThread(
    val rootComment: Comment,
    val replies: List<Comment> = emptyList(),
    val isResolved: Boolean = false,
    val lastUpdated: Long = rootComment.updatedAt
)
