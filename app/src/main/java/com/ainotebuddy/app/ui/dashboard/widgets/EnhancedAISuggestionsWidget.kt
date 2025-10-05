package com.ainotebuddy.app.ui.dashboard.widgets

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotebuddy.app.ai.*
import com.ainotebuddy.app.integration.ContentInsights
 
import com.ainotebuddy.app.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

@Composable
fun EnhancedAISuggestionsWidget(
    onSuggestionClick: (SmartSuggestion) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = viewModel()
) {
    val context = LocalContext.current
    val aiAnalysisEngine = remember { AIAnalysisEngine() }
    val scope = rememberCoroutineScope()
 
    val notes by viewModel.notes.collectAsState()
    
    var aiInsights by remember { mutableStateOf<ContentInsights?>(null) }
    var smartSuggestions by remember { mutableStateOf<List<SmartSuggestion>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(AIInsightTab.SUGGESTIONS) }
    // Analyze notes when they change
    LaunchedEffect(notes) {
        if (notes.isNotEmpty()) {
            isAnalyzing = true
            try {
                val summary = aiAnalysisEngine.generateContentSummary(notes)
                aiInsights = ContentInsights(
                    keyInsights = summary,
                    sentimentSummary = ""
                )
                
                // Generate simple smart suggestions for recent notes
                val recentNotes = notes.sortedByDescending { it.updatedAt }.take(5)
                val suggestions = mutableListOf<SmartSuggestion>()
                
                recentNotes.forEach { note ->
                    // Create a couple of simple suggestions per note
                    suggestions += SmartSuggestion(
                        id = "s-${note.id}-summary",
                        type = SuggestionType.CONTENT_IMPROVEMENT,
                        title = "Summarize \"${note.title.ifBlank { "note" }}\"",
                        description = "Create a quick summary",
                        confidence = 0.7f
                    )
                    suggestions += SmartSuggestion(
                        id = "s-${note.id}-tags",
                        type = SuggestionType.TAGGING,
                        title = "Suggest tags",
                        description = "Generate tags from content",
                        confidence = 0.6f
                    )
                }
                
                smartSuggestions = suggestions.distinctBy { it.title }.take(8)
            } catch (e: Exception) {
                // Swallow for now; show empty state
            } finally {
                isAnalyzing = false
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Widget Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Intelligence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Row {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF9C27B0)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    TextButton(
                        onClick = onViewAllClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Text("View All")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tab Row
            AIInsightTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content based on selected tab
            when (selectedTab) {
                AIInsightTab.SUGGESTIONS -> {
                    SmartSuggestionsContent(
                        suggestions = smartSuggestions,
                        onSuggestionClick = onSuggestionClick,
                        isAnalyzing = isAnalyzing
                    )
                }
                AIInsightTab.INSIGHTS -> {
                    ContentInsightsDisplay(
                        insights = aiInsights,
                        isAnalyzing = isAnalyzing
                    )
                }
                AIInsightTab.PATTERNS -> {
                    PatternAnalysisDisplay(
                        insights = aiInsights,
                        isAnalyzing = isAnalyzing
                    )
                }
            }
        }
    }
}

enum class AIInsightTab {
    SUGGESTIONS, INSIGHTS, PATTERNS
}

@Composable
fun AIInsightTabs(
    selectedTab: AIInsightTab,
    onTabSelected: (AIInsightTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AIInsightTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            
            Card(
                onClick = { onTabSelected(tab) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) 
                        Color(0xFF9C27B0).copy(alpha = 0.3f) 
                    else 
                        Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (tab) {
                            AIInsightTab.SUGGESTIONS -> Icons.Filled.Lightbulb
                            AIInsightTab.INSIGHTS -> Icons.Filled.Analytics
                            AIInsightTab.PATTERNS -> Icons.Filled.TrendingUp
                        },
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF9C27B0) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (tab) {
                            AIInsightTab.SUGGESTIONS -> "Smart"
                            AIInsightTab.INSIGHTS -> "Insights"
                            AIInsightTab.PATTERNS -> "Patterns"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color(0xFF9C27B0) else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SmartSuggestionsContent(
    suggestions: List<SmartSuggestion>,
    onSuggestionClick: (SmartSuggestion) -> Unit,
    isAnalyzing: Boolean
) {
    if (isAnalyzing) {
        AnalyzingState()
    } else if (suggestions.isEmpty()) {
        EmptyAIState(
            icon = Icons.Filled.Lightbulb,
            message = "No suggestions yet",
            subtitle = "AI will analyze your notes and provide smart suggestions"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(suggestions.take(4)) { suggestion ->
                SmartSuggestionCard(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
fun SentimentOverviewCard(sentimentTrend: SentimentTrend) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Mood,
                    contentDescription = null,
                    tint = getSentimentColor(sentimentTrend.averagePolarity),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sentiment Overview",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SentimentMetric("Positive", sentimentTrend.positiveCount, Color(0xFF4CAF50))
                SentimentMetric("Neutral", sentimentTrend.neutralCount, Color(0xFF9E9E9E))
                SentimentMetric("Negative", sentimentTrend.negativeCount, Color(0xFFE91E63))
            }
        }
    }
}

@Composable
fun TopTopicsCard(topics: List<TopicFrequency>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Topic,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Top Topics",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(topics) { topic ->
                    TopicChip(topic)
                }
            }
        }
    }
}

@Composable
fun ActionItemsSummaryCard(actionStats: ActionItemStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Assignment,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Action Items",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActionMetric("Total", actionStats.totalActionItems, Color(0xFF2196F3))
                ActionMetric("High Priority", actionStats.highPriorityCount, Color(0xFFE91E63))
                ActionMetric("With Dates", actionStats.withDueDates, Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun WritingPatternsCard(patterns: WritingPatterns) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Writing Patterns",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PatternMetric("Avg Length", "${patterns.averageNoteLength.toInt()} words")
                PatternMetric("Peak Hour", "${patterns.peakWritingHour}:00")
                PatternMetric("Frequency", "${String.format("%.1f", patterns.writingFrequency)} notes/day")
            }
        }
    }
}

@Composable
fun TopEntitiesCard(entities: List<EntityFrequency>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color(0xFF607D8B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Key Entities",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entities) { entity ->
                    EntityChip(entity)
                }
            }
        }
    }
}

@Composable
fun AnalyzingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color(0xFF9C27B0),
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Analyzing your notes...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Text(
            text = "AI is extracting insights and patterns",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun EmptyAIState(
    icon: ImageVector,
    message: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ContentInsightsDisplay(
    insights: ContentInsights?,
    isAnalyzing: Boolean
) {
    if (isAnalyzing) {
        AnalyzingState()
    } else if (insights == null) {
        EmptyAIState(
            icon = Icons.Filled.Analytics,
            message = "No insights available",
            subtitle = "AI will analyze your content to provide insights"
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (insights.sentimentSummary.isNotBlank()) {
                Text(
                    text = "Sentiment: ${insights.sentimentSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            if (insights.keyInsights.isNotEmpty()) {
                insights.keyInsights.forEach { line ->
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun PatternAnalysisDisplay(
    insights: ContentInsights?,
    isAnalyzing: Boolean
) {
    if (isAnalyzing) {
        AnalyzingState()
    } else if (insights == null) {
        EmptyAIState(
            icon = Icons.Filled.TrendingUp,
            message = "No patterns detected",
            subtitle = "AI will identify patterns in your writing and note-taking"
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Patterns overview",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "Detailed pattern analysis is not available in this build.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SmartSuggestionCard(
    suggestion: SmartSuggestion,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = suggestion.type.color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                suggestion.type.icon,
                contentDescription = null,
                tint = suggestion.type.color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ConfidenceIndicator(confidence = suggestion.confidence)
        }
    }
}

// Helper composables
@Composable
fun ConfidenceIndicator(confidence: Float) {
    val color = when {
        confidence >= 0.8f -> Color(0xFF4CAF50)
        confidence >= 0.6f -> Color(0xFFFF9800)
        else -> Color(0xFFE91E63)
    }
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, RoundedCornerShape(4.dp))
    )
}

@Composable
fun SentimentMetric(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ActionMetric(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun PatternMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun TopicChip(topic: TopicFrequency) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2196F3).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "${topic.topic} (${topic.frequency})",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF2196F3)
        )
    }
}

@Composable
fun EntityChip(entity: EntityFrequency) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = entity.type.color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "${entity.entity} (${entity.frequency})",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = entity.type.color
        )
    }
}

// Extension properties
val SuggestionType.color: Color
    get() = when (this) {
        SuggestionType.CONTENT_IMPROVEMENT -> Color(0xFF795548)
        SuggestionType.TAGGING -> Color(0xFF4CAF50)
        SuggestionType.ORGANIZATION -> Color(0xFF2196F3)
        SuggestionType.RELATED_NOTE -> Color(0xFF9C27B0)
        SuggestionType.PRODUCTIVITY -> Color(0xFFFF9800)
        SuggestionType.FORMATTING -> Color(0xFF3F51B5)
    }

val SuggestionType.icon: ImageVector
    get() = when (this) {
        SuggestionType.CONTENT_IMPROVEMENT -> Icons.Filled.Edit
        SuggestionType.TAGGING -> Icons.Filled.Label
        SuggestionType.ORGANIZATION -> Icons.Filled.Folder
        SuggestionType.RELATED_NOTE -> Icons.Filled.Link
        SuggestionType.PRODUCTIVITY -> Icons.Filled.Speed
        SuggestionType.FORMATTING -> Icons.Filled.FormatPaint
    }

val EntityType.color: Color
    get() = when (this) {
        EntityType.PERSON -> Color(0xFF2196F3)
        EntityType.ORGANIZATION -> Color(0xFF4CAF50)
        EntityType.LOCATION -> Color(0xFFFF9800)
        EntityType.DATE -> Color(0xFFE91E63)
        EntityType.TIME -> Color(0xFF9C27B0)
        EntityType.MONEY -> Color(0xFF4CAF50)
        EntityType.PHONE -> Color(0xFF607D8B)
        EntityType.EMAIL -> Color(0xFF795548)
        EntityType.URL -> Color(0xFF3F51B5)
        EntityType.NUMBER -> Color(0xFF9E9E9E)
        EntityType.TASK -> Color(0xFF8BC34A)
        EntityType.REMINDER -> Color(0xFFFF5722)
        EntityType.MISC -> Color(0xFF9E9E9E)
    }

private fun getSentimentColor(polarity: Float): Color {
    return when {
        polarity > 0.1f -> Color(0xFF4CAF50)
        polarity < -0.1f -> Color(0xFFE91E63)
        else -> Color(0xFF9E9E9E)
    }
}