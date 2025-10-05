package com.ainotebuddy.app.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ainotebuddy.app.collaboration.*
import com.ainotebuddy.app.collaboration.ui.*
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.viewmodel.NoteViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeNoteEditorScreen(
    noteId: String,
    viewModel: NoteViewModel,
    collaborativeViewModel: CollaborativeEditingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSave: (NoteEntity) -> Unit = {}
) {
    // Local state for UI
    var titleFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var contentFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentPosition by remember { mutableStateOf(TextRange.Zero) }
    var isTyping by remember { mutableStateOf(false) }
    
    // Collaborative state
    val currentSession by collaborativeViewModel.currentSession.collectAsState()
    val currentNote by collaborativeViewModel.currentNote.collectAsState()
    val presenceList by collaborativeViewModel.presenceList.collectAsState()
    val typingUsers by collaborativeViewModel.typingUsers.collectAsState()
    val conflictToResolve by collaborativeViewModel.conflictToResolve.collectAsState()
    val comments by collaborativeViewModel.comments.collectAsState()
    val isLoading by collaborativeViewModel.isLoading.collectAsState()
    val errorMessage by collaborativeViewModel.errorMessage.collectAsState()
    val activeCursorPositions by collaborativeViewModel.activeCursorPositions.collectAsState()
    
    // Load note and start collaboration
    LaunchedEffect(noteId) {
        if (noteId != "-1") {
            val note = viewModel.getNoteById(noteId.toLong())
            note?.let {
                titleFieldValue = TextFieldValue(it.title)
                contentFieldValue = TextFieldValue(it.content)
            }
            
            // Start collaborative editing
            collaborativeViewModel.startCollaboration(noteId)
        }
    }
    
    // Update local content when collaborative changes occur
    LaunchedEffect(currentNote) {
        currentNote?.let { note ->
            if (contentFieldValue.text != note.content) {
                contentFieldValue = TextFieldValue(
                    text = note.content,
                    selection = contentFieldValue.selection
                )
            }
            if (titleFieldValue.text != note.title) {
                titleFieldValue = TextFieldValue(
                    text = note.title,
                    selection = titleFieldValue.selection
                )
            }
        }
    }
    
    // Handle typing indicator
    LaunchedEffect(isTyping) {
        collaborativeViewModel.updateTypingStatus(isTyping)
        if (isTyping) {
            delay(3000) // Stop typing indicator after 3 seconds of inactivity
            isTyping = false
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            collaborativeViewModel.stopCollaboration()
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            if (noteId == "-1") "New Note" 
                            else if (currentSession != null) "Collaborative Note" 
                            else "Edit Note"
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Collaboration toggle
                        if (noteId != "-1") {
                            IconButton(
                                onClick = {
                                    if (currentSession != null) {
                                        collaborativeViewModel.stopCollaboration()
                                    } else {
                                        collaborativeViewModel.startCollaboration(noteId)
                                    }
                                }
                            ) {
                                Icon(
                                    if (currentSession != null) Icons.Default.GroupOff else Icons.Default.Group,
                                    contentDescription = if (currentSession != null) "Stop Collaboration" else "Start Collaboration",
                                    tint = if (currentSession != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Add comment
                        if (currentSession != null) {
                            IconButton(
                                onClick = {
                                    commentPosition = contentFieldValue.selection
                                    showCommentDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Comment, contentDescription = "Add Comment")
                            }
                        }
                        
                        // Save
                        IconButton(
                            onClick = {
                                val note = NoteEntity(
                                    id = if (noteId == "-1") 0 else noteId.toLong(),
                                    title = titleFieldValue.text.ifBlank { "Untitled" },
                                    content = contentFieldValue.text,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis(),
                                    shareId = currentSession?.sessionId.orEmpty()
                                )
                                onSave(note)
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                )
                
                // Collaboration status bar
                CollaborationStatusBar(
                    session = currentSession,
                    presenceList = presenceList,
                    onShareClick = {
                        // TODO: Implement sharing dialog
                    },
                    onSettingsClick = {
                        // TODO: Implement collaboration settings
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title field with collaborative features
                CollaborativeTextField(
                    value = titleFieldValue,
                    onValueChange = { newValue ->
                        val oldValue = titleFieldValue
                        titleFieldValue = newValue
                        
                        // Handle collaborative editing
                        if (currentSession != null && newValue.text != oldValue.text) {
                            val deletedText = oldValue.text.substring(
                                minOf(oldValue.selection.start, newValue.selection.start),
                                maxOf(oldValue.selection.start, oldValue.text.length)
                            )
                            val insertedText = newValue.text.substring(
                                minOf(oldValue.selection.start, newValue.selection.start),
                                maxOf(newValue.selection.start, newValue.text.length)
                            )
                            
                            collaborativeViewModel.applyTextEdit(
                                position = minOf(oldValue.selection.start, newValue.selection.start),
                                deletedText = deletedText,
                                insertedText = insertedText
                            )
                        }
                        
                        isTyping = true
                    },
                    onSelectionChange = { selection ->
                        collaborativeViewModel.updateCursorPosition(selection.start, selection.end)
                    },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = "Note title...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    activeCursorPositions = activeCursorPositions,
                    presenceList = presenceList
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                // Content field with collaborative features
                CollaborativeTextField(
                    value = contentFieldValue,
                    onValueChange = { newValue ->
                        val oldValue = contentFieldValue
                        contentFieldValue = newValue
                        
                        // Handle collaborative editing
                        if (currentSession != null && newValue.text != oldValue.text) {
                            val deletedText = if (oldValue.text.length > newValue.text.length) {
                                oldValue.text.substring(
                                    newValue.selection.start,
                                    newValue.selection.start + (oldValue.text.length - newValue.text.length)
                                )
                            } else ""
                            
                            val insertedText = if (newValue.text.length > oldValue.text.length) {
                                newValue.text.substring(
                                    oldValue.selection.start,
                                    oldValue.selection.start + (newValue.text.length - oldValue.text.length)
                                )
                            } else ""
                            
                            collaborativeViewModel.applyTextEdit(
                                position = minOf(oldValue.selection.start, newValue.selection.start),
                                deletedText = deletedText,
                                insertedText = insertedText
                            )
                        }
                        
                        isTyping = true
                    },
                    onSelectionChange = { selection ->
                        collaborativeViewModel.updateCursorPosition(selection.start, selection.end)
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = "Start writing your note...",
                    modifier = Modifier.fillMaxSize(),
                    activeCursorPositions = activeCursorPositions,
                    presenceList = presenceList,
                    comments = comments
                )
            }
            
            // Typing indicator
            if (typingUsers.isNotEmpty()) {
                TypingIndicator(
                    typingUsers = typingUsers,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    // Error handling
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or dialog
            // For now, just clear the error after showing
            delay(3000)
            collaborativeViewModel.clearError()
        }
    }
    
    // Conflict resolution dialog (mapped to ViewModel's types)
    CollaborativeConflictDialog(
        state = conflictToResolve,
        onAcceptLocal = {
            collaborativeViewModel.resolveConflict(CollaborativeEditingViewModel.ConflictResolution.AcceptLocal)
        },
        onAcceptRemote = {
            collaborativeViewModel.resolveConflict(CollaborativeEditingViewModel.ConflictResolution.AcceptRemote)
        },
        onMerge = {
            val merged = currentNote?.content ?: ""
            collaborativeViewModel.resolveConflict(CollaborativeEditingViewModel.ConflictResolution.Merge(merged))
        },
        onDismiss = { collaborativeViewModel.clearError() }
    )
    
    // Comment dialog
    if (showCommentDialog) {
        CommentDialog(
            onDismiss = { showCommentDialog = false },
            onAddComment = { commentText ->
                val selectedText = contentFieldValue.text.substring(
                    commentPosition.start,
                    commentPosition.end
                )
                collaborativeViewModel.addComment(
                    content = commentText,
                    startIndex = commentPosition.start,
                    endIndex = commentPosition.end,
                    selectedText = selectedText
                )
                showCommentDialog = false
            }
        )
    }
}

/**
 * Enhanced text field with collaborative features
 */
@Composable
private fun CollaborativeTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSelectionChange: (TextRange) -> Unit = {},
    textStyle: TextStyle,
    placeholder: String,
    modifier: Modifier = Modifier,
    activeCursorPositions: List<CursorPosition> = emptyList(),
    presenceList: List<PresenceInfo> = emptyList(),
    comments: List<CollaborativeComment> = emptyList()
) {
    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                if (newValue.selection != value.selection) {
                    onSelectionChange(newValue.selection)
                }
            },
            textStyle = textStyle,
            modifier = Modifier.fillMaxSize(),
            decorationBox = { innerTextField ->
                Box {
                    // Placeholder
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(
                                color = textStyle.color.copy(alpha = 0.5f)
                            )
                        )
                    }
                    
                    // Live cursors overlay
                    activeCursorPositions.forEach { cursorPosition ->
                        // Render a basic live cursor at the offset position with a default color/name
                        LiveCursor(
                            cursorPosition = cursorPosition,
                            userColor = "#4ECDC4",
                            userName = "User",
                            modifier = Modifier
                                .offset(
                                    x = calculateCursorXOffset(cursorPosition.offset, value.text),
                                    y = calculateCursorYOffset(cursorPosition.offset, value.text)
                                )
                        )
                    }
                    
                    // Comments overlay
                    CommentsOverlay(
                        comments = comments,
                        onCommentClick = { comment ->
                            // TODO: Show comment details
                        }
                    )
                    
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Comment dialog for adding collaborative comments
 */
@Composable
private fun CommentDialog(
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Comment") },
        text = {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Comment") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText)
                    }
                },
                enabled = commentText.isNotBlank()
            ) {
                Text("Add Comment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions for cursor positioning
private fun calculateCursorXOffset(position: Int, text: String): androidx.compose.ui.unit.Dp {
    // Simplified calculation - in a real implementation, you'd need to measure text
    return (position * 8).dp // Approximate character width
}

private fun calculateCursorYOffset(position: Int, text: String): androidx.compose.ui.unit.Dp {
    // Simplified calculation - count line breaks before position
    val lineBreaks = text.substring(0, minOf(position, text.length)).count { it == '\n' }
    return (lineBreaks * 20).dp // Approximate line height
}

private fun getUserColor(userId: String): String {
    val colors = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
        "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
    )
    return colors[userId.hashCode().rem(colors.size).let { if (it < 0) it + colors.size else it }]
}

private fun getUserDisplayName(userId: String): String {
    // This would typically fetch from user cache or database
    return "User ${userId.take(8)}"
}

@Composable
private fun CollaborativeConflictDialog(
    state: CollaborativeEditingViewModel.ConflictResolutionState?,
    onAcceptLocal: () -> Unit,
    onAcceptRemote: () -> Unit,
    onMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Resolve Conflict") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A conflict was detected between local and remote changes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Choose how to resolve:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onAcceptRemote) { Text("Accept Remote") }
                    TextButton(onClick = onAcceptLocal) { Text("Keep Local") }
                    Button(onClick = onMerge) { Text("Merge") }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}
