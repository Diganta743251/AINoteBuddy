package com.ainotebuddy.app.collaboration

import com.ainotebuddy.app.collaboration.model.Comment
import com.ainotebuddy.app.collaboration.model.CommentStatus
import com.ainotebuddy.app.collaboration.model.CommentThread
import com.ainotebuddy.app.collaboration.model.NewComment
import com.ainotebuddy.app.collaboration.model.CommentUpdate
import com.ainotebuddy.app.collaboration.model.SelectionRange
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages comments and comment threads in a collaborative document
 */
@Singleton
class CommentManager @Inject constructor(
    private val firebaseService: FirebaseCollaborationService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _comments = MutableStateFlow<Map<String, Comment>>(emptyMap())
    val comments: StateFlow<List<Comment>> = _comments.map { it.values.toList() }
        .stateIn(scope, SharingStarted.Lazily, emptyList())
    
    private val _commentThreads = MutableStateFlow<List<CommentThread>>(emptyList())
    val commentThreads: StateFlow<List<CommentThread>> = _commentThreads.asStateFlow()
    
    private var currentNoteId: String? = null
    private var commentSubscription: Job? = null
    
    /**
     * Start managing comments for a specific note
     */
    fun startForNote(noteId: String) {
        if (currentNoteId == noteId) return
        
        stop()
        currentNoteId = noteId
        
        commentSubscription = scope.launch {
            // In this codebase, comments are tied to collaboration sessions; use sessionId == noteId for now
            firebaseService.observeComments(noteId)
                .map { collabComments ->
                    collabComments.map { cc ->
                        // Map CollaborativeComment to UI Comment model
                        Comment(
                            id = cc.commentId,
                            noteId = cc.noteId,
                            authorId = cc.userId,
                            authorName = "", // display name not stored in CollaborativeComment
                            content = cc.content,
                            createdAt = cc.timestamp,
                            updatedAt = cc.resolvedAt ?: cc.timestamp,
                            status = if (cc.isResolved) CommentStatus.RESOLVED else CommentStatus.ACTIVE,
                            selection = cc.position.let { SelectionRange(it.startIndex, it.endIndex, it.selectedText) },
                            parentId = null,
                            mentions = cc.mentions
                        )
                    }
                }
                .catch { e -> 
                    e.printStackTrace()
                }
                .collect { commentList ->
                    _comments.value = commentList.associateBy { it.id }
                    updateCommentThreads()
                }
        }
    }
    
    /**
     * Create a new comment
     */
    suspend fun addComment(newComment: NewComment): Result<Comment> {
        val noteId = currentNoteId ?: return Result.failure(IllegalStateException("No active note"))
        
        return try {
            val collabComment = CollaborativeComment(
                commentId = UUID.randomUUID().toString(),
                noteId = noteId,
                sessionId = noteId,
                userId = firebaseService.getCurrentUserId(),
                content = newComment.content,
                position = CommentPosition(
                    startIndex = newComment.selection?.start ?: 0,
                    endIndex = newComment.selection?.end ?: 0,
                    selectedText = newComment.selection?.text ?: ""
                ),
                timestamp = System.currentTimeMillis(),
                isResolved = false,
                replies = emptyList(),
                mentions = newComment.mentions
            )
            
            // Persist comment (send via Firebase)
            firebaseService.sendComment(noteId, collabComment)
            Result.success(
                Comment(
                    id = collabComment.commentId,
                    noteId = noteId,
                    authorId = collabComment.userId,
                    authorName = firebaseService.getCurrentUserName(),
                    content = collabComment.content,
                    createdAt = collabComment.timestamp,
                    updatedAt = collabComment.timestamp,
                    status = CommentStatus.ACTIVE,
                    selection = newComment.selection,
                    parentId = newComment.parentId,
                    mentions = newComment.mentions
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing comment
     */
    suspend fun updateComment(update: CommentUpdate): Result<Comment> {
        val noteId = currentNoteId ?: return Result.failure(IllegalStateException("No active note"))
        
        return try {
            val currentComment = _comments.value[update.id] 
                ?: return Result.failure(NoSuchElementException("Comment not found"))
            
            val isResolving = update.status == CommentStatus.RESOLVED
            val updatedComment = currentComment.copy(
                content = update.content ?: currentComment.content,
                status = update.status ?: currentComment.status,
                updatedAt = System.currentTimeMillis(),
                resolvedAt = if (isResolving) currentComment.resolvedAt ?: System.currentTimeMillis() else null,
                resolvedBy = if (isResolving) firebaseService.getCurrentUserId() else null
            )
            
            // Persist via collaboration service
            val cc = CollaborativeComment(
                commentId = updatedComment.id,
                noteId = updatedComment.noteId,
                sessionId = updatedComment.noteId,
                userId = updatedComment.authorId,
                content = updatedComment.content,
                position = CommentPosition(
                    startIndex = updatedComment.selection?.start ?: 0,
                    endIndex = updatedComment.selection?.end ?: 0,
                    selectedText = updatedComment.selection?.text ?: ""
                ),
                timestamp = updatedComment.createdAt,
                isResolved = updatedComment.status == CommentStatus.RESOLVED,
                resolvedBy = if (updatedComment.status == CommentStatus.RESOLVED) firebaseService.getCurrentUserId() else null,
                resolvedAt = if (updatedComment.status == CommentStatus.RESOLVED) updatedComment.updatedAt else null,
                replies = emptyList(),
                mentions = updatedComment.mentions
            )
            firebaseService.sendComment(noteId, cc)
            Result.success(updatedComment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a comment
     */
    suspend fun deleteComment(commentId: String): Result<Unit> {
        val noteId = currentNoteId ?: return Result.failure(IllegalStateException("No active note"))
        
        return try {
            // No direct delete API; mark as deleted by sending updated comment with status
            val current = _comments.value[commentId] ?: return Result.failure(NoSuchElementException("Comment not found"))
            val updated = current.copy(status = CommentStatus.DELETED, updatedAt = System.currentTimeMillis())
            val cc = CollaborativeComment(
                commentId = updated.id,
                noteId = updated.noteId,
                sessionId = updated.noteId,
                userId = updated.authorId,
                content = updated.content,
                position = CommentPosition(
                    startIndex = updated.selection?.start ?: 0,
                    endIndex = updated.selection?.end ?: 0,
                    selectedText = updated.selection?.text ?: ""
                ),
                timestamp = updated.createdAt,
                isResolved = false,
                replies = emptyList(),
                mentions = updated.mentions
            )
            firebaseService.sendComment(updated.noteId, cc)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Resolve a comment thread
     */
    suspend fun resolveThread(threadId: String): Result<Unit> {
        val thread = _commentThreads.value.find { it.rootComment.id == threadId }
            ?: return Result.failure(NoSuchElementException("Thread not found"))
        
        // Update all comments in the thread to resolved
        val updateResults = (listOf(thread.rootComment) + thread.replies)
            .map { comment ->
                updateComment(
                    CommentUpdate(
                        id = comment.id,
                        status = CommentStatus.RESOLVED
                    )
                )
            }
        
        return if (updateResults.all { it.isSuccess }) {
            Result.success(Unit)
        } else {
            val errors = updateResults.filter { it.isFailure }
                .mapNotNull { it.exceptionOrNull()?.message }
                .joinToString(", ")
            Result.failure(RuntimeException("Failed to resolve thread: $errors"))
        }
    }
    
    /**
     * Get comments for a specific text selection
     */
    fun getCommentsForSelection(selection: SelectionRange): List<Comment> {
        return _comments.value.values.filter { comment ->
            comment.selection?.let { 
                it.start <= selection.start && it.end >= selection.end ||
                it.start >= selection.start && it.start <= selection.end ||
                it.end >= selection.start && it.end <= selection.end
            } ?: false
        }
    }
    
    /**
     * Get unread comments count for the current user
     */
    suspend fun getUnreadCount(): Int {
        val userId = firebaseService.getCurrentUserId()
        return _comments.value.values.count { 
            it.authorId != userId && 
            !it.mentions.contains(userId) &&
            it.status == CommentStatus.ACTIVE
        }
    }
    
    /**
     * Mark comments as read
     */
    suspend fun markAsRead(commentIds: List<String>) {
        // In a real app, you would track read status in the database
        // This is a simplified version
    }
    
    private fun updateCommentThreads() {
        val comments = _comments.value.values
            .filter { it.status != CommentStatus.DELETED }
            .sortedBy { it.createdAt }
        
        val rootComments = comments.filter { it.parentId == null }
        val replies = comments.filter { it.parentId != null }
            .groupBy { it.parentId!! }
        
        val threads = rootComments.map { comment ->
            val threadReplies = replies[comment.id] ?: emptyList()
            val isResolved = comment.status == CommentStatus.RESOLVED || 
                           (threadReplies.isNotEmpty() && threadReplies.all { it.status == CommentStatus.RESOLVED })
            
            val lastUpdated = (listOf(comment) + threadReplies)
                .maxOfOrNull { it.updatedAt } ?: comment.updatedAt
            
            CommentThread(
                rootComment = comment,
                replies = threadReplies,
                isResolved = isResolved,
                lastUpdated = lastUpdated
            )
        }.sortedByDescending { it.lastUpdated }
        
        _commentThreads.value = threads
    }
    
    /**
     * Stop managing comments and clean up resources
     */
    fun stop() {
        commentSubscription?.cancel()
        currentNoteId = null
        _comments.value = emptyMap()
        _commentThreads.value = emptyList()
    }
    
    /**
     * Clean up resources
     */
    fun dispose() {
        stop()
        scope.cancel()
    }
}
