package com.ainotebuddy.app.ui.screens
import com.ainotebuddy.app.ui.theme.NoteType


import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.ai.AISuggestion
import com.ainotebuddy.app.ai.VoiceCommand
import com.ainotebuddy.app.ai.VoiceCommandResult
import com.ainotebuddy.app.ai.SentimentAnalysisResult
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.model.Note as UINote
import com.ainotebuddy.app.ui.components.ai.*
import com.ainotebuddy.app.ui.components.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
 
import com.ainotebuddy.app.ui.theme.*
import com.ainotebuddy.app.ai.AIViewModel
import com.ainotebuddy.app.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

data class FormattingOption(
    val name: String,
    val icon: ImageVector,
    val isActive: Boolean = false
)

data class NoteColor(
    val name: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNoteEditorScreen(
    noteId: Long = -1L,
    viewModel: NoteViewModel,
    aiViewModel: AIViewModel,
    onBack: () -> Unit,
    onSave: (NoteEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Personal") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var selectedColor by remember { mutableStateOf(NoteType.PERSONAL) }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var isPinned by rememberSaveable { mutableStateOf(false) }
    
    // Security state
    var isEncrypted by rememberSaveable { mutableStateOf(false) }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    var showAuthDialog by rememberSaveable { mutableStateOf(false) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Editor state
    var showFormattingToolbar by rememberSaveable { mutableStateOf(false) }
    var showCommentsPanel by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showTagEditor by rememberSaveable { mutableStateOf(false) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var showAIAssistant by remember { mutableStateOf(false) }
    
    // AI State
    val aiState by aiViewModel.uiState.collectAsState()
    var lastVoiceCommandResult by remember { mutableStateOf<VoiceCommandResult?>(null) }
    val suggestions = aiViewModel.suggestions
    val currentSentiment = aiViewModel.sentimentCache[noteId.toString()]
    
    // AI Assistant State
    val aiAssistantState = rememberAIAssistantState(
        suggestions = suggestions,
        sentimentResult = currentSentiment
    )
    
    // Update AI Assistant state when suggestions or sentiment changes
    LaunchedEffect(suggestions) {
        aiAssistantState.updateWithSuggestions(suggestions)
    }
    
    LaunchedEffect(currentSentiment) {
        currentSentiment?.let { result ->
            aiAssistantState.updateWithSentimentResult(result)
        }
    }
    
    // Generate AI suggestions when content changes
    LaunchedEffect(content) {
        if (content.isNotBlank()) {
            // Debounce to avoid too many API calls
            kotlinx.coroutines.delay(500)
            aiAssistantState.showLoading(true)
            aiViewModel.loadSuggestions(
                NoteEntity(
                    title = title.ifEmpty { "Untitled" },
                    content = content
                )
            )
        }
    }
    
    // Handle voice command results
    LaunchedEffect(aiState.lastVoiceCommandResult) {
        aiState.lastVoiceCommandResult?.let { result ->
            lastVoiceCommandResult = result
            aiAssistantState.handleVoiceCommandResult("Command executed: ${result.commandType}")
            
            when (result.commandType) {
                VoiceCommandResult.CommandType.CREATE_NOTE -> {
                    // Handle note creation from voice command
                    val newTitle = result.parameters["title"] as? String ?: ""
                    val newContent = result.parameters["content"] as? String ?: ""
                    title = newTitle
                    content = newContent
                }
                else -> {}
            }
        }
    }
    
    // Simplified: voice input handled locally via isVoiceRecording; collaboration features removed for compile stability
    
    // Auto-save state
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var lastSaved by remember { mutableStateOf<Long?>(null) }
    

    
    // Presence typing tracking removed
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    // Load existing note if needed (no-op for now)
    LaunchedEffect(noteId) { }
    
    // Auto-save functionality
    LaunchedEffect(title, content) {
        if (title.isNotEmpty() || content.isNotEmpty()) {
            hasUnsavedChanges = true
            delay(2000) // Auto-save after 2 seconds of inactivity
            if (hasUnsavedChanges) {
                // Perform auto-save
                lastSaved = System.currentTimeMillis()
                hasUnsavedChanges = false
            }
        }
    }
    
    // Legacy local suggestion generator removed; suggestions come from aiViewModel
    
    Scaffold(
        topBar = {
            ModernNoteEditorTopBar(
                title = if (noteId == -1L) "New Note" else "Edit Note",
                onBack = onBack,
                onSave = {
                    val now = System.currentTimeMillis()
                    val note = NoteEntity(
                        id = if (noteId == -1L) 0 else noteId,
                        title = title.ifEmpty { "Untitled" },
                        content = content,
                        createdAt = now,
                        updatedAt = now,
                        isFavorite = isFavorite,
                        isPinned = isPinned,
                        category = selectedCategory,
                        tags = selectedTags.joinToString(",")
                    )
                    onSave(note)
                    lastSaved = now
                    hasUnsavedChanges = false
                },
                isFavorite = isFavorite,
                onFavoriteClick = { isFavorite = !isFavorite },
                isPinned = isPinned,
                onPinClick = { isPinned = !isPinned },
                onShare = { /* TODO */ },
                onMoreOptions = { /* menu handled internally */ },
                hasUnsavedChanges = hasUnsavedChanges,
                lastSaved = lastSaved
            )
        },
        bottomBar = {
            Column {
                // AI Assistant Panel
                AnimatedVisibility(
                    visible = aiAssistantState.isVisible,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                ) {
                    AIAssistantPanel(
                        note = UINote(
                            id = noteId.toString(),
                            title = title,
                            content = content,
                            tags = selectedTags.joinToString(","),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        ),
                        onClose = { aiAssistantState.hide() },
                        onSuggestionClick = { suggestion ->
                            // Generic append behavior using available fields
                            val append = (suggestion.metadata["text"] as? String)
                                ?: suggestion.description.ifBlank { suggestion.title }
                            content = if (content.isBlank()) append else "$content\n$append"
                            aiAssistantState.selectSuggestion(suggestion)
                        },
                        onVoiceCommand = { command ->
                            aiViewModel.processVoiceCommand(command)
                        },
                        onGenerateTags = { currentTags ->
                            val note = NoteEntity(
                                title = title.ifEmpty { "Untitled" },
                                content = content,
                                tags = selectedTags.joinToString(",")
                            )
                            aiViewModel.generateTags(note, currentTags).first()
                        },
                        onTagsChanged = { newTags ->
                            selectedTags = newTags.toSet()
                        },
                        sentimentResult = currentSentiment,
                        suggestions = suggestions,
                        voiceCommands = listOf(
                            VoiceCommand("1", "Add tag [tag]", "Add a tag to the current note", "Add tag important"),
                            VoiceCommand("2", "Continue note with [text]", "Continue the current note", "Continue note with more details"),
                            VoiceCommand("3", "Analyze sentiment", "Analyze the sentiment of the note", "Analyze sentiment"),
                            VoiceCommand("4", "Generate tags", "Generate tags for the current note", "Generate tags"),
                            VoiceCommand("5", "Save note", "Save the current note", "Save note")
                        ),
                        isLoading = aiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
                
                // Bottom bar
                ModernEditorBottomBar(
                    onFormattingClick = { showFormattingToolbar = !showFormattingToolbar },
                    onColorClick = { showColorPicker = true },
                    onTagClick = { 
                        aiAssistantState.navigateTo(AIAssistantTab.TAGS)
                        aiAssistantState.show()
                    },
                    onVoiceClick = { isVoiceRecording = !isVoiceRecording }
                )
                
                // Voice FAB removed; using bottom bar voice toggle only
            }
            // Duplicate bottom bar and legacy voice FAB removed
        }
    ) { paddingValues ->
        // Main editor content
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Main editor content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                ) {
                    // Title field
                    ModernTextField(
                        value = title,
                        onValueChange = { title = it; hasUnsavedChanges = true },
                        placeholder = "Title",
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(titleFocusRequester),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { contentFocusRequester.requestFocus() }
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Content field
                    ModernTextField(
                        value = content,
                        onValueChange = { content = it; hasUnsavedChanges = true },
                        placeholder = "Start writing...",
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        )
                    )
                    
                    // Metadata and comments UI removed for compile stability
                }
                
                // Formatting toolbar
                AnimatedVisibility(
                    visible = showFormattingToolbar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    FormattingToolbar(
                        onFormatClick = { format ->
                            // Apply formatting to selected text
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
                
                // Color picker
                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    ColorPickerBar(
                        selectedColor = selectedColor,
                        onColorSelect = { color ->
                            selectedColor = color
                            showColorPicker = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
                
                // Tag editor
                AnimatedVisibility(
                    visible = showTagEditor,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    TagEditorBar(
                        selectedTags = selectedTags,
                        onTagsChange = { selectedTags = it },
                        onDismiss = { showTagEditor = false }
                    )
                }
                
                // Voice recording indicator
                AnimatedVisibility(
                    visible = isVoiceRecording,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    VoiceRecordingBar(
                        onStop = { 
                            isVoiceRecording = false
                            // Process voice input and add to content
                        },
                        onCancel = { isVoiceRecording = false }
                    )
                }
                
                // Sentiment analysis indicator (floating in bottom end)
                if (currentSentiment != null) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        SentimentIndicator(
                            sentiment = currentSentiment.sentiment,
                            confidence = currentSentiment.confidence,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernNoteEditorTopBar(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isPinned: Boolean,
    onPinClick: () -> Unit,
    onShare: () -> Unit,
    onMoreOptions: () -> Unit,
    hasUnsavedChanges: Boolean,
    lastSaved: Long?
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                if (lastSaved != null) {
                    Text(
                        text = "Saved ${formatTime(lastSaved)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (hasUnsavedChanges) {
                    Text(
                        text = "Unsaved changes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // Collaboration and comment panels removed
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (isFavorite) MaterialTheme.semanticColors().error else MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = onPinClick) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Toggle pin",
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            
            IconButton(onClick = onSave) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    tint = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            var expanded by remember { mutableStateOf(false) }
            
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    // Menu items
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            onShare()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null
                            )
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("Export") },
                        onClick = {
                            // Handle export
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ModernEditorBottomBar(
    onFormattingClick: () -> Unit,
    onColorClick: () -> Unit,
    onTagClick: () -> Unit,
    onVoiceClick: (() -> Unit)? = null,
    showVoiceButton: Boolean = true,
    showAIButton: Boolean = true,
    isVoiceRecording: Boolean = false,
    showAIAssistant: Boolean = false,
    onAIClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorToolButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Formatting",
                onClick = onFormattingClick
            )
            
            EditorToolButton(
                icon = Icons.Default.Palette,
                contentDescription = "Colors",
                onClick = onColorClick
            )
            
            EditorToolButton(
                icon = Icons.Default.LocalOffer,
                contentDescription = "Tags",
                onClick = onTagClick
            )
            
            // Voice button (conditionally shown)
            if (showVoiceButton && onVoiceClick != null) {
                EditorToolButton(
                    icon = if (isVoiceRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice input",
                    onClick = onVoiceClick,
                    isActive = isVoiceRecording,
                    activeColor = MaterialTheme.semanticColors().error
                )
            }
            
            // AI button (conditionally shown)
            if (showAIButton && onAIClick != null) {
                EditorToolButton(
                    icon = Icons.Default.Psychology,
                    contentDescription = "AI Assistant",
                    onClick = onAIClick,
                    isActive = showAIAssistant,
                    activeColor = MaterialTheme.semanticColors().aiPrimary
                )
            }
        }
    }
}

@Composable
private fun EditorToolButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isActive) activeColor.copy(alpha = 0.1f) else Color.Transparent
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormattingToolbar(
    onFormatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            val actions = listOf("B", "I", "U", "H1", "H2")
            actions.forEach { action ->
                AssistChip(
                    onClick = { onFormatClick(action) },
                    label = { Text(action) }
                )
            }
        }
    }
}

@Composable
private fun ColorPickerBar(
    selectedColor: NoteType,
    onColorSelect: (NoteType) -> Unit,
    modifier: Modifier = Modifier
) {
    val choices = NoteType.values().toList()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        choices.forEach { type ->
            val isSelected = type == selectedColor
            FilterChip(
                selected = isSelected,
                onClick = { onColorSelect(type) },
                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun TagEditorBar(
    selectedTags: Set<String>,
    onTagsChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newTag by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("Add tag") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newTag.isNotBlank()) {
                    onTagsChange(selectedTags + newTag.trim())
                    newTag = ""
                }
            }) { Text("Add") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) { Text("Done") }
        }
        if (selectedTags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedTags.toList()) { tag ->
                    AssistChip(
                        onClick = { onTagsChange(selectedTags - tag) },
                        label = { Text(tag) },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceRecordingBar(
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Listening…")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onStop) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        modifier = modifier
    ) { innerTextField ->
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        innerTextField()
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
