package com.ainotebuddy.app.collaboration.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.ainotebuddy.app.collaboration.*

/**
 * Live cursor indicator showing other users' cursor positions
 */
@Composable
fun LiveCursor(
    cursorPosition: CursorPosition,
    userColor: String,
    userName: String,
    modifier: Modifier = Modifier
) {
    val color = Color(android.graphics.Color.parseColor(userColor))
    
    Box(
        modifier = modifier
            .size(2.dp, 20.dp)
            .background(color)
    ) {
        // Cursor label
        Card(
            modifier = Modifier
                .offset(x = 4.dp, y = (-8).dp)
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = color),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = userName,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Selection highlight showing other users' text selections
 */
@Composable
fun LiveSelection(
    startPosition: Int,
    endPosition: Int,
    userColor: String,
    modifier: Modifier = Modifier
) {
    val color = Color(android.graphics.Color.parseColor(userColor))
    val selectionColor = color.copy(alpha = 0.3f)
    
    Box(
        modifier = modifier
            .background(selectionColor, RoundedCornerShape(2.dp))
            .animateContentSize()
    )
}

/**
 * Typing indicator showing who is currently typing
 */
@Composable
fun TypingIndicator(
    typingUsers: List<PresenceInfo>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = typingUsers.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated typing dots
                TypingDots()
                
                // Typing text
                Text(
                    text = getTypingText(typingUsers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Animated typing dots indicator
 */
@Composable
private fun TypingDots() {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            var isVisible by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 200L)
                while (true) {
                    isVisible = !isVisible
                    kotlinx.coroutines.delay(600L)
                }
            }
            
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isVisible) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    .animateContentSize()
            )
        }
    }
}

/**
 * User presence avatars showing active collaborators
 */
@Composable
fun PresenceAvatars(
    presenceList: List<PresenceInfo>,
    maxVisible: Int = 5,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {}
) {
    val activeUsers = presenceList.filter { it.isActive }.sortedBy { it.getPriority() }
    val visibleUsers = activeUsers.take(maxVisible)
    val remainingCount = (activeUsers.size - maxVisible).coerceAtLeast(0)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-8).dp)
    ) {
        visibleUsers.forEach { presence ->
            PresenceAvatar(
                presence = presence,
                onClick = { onUserClick(presence.userId) }
            )
        }
        
        if (remainingCount > 0) {
            OverflowAvatar(count = remainingCount)
        }
    }
}

/**
 * Individual presence avatar
 */
@Composable
private fun PresenceAvatar(
    presence: PresenceInfo,
    onClick: () -> Unit = {}
) {
    val color = Color(android.graphics.Color.parseColor(getUserColor(presence.userId)))
    val isTyping = presence.isTyping
    
    Box {
        Card(
            modifier = Modifier
                .size(32.dp)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = color),
            shape = CircleShape,
            border = if (isTyping) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getUserInitials(presence.userId),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Online indicator
        if (presence.isActive) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/**
 * Overflow avatar showing remaining user count
 */
@Composable
private fun OverflowAvatar(count: Int) {
    Card(
        modifier = Modifier.size(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+$count",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Collaboration status bar showing session info
 */
@Composable
fun CollaborationStatusBar(
    session: CollaborativeSession?,
    presenceList: List<PresenceInfo>,
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = session != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        session?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Collaboration",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Column {
                            Text(
                                text = "Collaborative Session",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${presenceList.count { it.isActive }} active users",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        
                        PresenceAvatars(
                            presenceList = presenceList,
                            maxVisible = 3
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onShareClick) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Conflict resolution dialog
 */
@Composable
fun ConflictResolutionDialog(
    conflict: CollaborativeChange?,
    onResolve: (ResolutionStrategy) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (conflict != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Conflict",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Editing Conflict")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Multiple users edited the same content. Choose how to resolve:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Details:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Changes: ${'$'}{conflict.operations.size} • Version ${'$'}{conflict.version}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onResolve(ResolutionStrategy.LAST_WRITER_WINS) }
                    ) {
                        Text("Accept Theirs")
                    }
                    
                    TextButton(
                        onClick = { onResolve(ResolutionStrategy.MANUAL_RESOLUTION) }
                    ) {
                        Text("Keep Mine")
                    }
                    
                    Button(
                        onClick = { onResolve(ResolutionStrategy.OPERATIONAL_TRANSFORM) }
                    ) {
                        Text("Merge")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Comments overlay for collaborative annotations
 */
@Composable
fun CommentsOverlay(
    comments: List<CollaborativeComment>,
    onCommentClick: (CollaborativeComment) -> Unit,
    modifier: Modifier = Modifier
) {
    comments.forEach { comment ->
        CommentBubble(
            comment = comment,
            onClick = { onCommentClick(comment) },
            modifier = modifier
        )
    }
}

/**
 * Individual comment bubble
 */
@Composable
private fun CommentBubble(
    comment: CollaborativeComment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userColor = Color(android.graphics.Color.parseColor(getUserColor(comment.userId)))
    
    Card(
        modifier = modifier
            .clickable { onClick() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = userColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, userColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(userColor)
            )
            
            Text(
                text = if (comment.replies.isNotEmpty()) 
                    "${comment.replies.size + 1} comments" 
                else "1 comment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            if (!comment.isResolved) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Unresolved",
                    modifier = Modifier.size(12.dp),
                    tint = userColor
                )
            }
        }
    }
}

// Helper functions

private fun getTypingText(typingUsers: List<PresenceInfo>): String {
    return when (typingUsers.size) {
        0 -> ""
        1 -> "${getUserDisplayName(typingUsers[0].userId)} is typing..."
        2 -> "${getUserDisplayName(typingUsers[0].userId)} and ${getUserDisplayName(typingUsers[1].userId)} are typing..."
        else -> "${getUserDisplayName(typingUsers[0].userId)} and ${typingUsers.size - 1} others are typing..."
    }
}

private fun getUserDisplayName(userId: String): String {
    // This would typically fetch from user cache or database
    return "User ${userId.take(8)}"
}

private fun getUserInitials(userId: String): String {
    // This would typically fetch from user cache or database
    return userId.take(2).uppercase()
}

private fun getUserColor(userId: String): String {
    val colors = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
        "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
    )
    return colors[userId.hashCode().rem(colors.size).let { if (it < 0) it + colors.size else it }]
}
