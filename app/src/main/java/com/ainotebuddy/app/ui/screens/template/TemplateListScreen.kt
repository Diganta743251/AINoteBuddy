package com.ainotebuddy.app.ui.screens.template

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.components.LoadingIndicator
import com.ainotebuddy.app.ui.theme.LocalSpacing
import com.ainotebuddy.app.ui.viewmodel.template.TemplateListViewModel
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel

// ... [Previous code remains the same until the SearchBar composable] ...

// Temporary extension properties to satisfy UI until metadata is implemented
private val NoteTemplate.tags: List<String> get() = emptyList()
private val NoteTemplate.usageCount: Int get() = 0
private val NoteTemplate.lastUsedAt: Long get() = 0L

/**
 * Composable that displays a list of templates
 */
@Composable
private fun TemplateList(
    templates: List<NoteTemplate>,
    onTemplateClick: (String) -> Unit,
    onTemplateFavoriteToggle: (NoteTemplate, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val spacing = LocalSpacing.current
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = spacing.medium,
            top = spacing.small,
            end = spacing.medium,
            bottom = spacing.extraLarge + 80.dp // Extra padding for FAB
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = modifier
    ) {
        items(
            items = templates,
            key = { it.id }
        ) { template ->
            TemplateItem(
                template = template,
                onTemplateClick = { onTemplateClick(template.id) },
                onFavoriteToggle = { isFavorite ->
                    onTemplateFavoriteToggle(template, isFavorite)
                }
            )
        }
    }
}

/**
 * Composable that displays a single template item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateItem(
    template: NoteTemplate,
    onTemplateClick: () -> Unit,
    onFavoriteToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Card(
        onClick = onTemplateClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Header with title and favorite button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Template icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(spacing.medium))
                
                // Title and description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (template.description.isNotBlank()) {
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Favorite button
                // Favorite toggle removed as NoteTemplate doesn't track favorites
                /*IconButton(
                    onClick = { onFavoriteToggle(!template.isFavorite) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (template.isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (template.isFavorite) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(R.string.add_to_favorites)
                        },
                        tint = if (template.isFavorite) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }*/
            }
            
            // Tags
            if (template.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.small))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    template.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { /* TODO: Filter by tag */ }
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = spacing.small,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                    
                    if (template.tags.size > 3) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        ) {
                            Text(
                                text = "+${template.tags.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
            
            // Footer with usage stats
            Spacer(modifier = Modifier.height(spacing.small))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Last used time
                if (template.lastUsedAt > 0) {
                    Text(
                        text = "Created: ${formatDate(template.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Never used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // Usage count
                // Usage count not tracked in current model; hide
            }
        }
    }
}

/**
 * Helper function to format a timestamp to a readable date string
 */
private fun formatDate(timestamp: Long): String {
    // In a real app, you would use a proper date formatter
    return "${android.text.format.DateUtils.getRelativeTimeSpanString(timestamp)}"
}

/**
 * Enum class for template sort options
 */
enum class TemplateSortOption {
    NAME_ASC, NAME_DESC, LAST_USED, CREATION_DATE
}

/**
 * Top-level screen that displays templates and exposes navigation callbacks.
 */
@Composable
fun TemplateListScreen(
    onTemplateClick: (String) -> Unit,
    onCreateNewTemplate: () -> Unit,
    viewModel: TemplateListViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates") },
                actions = {
                    IconButton(onClick = { onCreateNewTemplate() }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create Template")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNewTemplate) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Template")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { LoadingIndicator() }
            } else {
                TemplateList(
                    templates = uiState.filteredTemplates.ifEmpty { uiState.templates },
                    onTemplateClick = onTemplateClick,
                    onTemplateFavoriteToggle = { _, _ -> },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
