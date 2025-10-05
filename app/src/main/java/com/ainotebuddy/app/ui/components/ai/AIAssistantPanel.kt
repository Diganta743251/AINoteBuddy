package com.ainotebuddy.app.ui.components.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.ai.AISuggestion
import com.ainotebuddy.app.ai.SuggestionType
import com.ainotebuddy.app.ai.SentimentAnalysisResult
import com.ainotebuddy.app.ai.VoiceCommand
import com.ainotebuddy.app.data.model.Note
import com.ainotebuddy.app.ui.theme.LocalSpacing
import kotlinx.coroutines.flow.StateFlow

/**
 * AI Assistant Panel that combines all AI features into a single interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantPanel(
    note: Note,
    onClose: () -> Unit,
    onSuggestionClick: (AISuggestion) -> Unit,
    onVoiceCommand: (String) -> Unit,
    onGenerateTags: suspend (List<String>) -> List<String>,
    onTagsChanged: (List<String>) -> Unit,
    sentimentResult: SentimentAnalysisResult?,
    suggestions: List<AISuggestion>,
    voiceCommands: List<VoiceCommand>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(AIAssistantTab.SUGGESTIONS) }
    
    // Animation specs
    val animationDuration = 300
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.medium)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.medium)
            ) {
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close AI Assistant"
                    )
                }
            }
            
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                AIAssistantTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = spacing.medium)
            ) {
                when (selectedTab) {
                    AIAssistantTab.SUGGESTIONS -> {
                        AISuggestionsContent(
                            suggestions = suggestions,
                            onSuggestionClick = onSuggestionClick,
                            isLoading = isLoading
                        )
                    }
                    AIAssistantTab.SENTIMENT -> {
                        SentimentAnalysisContent(
                            sentimentResult = sentimentResult,
                            isLoading = isLoading && sentimentResult == null
                        )
                    }
                    AIAssistantTab.TAGS -> {
                        AITagGenerator(
                            currentTags = note.tags?.split(",") ?: emptyList(),
                            onTagsChanged = onTagsChanged,
                            onGenerateTags = onGenerateTags,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    AIAssistantTab.VOICE -> {
                        VoiceCommandsContent(
                            commands = voiceCommands,
                            onCommandClick = { onVoiceCommand(it.command) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AISuggestionsContent(
    suggestions: List<AISuggestion>,
    onSuggestionClick: (AISuggestion) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    if (isLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            CircularProgressIndicator()
        }
    } else if (suggestions.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.AutoAwesome,
            title = "No suggestions yet",
            description = "Start typing to get AI suggestions",
            modifier = modifier
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = modifier.fillMaxSize()
        ) {
            items(suggestions) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun SentimentAnalysisContent(
    sentimentResult: SentimentAnalysisResult?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            CircularProgressIndicator()
        }
    } else if (sentimentResult == null) {
        EmptyState(
            icon = Icons.Outlined.SentimentNeutral,
            title = "No sentiment data",
            description = "Analyze your note to see sentiment insights",
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            // Sentiment summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Overall Sentiment",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SentimentIndicator(
                            sentiment = sentimentResult.sentiment,
                            confidence = sentimentResult.confidence,
                            size = 48.dp
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = sentimentResult.sentiment.toString().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "${(sentimentResult.confidence * 100).toInt()}% confidence",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Key phrases
            if (sentimentResult.keyPhrases.isNotEmpty()) {
                Text(
                    text = "Key Phrases",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sentimentResult.keyPhrases.take(10)) { phrase ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            
            // Emotions (if available)
            if (sentimentResult.emotions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Emotional Tone",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sentimentResult.emotions.entries.sortedByDescending { it.value }.forEach { (emotion, value) ->
                        EmotionBar(
                            emotion = emotion.toString().lowercase().replaceFirstChar { it.uppercase() },
                            value = value,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCommandsContent(
    commands: List<VoiceCommand>,
    onCommandClick: (VoiceCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    if (commands.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.RecordVoiceOver,
            title = "No voice commands available",
            description = "Voice commands will appear here",
            modifier = modifier
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(commands) { command ->
                Surface(
                    onClick = { onCommandClick(command) },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = command.command,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        if (command.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = command.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (command.example.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Example: ${command.example}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: AISuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (suggestion.type) {
        SuggestionType.TAGGING -> Icons.Outlined.Label
        SuggestionType.CONTENT_IMPROVEMENT -> Icons.Outlined.AutoFixHigh
        SuggestionType.RELATED_NOTE -> Icons.Outlined.Link
        SuggestionType.PRODUCTIVITY -> Icons.Outlined.AutoAwesome
        SuggestionType.FORMATTING -> Icons.Outlined.FormatColorText
        SuggestionType.ORGANIZATION -> Icons.Outlined.Folder
    }
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (suggestion.description.isNotBlank()) {
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (suggestion.confidence > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(suggestion.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EmotionBar(
    emotion: String,
    value: Float,
    modifier: Modifier = Modifier
) {
    val progress = value.coerceIn(0f, 1f)
    
    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = emotion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(100.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    progress > 0.7f -> MaterialTheme.colorScheme.primary
                    progress > 0.4f -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

