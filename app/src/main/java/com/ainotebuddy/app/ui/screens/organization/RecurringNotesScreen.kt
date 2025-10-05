package com.ainotebuddy.app.ui.screens.organization

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.RecurringNote
import androidx.compose.foundation.lazy.LazyRow
import com.ainotebuddy.app.ui.components.LoadingIndicator
import com.ainotebuddy.app.ui.theme.LocalSpacing
import com.ainotebuddy.app.ui.viewmodel.organization.RecurringNotesViewModel
import com.ainotebuddy.app.ui.viewmodel.organization.RecurringNotesViewModel.RecurringNotesUiState
import com.ainotebuddy.app.data.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecurringNotesScreen(
    onBackClick: () -> Unit,
    onNoteClick: (String) -> Unit,
    viewModel: RecurringNotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val availableTemplates by viewModel.availableTemplates.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val selectedPatternIds by viewModel.selectedPatterns.collectAsState()
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Process due notes when the screen is first shown
    LaunchedEffect(Unit) {
        viewModel.processDueNotes()
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var highlightedPatternId by remember { mutableStateOf<Long?>(null) }
    var showBulkDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Collect one-shot UI events for snackbar + haptic + highlight + undo
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecurringNotesViewModel.UiEvent.ShowMessage -> {
                    val result = snackbarHostState.showSnackbar(event.message)
                    if (result != SnackbarResult.Dismissed) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    event.noteId?.let { highlightedPatternId = it }
                }
                is RecurringNotesViewModel.UiEvent.ShowUndoToggle -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.togglePatternActive(event.patternId, event.previousState)
                    }
                }
                is RecurringNotesViewModel.UiEvent.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Recreate the deleted pattern
                        viewModel.undoDeletePattern(event.pattern)
                    }
                }
            }
        }
    }
    // Auto fade-out for highlight
    LaunchedEffect(highlightedPatternId) {
        if (highlightedPatternId != null) {
            kotlinx.coroutines.delay(com.ainotebuddy.app.ui.UiConstants.HIGHLIGHT_DURATION_MS)
            highlightedPatternId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_notes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    if (selectedPatternIds.isNotEmpty()) {
                        // Clear selection
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear selection"
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete)
                            )
                        }
                        // Overflow for extra actions
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Select all") },
                                onClick = {
                                    viewModel.selectAll()
                                    menuExpanded = false
                                }
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.setShowCreateDialog(true) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.create_recurring_note)
                            )
                        }
                        // Overflow for bulk operations
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Select all") },
                                onClick = {
                                    viewModel.selectAll()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Create recurring for all notes") },
                                onClick = {
                                    showBulkDialog = true
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowCreateDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_recurring_note)
                )
            }

            // Bulk create for all notes dialog
            if (showBulkDialog) {
                BulkRecurringDialog(
                    onDismiss = { showBulkDialog = false },
                    onConfirm = { rule: RecurringNote.RecurrenceRule, startDate: Date, maybeEndDate: Date? ->
                        viewModel.createRecurringForAllNotes(
                            rule = rule,
                            startMillis = startDate.time,
                            endMillis = maybeEndDate?.time
                        )
                        showBulkDialog = false
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is RecurringNotesUiState.Loading -> {
                    LoadingIndicator()
                }
                is RecurringNotesUiState.Success -> {
                    if (state.patterns.isEmpty()) {
                        EmptyRecurringNotesView(
                            onCreateClick = { viewModel.setShowCreateDialog(true) }
                        )
                    } else {
                        RecurrencePatternsList(
                            patterns = state.patterns.map { it.copy(isSelected = selectedPatternIds.contains(it.id)) },
                            onItemClick = { id ->
                                if (selectedPatternIds.isNotEmpty()) {
                                    viewModel.togglePatternSelection(id)
                                }
                            },
                            onItemLongClick = { id ->
                                viewModel.togglePatternSelection(id)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onToggleActive = { id, isActive -> viewModel.togglePatternActive(id, isActive) },
                            highlightedPatternId = highlightedPatternId
                        )
                    }
                }
                is RecurringNotesUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
            }
            
            // Create/Edit Recurring Note Dialog
            if (showCreateDialog) {
                RecurringNoteEditorDialog(
                    templates = availableTemplates,
                    notes = allNotes,
                    onDismiss = { viewModel.setShowCreateDialog(false) },
                    onCreate = { title, templateId, noteId, variables, rule, startDate, endDate ->
                        if (templateId != null) {
                            viewModel.createRecurringFromTemplate(
                                templateId = templateId,
                                title = title,
                                variables = variables,
                                rule = rule,
                                startMillis = startDate.time,
                                endMillis = endDate?.time
                            )
                            viewModel.setShowCreateDialog(false)
                        } else if (noteId != null) {
                            viewModel.createRecurringForExistingNote(
                                noteId = noteId,
                                rule = rule,
                                startMillis = startDate.time,
                                endMillis = endDate?.time
                            )
                            viewModel.setShowCreateDialog(false)
                        } else {
                            // No selection made; keep dialog open and show error
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Please select a template or an existing note"
                                )
                            }
                        }
                    }
                )
            }
            
            // Delete Confirmation Dialog
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text(stringResource(R.string.delete_recurring_notes_title)) },
                    text = {
                        Text(stringResource(R.string.delete_recurring_notes_message, selectedPatternIds.size))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSelectedPatterns()
                                showDeleteConfirm = false
                            }
                        ) { Text(stringResource(R.string.delete)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecurrencePatternsList(
    patterns: List<RecurringNotesViewModel.RecurrencePatternUiModel>,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    highlightedPatternId: Long? = null
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = patterns, key = { it.id }) { pattern ->
            val isHighlighted = highlightedPatternId == pattern.id
            RecurrencePatternItem(
                pattern = pattern,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                onClick = { onItemClick(pattern.id) },
                onLongClick = { onItemLongClick(pattern.id) },
                onToggleActive = { isChecked -> onToggleActive(pattern.id, isChecked) },
                highlight = isHighlighted
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecurrencePatternItem(
    pattern: RecurringNotesViewModel.RecurrencePatternUiModel,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    highlight: Boolean
) {
    val isSelected = pattern.isSelected

    // Compute highlight based on screen-level state (passed via remember in the list)
    // For now, derive highlight from a local remember key (pattern.id) via current highlight state in parent
    // We'll pass highlight flag from RecurrencePatternsList using highlightedPatternId

    // Colors: selected vs normal vs highlight (subtle)
    val targetColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = com.ainotebuddy.app.ui.UiConstants.HIGHLIGHT_FADE_DURATION_MS),
        label = "recurrenceItemBg"
    )

    // Subtle pulse when highlighted
    val scale by animateFloatAsState(
        targetValue = if (highlight) com.ainotebuddy.app.ui.UiConstants.HIGHLIGHT_PULSE_SCALE else 1f,
        animationSpec = tween(durationMillis = com.ainotebuddy.app.ui.UiConstants.HIGHLIGHT_PULSE_DURATION_MS),
        label = "recurrenceItemScale"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.scaleX = scale; this.scaleY = scale }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and Status
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pattern.noteTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Next Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        val nextText = pattern.nextOccurrence?.let { date ->
                            val dateStr = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy").format(date)
                            val timeStr = java.time.format.DateTimeFormatter.ofPattern("hh:mm a").format(pattern.timeOfDay)
                            "$dateStr at $timeStr"
                        } ?: "Not scheduled"
                        Text(
                            text = nextText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Recurrence and Template
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Recurrence
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = pattern.formatRecurrence(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Template (optional): show template label when available in UI model
                        // If you later add templateName to RecurrencePatternUiModel, re-enable this chip.
                    }
                }
                
                // Toggle Switch
                Switch(
                    checked = pattern.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                // Selection Indicator
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecurringNotesView(
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.no_recurring_notes_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.no_recurring_notes_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = stringResource(R.string.create_recurring_note))
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.error_loading_recurring_notes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecurringNoteEditorDialog(
    templates: List<NoteTemplate>,
    notes: List<Note>,
    onDismiss: () -> Unit,
    onCreate: (String, String?, Long?, Map<String, String>, RecurringNote.RecurrenceRule, Date, Date?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    var selectedNoteId by remember { mutableStateOf<Long?>(null) }
    var startDate by remember { mutableStateOf(Calendar.getInstance()) }
    var endDate by remember { mutableStateOf<Calendar?>(null) }
    var recurrenceRule by remember { 
        mutableStateOf(RecurringNote.RecurrenceRule.DAILY) 
    }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var endDateEnabled by remember { mutableStateOf(false) }
    var showTemplateSelector by remember { mutableStateOf(false) }
    var showNoteSelector by remember { mutableStateOf(false) }
    var useExistingNote by remember { mutableStateOf(false) }
    
    // Template variables
    var templateVariables by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    // Get the selected template
    val selectedTemplate = remember(selectedTemplateId, templates) {
        templates.find { it.id == selectedTemplateId }
    }
    
    // Update variables when template changes
    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { template ->
            // Initialize variables with default values
            val variables = template.variables.associate { variable ->
                variable.name to variable.defaultValue
            }
            templateVariables = variables
            
            // Set default title if not set
            if (title.isBlank()) {
                title = template.name
            }
        } ?: run {
            templateVariables = emptyMap()
        }
    }
    
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(text = stringResource(R.string.create_recurring_note))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useExistingNote,
                        onClick = {
                            useExistingNote = false
                            selectedNoteId = null
                        },
                        label = { Text("Use Template") }
                    )
                    FilterChip(
                        selected = useExistingNote,
                        onClick = {
                            useExistingNote = true
                            selectedTemplateId = null
                            templateVariables = emptyMap()
                        },
                        label = { Text("Existing Note") }
                    )
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Selector (Template or Note)
                if (!useExistingNote) {
                    OutlinedButton(
                        onClick = { showTemplateSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedTemplate?.name ?: stringResource(R.string.select_template),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedTemplate != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                } else {
                    val selectedNoteTitle = remember(selectedNoteId, notes) {
                        notes.firstOrNull { it.id == selectedNoteId }?.title
                    }
                    OutlinedButton(
                        onClick = { showNoteSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedNoteTitle ?: "Select existing note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedNoteTitle != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                // Template Variables
                if (!useExistingNote && selectedTemplate != null && templateVariables.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.template_variables),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedTemplate.variables.forEach { variable ->
                            val currentValue = templateVariables[variable.name] ?: ""
                            
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { newValue ->
                                    templateVariables = templateVariables.toMutableMap().apply {
                                        put(variable.name, newValue)
                                    }
                                },
                                label = { 
                                    Text(
                                        variable.placeholder.ifEmpty { variable.name }
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = variable.type == NoteTemplate.VariableType.TEXT
                            )
                        }
                    }
                }
                
                // Start Date & Time
                Text(
                    text = stringResource(R.string.start_date_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = dateFormat.format(startDate.time),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val initialHour = startDate.get(Calendar.HOUR_OF_DAY)
                            val initialMinute = startDate.get(Calendar.MINUTE)
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    startDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    startDate.set(Calendar.MINUTE, minute)
                                },
                                initialHour,
                                initialMinute,
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = timeFormat.format(startDate.time),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Recurrence Rule
                Text(
                    text = stringResource(R.string.repeat_every),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val recurrenceOptions = RecurringNote.RecurrenceRule.values()
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recurrenceOptions) { rule ->
                        val isSelected = rule == recurrenceRule
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { recurrenceRule = rule },
                            label = { Text(rule.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                
                // End Date (Optional)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = endDateEnabled,
                        onCheckedChange = { endDateEnabled = it }
                    )
                    
                    Text(
                        text = stringResource(R.string.set_end_date),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (endDateEnabled) {
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = endDate?.let { dateFormat.format(it.time) } 
                                ?: stringResource(R.string.select_end_date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (endDate != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val canConfirm = title.isNotBlank() && ((selectedTemplateId != null && !useExistingNote) || (selectedNoteId != null && useExistingNote))
            TextButton(
                onClick = {
                    if (!useExistingNote) {
                        onCreate(
                            title,
                            selectedTemplateId,
                            null,
                            templateVariables,
                            recurrenceRule,
                            startDate.time,
                            if (endDateEnabled) endDate?.time else null
                        )
                    } else {
                        onCreate(
                            title,
                            null,
                            selectedNoteId,
                            emptyMap(),
                            recurrenceRule,
                            startDate.time,
                            if (endDateEnabled) endDate?.time else null
                        )
                    }
                },
                enabled = canConfirm
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
    
    // Template Selector Dialog
    if (showTemplateSelector) {
        AlertDialog(
            onDismissRequest = { showTemplateSelector = false },
            title = { Text(stringResource(R.string.select_template)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (templates.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_templates_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(templates) { template ->
                            val isSelected = template.id == selectedTemplateId
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTemplateId = template.id
                                        showTemplateSelector = false
                                    },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = template.icon,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    
                                    Column {
                                        Text(
                                            text = template.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        
                                        if (template.description.isNotBlank()) {
                                            Text(
                                                text = template.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showTemplateSelector = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Note Selector Dialog
    if (showNoteSelector) {
        AlertDialog(
            onDismissRequest = { showNoteSelector = false },
            title = { Text("Select Note") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (notes.isEmpty()) {
                        item {
                            Text(
                                text = "No notes available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(notes) { note ->
                            val isSelected = note.id == selectedNoteId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedNoteId = note.id
                                        showNoteSelector = false
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = note.title, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNoteSelector = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Date Pickers
    if (showStartDatePicker) {
        LaunchedEffect(Unit) {
            val year = startDate.get(Calendar.YEAR)
            val month = startDate.get(Calendar.MONTH)
            val day = startDate.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(context, { _, y, m, d ->
                startDate.set(Calendar.YEAR, y)
                startDate.set(Calendar.MONTH, m)
                startDate.set(Calendar.DAY_OF_MONTH, d)
                showStartDatePicker = false
            }, year, month, day).apply {
                setOnDismissListener { showStartDatePicker = false }
            }.show()
        }
    }
    
    if (showEndDatePicker) {
        LaunchedEffect(Unit) {
            val cal = endDate ?: Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(context, { _, y, m, d ->
                val newCal = (endDate ?: Calendar.getInstance()).apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                endDate = newCal
                showEndDatePicker = false
            }, year, month, day).apply {
                setOnDismissListener { showEndDatePicker = false }
            }.show()
        }
    }
}

@Composable
private fun BulkRecurringDialog(
    onDismiss: () -> Unit,
    onConfirm: (RecurringNote.RecurrenceRule, Date, Date?) -> Unit
) {
    val context = LocalContext.current
    var startCal by remember { mutableStateOf(Calendar.getInstance()) }
    var endCal by remember { mutableStateOf<Calendar?>(null) }
    var endEnabled by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var rule by remember { mutableStateOf(RecurringNote.RecurrenceRule.DAILY) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Create for all notes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.repeat_every),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RecurringNote.RecurrenceRule.values()) { opt ->
                        FilterChip(
                            selected = rule == opt,
                            onClick = { rule = opt },
                            label = { Text(opt.displayName) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.start_date_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dateFormat.format(startCal.time))
                    }
                    OutlinedButton(onClick = {
                        val ih = startCal.get(Calendar.HOUR_OF_DAY)
                        val im = startCal.get(Calendar.MINUTE)
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                startCal.set(Calendar.HOUR_OF_DAY, h)
                                startCal.set(Calendar.MINUTE, m)
                            },
                            ih,
                            im,
                            false
                        ).show()
                    }, modifier = Modifier.weight(1f)) {
                        Text(timeFormat.format(startCal.time))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = endEnabled, onCheckedChange = { endEnabled = it })
                    Text(text = stringResource(R.string.set_end_date), style = MaterialTheme.typography.bodyMedium)
                }
                if (endEnabled) {
                    OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(endCal?.let { dateFormat.format(it.time) } ?: stringResource(R.string.select_end_date))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(rule, startCal.time, if (endEnabled) endCal?.time else null)
            }) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )

    if (showStartDatePicker) {
        LaunchedEffect(Unit) {
            val y = startCal.get(Calendar.YEAR)
            val m = startCal.get(Calendar.MONTH)
            val d = startCal.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(context, { _, yy, mm, dd ->
                startCal.set(Calendar.YEAR, yy)
                startCal.set(Calendar.MONTH, mm)
                startCal.set(Calendar.DAY_OF_MONTH, dd)
                showStartDatePicker = false
            }, y, m, d).apply { setOnDismissListener { showStartDatePicker = false } }.show()
        }
    }
    if (showEndDatePicker) {
        LaunchedEffect(Unit) {
            val base = endCal ?: Calendar.getInstance()
            val y = base.get(Calendar.YEAR)
            val m = base.get(Calendar.MONTH)
            val d = base.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(context, { _, yy, mm, dd ->
                endCal = (endCal ?: Calendar.getInstance()).apply {
                    set(Calendar.YEAR, yy)
                    set(Calendar.MONTH, mm)
                    set(Calendar.DAY_OF_MONTH, dd)
                }
                showEndDatePicker = false
            }, y, m, d).apply { setOnDismissListener { showEndDatePicker = false } }.show()
        }
    }
}
