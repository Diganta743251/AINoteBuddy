package com.ainotebuddy.app.ui.components.collaboration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.collaboration.PresenceInfo
import kotlinx.coroutines.delay

@Composable
fun PresenceIndicator(
    presenceInfo: PresenceInfo,
    isCurrentUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayName = remember(presenceInfo.userId) {
        // In a real app, this would come from a user repository
        "User ${presenceInfo.userId.take(6)}"
    }
    
    val statusColor = when {
        !presenceInfo.isActive -> MaterialTheme.colorScheme.error
        presenceInfo.isTyping -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    }
    
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(
                width = 2.dp,
                color = statusColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // User avatar/initial
        Text(
            text = displayName.take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Status indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun PresenceTooltip(
    presenceInfo: PresenceInfo,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val displayName = remember(presenceInfo.userId) {
        "User ${presenceInfo.userId.take(6)}"
    }
    
    val statusText = remember(presenceInfo) {
        when {
            presenceInfo.isTyping -> "typing..."
            !presenceInfo.isActive -> "inactive"
            else -> "online"
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                presenceInfo.currentSection?.let { section ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Viewing: $section",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CursorIndicator(
    presenceInfo: PresenceInfo,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .width(2.dp)
            .height(24.dp)
            .background(color)
            .onHover { showTooltip = it }
    ) {
        // Cursor indicator line
        
        // Tooltip with user info
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = (-32).dp)
        ) {
            PresenceTooltip(
                presenceInfo = presenceInfo,
                isVisible = showTooltip
            )
        }
        
        // User label
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .offset(y = (-24).dp)
                .background(
                    color = color.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.extraSmall
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TypingIndicator(
    typingUsers: List<PresenceInfo>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return
    
    val typingText = when (typingUsers.size) {
        1 -> "${typingUsers[0].userId.take(6)} is typing..."
        2 -> "${typingUsers[0].userId.take(6)} and ${typingUsers[1].userId.take(6)} are typing..."
        else -> "${typingUsers.size} people are typing..."
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Text(
            text = typingText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PresenceAvatars(
    users: List<PresenceInfo>,
    maxVisible: Int = 3,
    modifier: Modifier = Modifier
) {
    val visibleUsers = users.take(maxVisible)
    val remainingCount = (users.size - maxVisible).coerceAtLeast(0)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleUsers.forEach { user ->
            var showTooltip by remember { mutableStateOf(false) }
            
            Box(
                modifier = Modifier
                    .padding(end = (-8).dp)
                    .onHover { showTooltip = it }
            ) {
                PresenceIndicator(
                    presenceInfo = user,
                    modifier = Modifier.size(28.dp)
                )
                
                // Show tooltip on hover
                PresenceTooltip(
                    presenceInfo = user,
                    isVisible = showTooltip,
                    modifier = Modifier.offset(y = 4.dp)
                )
            }
        }
        
        if (remainingCount > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$remainingCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun Modifier.onHover(
    onHover: (Boolean) -> Unit
): Modifier = composed {
    // Mobile Compose has no hover; call once with false and return unchanged
    LaunchedEffect(Unit) { onHover(false) }
    this
}

@Composable
fun PresenceStatusBar(
    activeUsers: List<PresenceInfo>,
    typingUsers: List<PresenceInfo>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show active users
        if (activeUsers.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Online:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PresenceAvatars(users = activeUsers)
            }
        }
        
        // Show typing indicator if needed
        if (typingUsers.isNotEmpty()) {
            TypingIndicator(typingUsers = typingUsers)
        }
    }
}
