package com.ainotebuddy.app.ui.components.collaboration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.collaboration.CommentManager
import com.ainotebuddy.app.collaboration.model.Comment
import com.ainotebuddy.app.collaboration.model.CommentStatus
import com.ainotebuddy.app.collaboration.model.CommentThread
import com.ainotebuddy.app.collaboration.model.NewComment
import com.ainotebuddy.app.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentThreadView(
    thread: CommentThread,
    commentManager: CommentManager,
    currentUserId: String,
    onReply: (Comment) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
            .padding(spacing.small)
    ) {
        // Thread header with toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = spacing.small)
        ) {
            // Toggle icon
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = if (isExpanded) "Collapse thread" else "Expand thread",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            // Thread info
            Text(
                text = "Thread • ${thread.replies.size + 1} ${if (thread.replies.size == 1) "reply" else "replies"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Resolved indicator
            if (thread.isResolved) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = spacing.small, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Resolved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Thread content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                // Root comment
                CommentItem(
                    comment = thread.rootComment,
                    isCurrentUser = thread.rootComment.authorId == currentUserId,
                    onReply = { onReply(thread.rootComment) },
                    onEdit = { /* Handle edit */ },
                    onDelete = { /* Handle delete */ },
                    onResolve = { /* Handle resolve */ },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Reply button
                if (!thread.isResolved) {
                    TextButton(
                        onClick = { showReplyInput = !showReplyInput },
                        modifier = Modifier.padding(start = spacing.medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text("Reply")
                    }
                }
                
                // Reply input
                if (showReplyInput) {
                    var replyText by remember { mutableStateOf("") }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = spacing.medium, top = spacing.small)
                    ) {
                        CommentInputField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            onSend = {
                                if (replyText.isNotBlank()) {
                                    scope.launch {
                                        commentManager.addComment(
                                            NewComment(
                                                content = replyText,
                                                parentId = thread.rootComment.id
                                            )
                                        )
                                        replyText = ""
                                        showReplyInput = false
                                    }
                                }
                            },
                            placeholder = "Write a reply...",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { showReplyInput = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
                
                // Replies
                if (thread.replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    
                    // Vertical line to connect replies
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = spacing.large)
                    ) {
                        // Vertical line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                .padding(vertical = spacing.small)
                        )
                        
                        // Replies list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = spacing.large)
                        ) {
                            thread.replies.forEach { reply ->
                                CommentItem(
                                    comment = reply,
                                    isCurrentUser = reply.authorId == currentUserId,
                                    onReply = { onReply(reply) },
                                    onEdit = { /* Handle edit */ },
                                    onDelete = { /* Handle delete */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = spacing.small)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentThreadsList(
    threads: List<CommentThread>,
    commentManager: CommentManager,
    currentUserId: String,
    modifier: Modifier = Modifier,
    onThreadClick: (CommentThread) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(threads, key = { it.rootComment.id }) { thread ->
            CommentThreadView(
                thread = thread,
                commentManager = commentManager,
                currentUserId = currentUserId,
                onReply = { /* Handle reply */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThreadClick(thread) }
            )
        }
    }
}

@Composable
fun CommentSidePanel(
    commentManager: CommentManager,
    currentUserId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val threads by commentManager.commentThreads.collectAsState()
    var newCommentText by remember { mutableStateOf("") }
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = modifier.fillMaxHeight(),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.medium)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close comments"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            // Comment threads list
            if (threads.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(
                        text = "No comments yet\nAdd a comment to start a discussion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                CommentThreadsList(
                    threads = threads,
                    commentManager = commentManager,
                    currentUserId = currentUserId,
                    modifier = Modifier.weight(1f),
                    onThreadClick = { /* Handle thread click */ }
                )
            }
            
            // New comment input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.medium)
            ) {
                CommentInputField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    onSend = {
                        if (newCommentText.isNotBlank()) {
                            scope.launch {
                                commentManager.addComment(
                                    NewComment(content = newCommentText)
                                )
                                newCommentText = ""
                            }
                        }
                    },
                    placeholder = "Add a comment...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
