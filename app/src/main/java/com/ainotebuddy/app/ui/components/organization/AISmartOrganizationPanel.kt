package com.ainotebuddy.app.ui.components.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.SmartFolder
import com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule
import com.ainotebuddy.app.ui.theme.LocalSpacing
import com.ainotebuddy.app.ui.viewmodel.organization.AISmartOrganizationUiState
import com.ainotebuddy.app.ui.viewmodel.organization.AISmartOrganizationViewModel
import kotlinx.coroutines.launch

/**
 * Main panel for AI-powered smart organization features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISmartOrganizationPanel(
    noteId: String,
    noteTitle: String,
    noteContent: String,
    viewModel: AISmartOrganizationViewModel,
    onFolderSelected: (SmartFolder) -> Unit,
    onNoteSelected: (Note) -> Unit,
    onTemplateCreated: (NoteTemplate) -> Unit,
    onRulesCreated: (List<SmartFolderRule>) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isProcessing = viewModel.isProcessing
    val processingMessage = viewModel.processingMessage
    
    val relatedNotes by viewModel.relatedNotes.collectAsState()
    val suggestedFolders by viewModel.suggestedFolders.collectAsState()
    val generatedTemplates by viewModel.generatedTemplates.collectAsState()
    val suggestedFolderRules by viewModel.suggestedFolderRules.collectAsState()
    val smartFolderSuggestions by viewModel.smartFolderSuggestions.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Tab titles
    val tabs = listOf(
        stringResource(R.string.tab_suggestions),
        stringResource(R.string.tab_related_notes),
        stringResource(R.string.tab_templates),
        stringResource(R.string.tab_smart_folders)
    )
    
    // Load data when panel is shown
    LaunchedEffect(noteId) {
        viewModel.analyzeNoteForFolders(noteId)
        viewModel.findRelatedNotes(noteId)
        viewModel.suggestFolderRules(noteId)
    }
    
    // Show loading state
    if (isProcessing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = processingMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    
    // Show error state
    if (uiState is AISmartOrganizationUiState.Error) {
        val errorMessage = (uiState as AISmartOrganizationUiState.Error).message
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = stringResource(R.string.error_loading_suggestions),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = { 
                        viewModel.clearState() 
                        viewModel.analyzeNoteForFolders(noteId)
                        viewModel.findRelatedNotes(noteId)
                        viewModel.suggestFolderRules(noteId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
        return
    }
    
    // Main content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.ai_smart_organization),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Tabs
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Tab content
        when (selectedTabIndex) {
            0 -> SuggestionsTab(
                noteId = noteId,
                noteTitle = noteTitle,
                noteContent = noteContent,
                suggestedFolders = suggestedFolders[noteId] ?: emptyList(),
                relatedNotes = relatedNotes[noteId] ?: emptyList(),
                generatedTemplate = generatedTemplates[noteId],
                suggestedFolderRules = suggestedFolderRules[noteId] ?: emptyList(),
                onFolderSelected = onFolderSelected,
                onNoteSelected = onNoteSelected,
                onTemplateCreated = onTemplateCreated,
                onRulesCreated = onRulesCreated
            )
            
            1 -> RelatedNotesTab(
                relatedNotes = relatedNotes[noteId] ?: emptyList(),
                onNoteSelected = onNoteSelected
            )
            
            2 -> TemplatesTab(
                generatedTemplate = generatedTemplates[noteId],
                onTemplateCreated = onTemplateCreated,
                onGenerateTemplate = { 
                    coroutineScope.launch {
                        viewModel.generateTemplateFromNote(noteId)
                    }
                }
            )
            
            3 -> SmartFoldersTab(
                suggestedFolders = suggestedFolders[noteId] ?: emptyList(),
                smartFolderSuggestions = smartFolderSuggestions,
                onFolderSelected = onFolderSelected,
                onRulesCreated = onRulesCreated,
                onGenerateSuggestions = { viewModel.suggestSmartFolders() }
            )
        }
    }
}

@Composable
private fun SuggestionsTab(
    noteId: String,
    noteTitle: String,
    noteContent: String,
    suggestedFolders: List<Pair<SmartFolder, Double>>,
    relatedNotes: List<Pair<Note, Double>>,
    generatedTemplate: NoteTemplate?,
    suggestedFolderRules: List<SmartFolderRule>,
    onFolderSelected: (SmartFolder) -> Unit,
    onNoteSelected: (Note) -> Unit,
    onTemplateCreated: (NoteTemplate) -> Unit,
    onRulesCreated: (List<SmartFolderRule>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Suggested Folders
        if (suggestedFolders.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.suggested_folders),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SuggestedFoldersList(
                    suggestedFolders = suggestedFolders,
                    onFolderSelected = onFolderSelected
                )
                
                if (suggestedFolderRules.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { onRulesCreated(suggestedFolderRules) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.apply_suggested_rules))
                    }
                }
            }
        }
        
        // Related Notes
        if (relatedNotes.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.related_notes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                RelatedNotesList(
                    relatedNotes = relatedNotes,
                    onNoteSelected = onNoteSelected
                )
            }
        }
        
        // Generated Template
        generatedTemplate?.let { template ->
            item {
                Text(
                    text = stringResource(R.string.generated_template),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                GeneratedTemplateCard(
                    template = template,
                    onUseTemplate = { onTemplateCreated(template) }
                )
            }
        }
    }
}

@Composable
private fun RelatedNotesTab(
    relatedNotes: List<Pair<Note, Double>>,
    onNoteSelected: (Note) -> Unit
) {
    if (relatedNotes.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Note,
            title = stringResource(R.string.no_related_notes_title),
            message = stringResource(R.string.no_related_notes_message)
        )
    } else {
        RelatedNotesList(
            relatedNotes = relatedNotes,
            onNoteSelected = onNoteSelected
        )
    }
}

@Composable
private fun TemplatesTab(
    generatedTemplate: NoteTemplate?,
    onTemplateCreated: (NoteTemplate) -> Unit,
    onGenerateTemplate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (generatedTemplate == null) {
            EmptyState(
                icon = Icons.Default.Description,
                title = stringResource(R.string.no_template_generated_title),
                message = stringResource(R.string.no_template_generated_message)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onGenerateTemplate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.generate_template))
            }
        } else {
            GeneratedTemplateCard(
                template = generatedTemplate,
                onUseTemplate = { onTemplateCreated(generatedTemplate) }
            )
        }
    }
}

@Composable
private fun SmartFoldersTab(
    suggestedFolders: List<Pair<SmartFolder, Double>>,
    smartFolderSuggestions: List<Pair<String, List<SmartFolderRule>>>,
    onFolderSelected: (SmartFolder) -> Unit,
    onRulesCreated: (List<SmartFolderRule>) -> Unit,
    onGenerateSuggestions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Suggested Folders
        if (suggestedFolders.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.suggested_folders),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SuggestedFoldersList(
                    suggestedFolders = suggestedFolders,
                    onFolderSelected = onFolderSelected
                )
            }
        }
        
        // Smart Folder Suggestions
        item {
            Text(
                text = stringResource(R.string.smart_folder_suggestions),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (smartFolderSuggestions.isEmpty()) {
                Button(
                    onClick = onGenerateSuggestions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(text = stringResource(R.string.generate_suggestions))
                }
            } else {
                SmartFolderSuggestionsList(
                    suggestions = smartFolderSuggestions,
                    onRulesCreated = onRulesCreated
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: Any,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
