package com.ainotebuddy.app.ui.components.collaboration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.focus.onFocusChanged
import com.ainotebuddy.app.collaboration.model.Comment
import com.ainotebuddy.app.collaboration.model.CommentStatus
import com.ainotebuddy.app.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentItem(
    comment: Comment,
    isCurrentUser: Boolean,
    onReply: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onResolve: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showActions by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = when (comment.status) {
                    CommentStatus.RESOLVED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    CommentStatus.DELETED -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                when (comment.status) {
                    CommentStatus.RESOLVED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    CommentStatus.DELETED -> MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Header with author and timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Author avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comment.authorName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(spacing.small))
                
                // Author name and time
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            .format(Date(comment.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action menu
                if (onReply != null || onEdit != null || onDelete != null || onResolve != null) {
                    Box {
                        IconButton(
                            onClick = { showActions = !showActions },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Dropdown menu
                        DropdownMenu(
                            expanded = showActions,
                            onDismissRequest = { showActions = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            onReply?.let { onReply ->
                                DropdownMenuItem(
                                    text = { Text("Reply") },
                                    onClick = {
                                        onReply()
                                        showActions = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Reply,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            
                            onEdit?.takeIf { isCurrentUser }?.let { onEdit ->
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        onEdit()
                                        showActions = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            
                            onResolve?.takeIf { comment.status != CommentStatus.RESOLVED }?.let { onResolve ->
                                DropdownMenuItem(
                                    text = { Text("Resolve") },
                                    onClick = {
                                        onResolve()
                                        showActions = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Done,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            
                            onDelete?.takeIf { isCurrentUser }?.let { onDelete ->
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        onDelete()
                                        showActions = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Comment content
            if (comment.status == CommentStatus.DELETED) {
                Text(
                    text = "Comment deleted",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                // Highlight mentions in the comment text
                val annotatedString = buildAnnotatedString {
                    // In a real implementation, you would parse the content for mentions
                    // and add annotations for styling and click handling
                    append(comment.content)
                }
                
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Show resolved status
                if (comment.status == CommentStatus.RESOLVED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = spacing.small)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = spacing.small, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
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
        }
    }
}

@Composable
fun CommentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String = "Add a comment...",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    isFocused -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text field
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused },
            enabled = enabled,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                innerTextField()
            },
            singleLine = false,
            maxLines = 5,
            minLines = 1
        )
        
        // Send button
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank() && enabled,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (value.isNotBlank() && enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
            )
        }
    }
}
