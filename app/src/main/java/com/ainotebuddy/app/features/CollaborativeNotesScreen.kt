package com.ainotebuddy.app.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

data class Collaborator(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val role: CollaboratorRole = CollaboratorRole.VIEWER,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

enum class CollaboratorRole {
    OWNER, EDITOR, VIEWER
}

data class NoteComment(
    val id: String = UUID.randomUUID().toString(),
    val noteId: Long,
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val parentCommentId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeNotesScreen(
    note: NoteEntity,
    collaborators: List<Collaborator>,
    comments: List<NoteComment>,
    onBack: () -> Unit,
    onInviteCollaborator: (String, CollaboratorRole) -> Unit,
    onRemoveCollaborator: (Collaborator) -> Unit,
    onChangeRole: (Collaborator, CollaboratorRole) -> Unit,
    onAddComment: (String) -> Unit,
    onResolveComment: (NoteComment) -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Collaboration") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = { showInviteDialog = true }) {
                    Icon(Icons.Filled.PersonAdd, "Invite")
                }
                IconButton(onClick = { showCommentsSheet = true }) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text("${comments.count { !it.isResolved }}")
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Comment, "Comments")
                    }
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Note info
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shared with ${collaborators.size} people",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Online collaborators
            item {
                val onlineCollaborators = collaborators.filter { it.isOnline }
                if (onlineCollaborators.isNotEmpty()) {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Currently Online",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                onlineCollaborators.forEach { collaborator ->
                                    OnlineCollaboratorChip(collaborator)
                                }
                            }
                        }
                    }
                }
            }
            
            // All collaborators
            item {
                Text(
                    text = "All Collaborators",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(collaborators) { collaborator ->
                CollaboratorCard(
                    collaborator = collaborator,
                    onRemove = { onRemoveCollaborator(collaborator) },
                    onChangeRole = { role -> onChangeRole(collaborator, role) }
                )
            }
            
            // Recent activity
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(comments.take(5)) { comment ->
                CommentCard(
                    comment = comment,
                    onResolve = { onResolveComment(comment) }
                )
            }
        }
    }
    
    // Invite dialog
    if (showInviteDialog) {
        InviteCollaboratorDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { email, role ->
                onInviteCollaborator(email, role)
                showInviteDialog = false
            }
        )
    }
    
    // Comments sheet
    if (showCommentsSheet) {
        CommentsBottomSheet(
            comments = comments,
            onDismiss = { showCommentsSheet = false },
            onAddComment = onAddComment,
            onResolveComment = onResolveComment
        )
    }
}

@Composable
fun OnlineCollaboratorChip(collaborator: Collaborator) {
    AssistChip(
        onClick = { },
        label = { Text(collaborator.name.split(" ").firstOrNull() ?: "User") },
        leadingIcon = {
            Box {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = collaborator.name.firstOrNull()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                // Online indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    )
}

@Composable
fun CollaboratorCard(
    collaborator: Collaborator,
    onRemove: () -> Unit,
    onChangeRole: (CollaboratorRole) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = collaborator.name.firstOrNull()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = collaborator.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (collaborator.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                    }
                }
                
                Text(
                    text = collaborator.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!collaborator.isOnline) {
                    Text(
                        text = "Last seen ${dateFormat.format(Date(collaborator.lastSeen))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Role chip
            AssistChip(
                onClick = { /* Show role change menu */ },
                label = { Text(collaborator.role.name) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = when (collaborator.role) {
                        CollaboratorRole.OWNER -> MaterialTheme.colorScheme.primary
                        CollaboratorRole.EDITOR -> MaterialTheme.colorScheme.secondary
                        CollaboratorRole.VIEWER -> MaterialTheme.colorScheme.outline
                    }
                )
            )
            
            if (collaborator.role != CollaboratorRole.OWNER) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CommentCard(
    comment: NoteComment,
    onResolve: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (comment.isResolved) 
                MaterialTheme.colorScheme.surfaceVariant 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateFormat.format(Date(comment.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!comment.isResolved) {
                    TextButton(onClick = onResolve) {
                        Text("Resolve")
                    }
                } else {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Resolved",
                        tint = Color.Green
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InviteCollaboratorDialog(
    onDismiss: () -> Unit,
    onInvite: (String, CollaboratorRole) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(CollaboratorRole.VIEWER) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Collaborator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Role",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Column {
                    CollaboratorRole.values().filter { it != CollaboratorRole.OWNER }.forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role }
                            )
                            Text(
                                text = "${role.name} - ${getRoleDescription(role)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(email, selectedRole) },
                enabled = email.isNotBlank()
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CommentsBottomSheet(
    comments: List<NoteComment>,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onResolveComment: (NoteComment) -> Unit
) {
    var newComment by remember { mutableStateOf("") }
    
    // This would typically be a BottomSheetScaffold or ModalBottomSheet
    // For now, using a simple dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments (${comments.size})") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments) { comment ->
                        CommentCard(
                            comment = comment,
                            onResolve = { onResolveComment(comment) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    label = { Text("Add a comment...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newComment.isNotBlank()) {
                                    onAddComment(newComment)
                                    newComment = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Send, "Send")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun getRoleDescription(role: CollaboratorRole): String {
    return when (role) {
        CollaboratorRole.OWNER -> "Full access"
        CollaboratorRole.EDITOR -> "Can edit and comment"
        CollaboratorRole.VIEWER -> "Can view and comment"
    }
}