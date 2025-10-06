package com.ainotebuddy.app.ui.screens.organization

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
// Using staggered grid for template cards
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ainotebuddy.app.R
import com.ainotebuddy.app.ui.components.LoadingIndicator
import com.ainotebuddy.app.ui.theme.LocalSpacing
import com.ainotebuddy.app.ui.viewmodel.organization.TemplatesViewModel
import com.ainotebuddy.app.ui.viewmodel.organization.TemplatesViewModel.TemplatesUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesScreen(
    onBackClick: () -> Unit,
    onTemplateClick: (String) -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTemplates by viewModel.selectedTemplates.collectAsStateWithLifecycle()
    val showCreateDialog by viewModel.showCreateDialog.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    
    val spacing = LocalSpacing.current
    
    // Handle template generation result
    LaunchedEffect(uiState) {
        if (uiState is TemplatesUiState.TemplateGenerated) {
            // Show the generated template in the create dialog
            viewModel.setShowCreateDialog(true)
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot UI events for snackbar + haptic + highlight
    val haptic = LocalHapticFeedback.current
    var highlightedTemplateId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplatesViewModel.UiEvent.ShowMessage -> {
                    val result = snackbarHostState.showSnackbar(event.message)
                    if (result != SnackbarResult.Dismissed) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    // Highlight the affected template if id provided
                    event.templateId?.let { id ->
                        highlightedTemplateId = id
                    }
                }
                is TemplatesViewModel.UiEvent.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.addTemplate(event.template.copy(id = ""))
                    }
                }
            }
        }
    }

    // Auto fade-out for highlight
    LaunchedEffect(highlightedTemplateId) {
        if (highlightedTemplateId != null) {
            kotlinx.coroutines.delay(com.ainotebuddy.app.ui.UiConstants.HIGHLIGHT_DURATION_MS)
            highlightedTemplateId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    // Category filter
                    Box {
                        IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filter_by_category)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            // All categories option
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.all_categories)) },
                                onClick = {
                                    viewModel.filterByCategory(null)
                                    showCategoryMenu = false
                                },
                                leadingIcon = {
                                    if (selectedCategory == null) {
                                        Icon(Icons.Default.Check, null)
                                    } else {
                                        Spacer(Modifier.size(24.dp))
                                    }
                                }
                            )
                            
                            Divider()
                            
                            // Category list
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.filterByCategory(category)
                                        showCategoryMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedCategory == category) {
                                            Icon(Icons.Default.Check, null)
                                        } else {
                                            Spacer(Modifier.size(24.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    // Action buttons
                    if (selectedTemplates.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_selected)
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.setShowCreateDialog(true) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.create_template)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is TemplatesUiState.Loading -> {
                    LoadingIndicator()
                }
                is TemplatesUiState.Success -> {
                    val liveTemplates by viewModel.templates.collectAsState()
                    if (liveTemplates.isEmpty()) {
                        EmptyTemplatesView(
                            onCreateClick = { viewModel.setShowCreateDialog(true) }
                        )
                    } else {
                        TemplatesGrid(
                            templates = state.templates,
                            selectedTemplates = selectedTemplates,
                            onTemplateClick = onTemplateClick,
                            onTemplateLongClick = { templateId ->
                                viewModel.toggleTemplateSelection(templateId)
                            },
                            onEdit = { templateId -> viewModel.startEditTemplate(templateId) },
                            onDelete = { templateId -> viewModel.deleteTemplate(templateId) },
                            highlightedTemplateId = highlightedTemplateId
                        )
                    }
                }
                is TemplatesUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { /* no-op: loadTemplates is internal and screen should rely on flow */ }
                    )
                }
                is TemplatesUiState.TemplateGenerated -> {
                    // Handled by the dialog
                }
            }
            
            // Create Template Dialog
            if (showCreateDialog) {
                TemplateEditorDialog(
                    template = null,
                    categories = categories,
                    onDismiss = {
                        viewModel.setShowCreateDialog(false)
                    },
                    onSave = { name, description, icon, category, content, variables ->
                        viewModel.createTemplate(
                            name = name,
                            description = description,
                            icon = icon,
                            category = category,
                            content = content,
                            variables = variables
                        )
                    }
                )
            }

            // Edit Template Dialog (prefilled)
            val editingTemplate by viewModel.editingTemplate.collectAsState()
            if (editingTemplate != null) {
                TemplateEditorDialog(
                    template = editingTemplate,
                    categories = categories,
                    onDismiss = { viewModel.dismissEditor() },
                    onSave = { name, description, icon, category, content, variables ->
                        val updated = editingTemplate!!.copy(
                            name = name,
                            description = description,
                            icon = icon,
                            category = category,
                            content = content,
                            variables = variables
                        )
                        viewModel.saveEditedTemplate(updated)
                    }
                )
            }
            
            // Delete Confirmation Dialog
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text(stringResource(R.string.delete_templates_title)) },
                    text = {
                        Text(stringResource(R.string.delete_templates_message, selectedTemplates.size))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSelectedTemplates()
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

@Composable
private fun TemplatesGrid(
    templates: List<TemplatesViewModel.TemplateUiModel>,
    selectedTemplates: Set<String>,
    onTemplateClick: (String) -> Unit,
    onTemplateLongClick: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    highlightedTemplateId: String?
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(
            count = templates.size,
            key = { index -> templates[index].id }
        ) { index ->
            val template = templates[index]
            val isSelected = selectedTemplates.contains(template.id)

            TemplateCard(
                template = template,
                isSelected = isSelected,
                onClick = { onTemplateClick(template.id) },
                onLongClick = { onTemplateLongClick(template.id) },
                onEdit = { onEdit(template.id) },
                onDelete = { onDelete(template.id) },
                highlight = template.id == highlightedTemplateId
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateCard(
    template: TemplatesViewModel.TemplateUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    highlight: Boolean
) {
    // Subtle highlight animation for success cues
    val targetColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        highlight -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 450),
        label = "templateCardBg"
    )
    
    // Subtle pulse scale on highlight
    val scale by animateFloatAsState(
        targetValue = if (highlight) com.ainotebuddy.app.ui.UiConstants.HIGHLIGHT_PULSE_SCALE else 1f,
        animationSpec = tween(durationMillis = com.ainotebuddy.app.ui.UiConstants.HIGHLIGHT_PULSE_DURATION_MS),
        label = "templateCardScale"
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
            // Template Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = template.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Template Name
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Template Description
            if (template.description.isNotBlank()) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category Chip
            if (template.category.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = template.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun EmptyTemplatesView(
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
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.no_templates_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.no_templates_message),
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
            Text(text = stringResource(R.string.create_template))
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
            text = stringResource(R.string.error_loading_templates),
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
private fun TemplateEditorDialog(
    template: com.ainotebuddy.app.data.model.organization.NoteTemplate?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, List<com.ainotebuddy.app.data.model.organization.NoteTemplate.TemplateVariable>) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var icon by remember { mutableStateOf(template?.icon ?: "📝") }
    var category by remember { mutableStateOf(template?.category ?: categories.firstOrNull() ?: "") }
    var content by remember { mutableStateOf(template?.content ?: "") }
    
    // For template variables
    var variables by remember {
        mutableStateOf(
            template?.variables ?: listOf(
                NoteTemplate.TemplateVariable("title", "", "Note Title"),
                NoteTemplate.TemplateVariable("content", "", "Note Content")
            )
        )
    }
    // Remove invalid extension overrides; use standard list operations
    
    var showAddVariableDialog by remember { mutableStateOf(false) }
    var editingVariableIndex by remember { mutableStateOf(-1) }
    
    val icons = listOf("📝", "📋", "📑", "📄", "📝", "📋", "📑", "📄")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(text = if (template == null) stringResource(R.string.create_template) else stringResource(R.string.edit_template))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Template Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.template_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Template Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                // Icon Selection
                Text(
                    text = stringResource(R.string.select_icon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(icons) { currentIcon ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (currentIcon == icon) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable { icon = currentIcon },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentIcon,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
                
                // Category Selection
                var expanded by remember { mutableStateOf(false) }
                val currentCategory = category.ifEmpty { stringResource(R.string.select_category) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = currentCategory,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { categoryOption ->
                            DropdownMenuItem(
                                text = { Text(categoryOption) },
                                onClick = {
                                    category = categoryOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Template Variables
                Text(
                    text = stringResource(R.string.template_variables),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = variables,
                        key = { it.name }
                    ) { variable ->
                        TemplateVariableItem(
                            name = variable.name,
                            defaultValue = variable.defaultValue,
                            description = variable.placeholder,
                            onEdit = {
                                val idx = variables.indexOfFirst { it.name == variable.name }
                                editingVariableIndex = idx
                                showAddVariableDialog = true
                            },
                            onDelete = {
                                variables = variables.filterNot { it.name == variable.name }
                            }
                        )
                    }
                    
                    item {
                        TextButton(
                            onClick = {
                                editingVariableIndex = -1
                                showAddVariableDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Add Variable button
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_variable))
                        }
                    }
                }
                
                // Template Content
                Text(
                    text = stringResource(R.string.template_content),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = {
                        Text(
                            text = "Enter your template content here. Use {{variable}} for variables.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && content.isNotBlank()) {
                        onSave(name, description, icon, category, content, variables)
                    }
                },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
    
    // Variable Editor Dialog
    if (showAddVariableDialog) {
        var varName by remember { mutableStateOf("") }
        var defaultValue by remember { mutableStateOf("") }
        var varDescription by remember { mutableStateOf("") }
        
        // Initialize with existing variable data if editing
        LaunchedEffect(editingVariableIndex) {
            if (editingVariableIndex >= 0 && editingVariableIndex < variables.size) {
                val variable = variables[editingVariableIndex]
                varName = variable.name
                defaultValue = variable.defaultValue
                varDescription = variable.placeholder
            } else {
                varName = ""
                defaultValue = ""
                varDescription = ""
            }
        }
        
        AlertDialog(
            onDismissRequest = { showAddVariableDialog = false },
            title = { 
                Text(
                    if (editingVariableIndex >= 0) 
                        stringResource(R.string.edit_variable) 
                    else 
                        stringResource(R.string.add_variable)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = varName,
                        onValueChange = { 
                            // Only allow alphanumeric and underscore
                            if (it.matches(Regex("^[a-zA-Z0-9_]*$"))) {
                                varName = it
                            }
                        },
                        label = { Text(stringResource(R.string.variable_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = varName.isNotEmpty() && !varName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
                    )
                    
                    OutlinedTextField(
                        value = defaultValue,
                        onValueChange = { defaultValue = it },
                        label = { Text(stringResource(R.string.default_value)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = varDescription,
                        onValueChange = { varDescription = it },
                        label = { Text(stringResource(R.string.description_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (varName.isNotBlank() && varName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                            val newVariable = com.ainotebuddy.app.data.model.organization.NoteTemplate.TemplateVariable(
                                name = varName,
                                defaultValue = defaultValue,
                                placeholder = varDescription
                            )
                            // Replace or append variable safely
                            val list = variables.toMutableList()
                            if (editingVariableIndex >= 0 && editingVariableIndex < list.size) {
                                list[editingVariableIndex] = newVariable
                            } else {
                                list.add(newVariable)
                            }
                            variables = list
                            
                            showAddVariableDialog = false
                        }
                    },
                    enabled = varName.isNotBlank() && varName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVariableDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TemplateVariableItem(
    name: String,
    defaultValue: String,
    description: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "{{$name}}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (defaultValue.isNotBlank()) {
                    Text(
                        text = "Default: $defaultValue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit) + " variable",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete) + " variable",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
