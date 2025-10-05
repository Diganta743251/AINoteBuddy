package com.ainotebuddy.app.features

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.ai.ComprehensiveAnalysisResult
import com.ainotebuddy.app.ai.EnhancedAIService
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartAssistantState(
    val isAnalyzing: Boolean = false,
    val analysisResult: ComprehensiveAnalysisResult? = null,
    val suggestions: List<String> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SmartNoteAssistantViewModel @Inject constructor(
    private val aiService: EnhancedAIService,
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SmartAssistantState())
    val state: StateFlow<SmartAssistantState> = _state.asStateFlow()
    
    fun analyzeNote(note: NoteEntity) {
        if (note.content.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isAnalyzing = true, error = null)

            try {
                val result = aiService.performComprehensiveAnalysis(note)
                _state.value = _state.value.copy(
                    analysisResult = result,
                    suggestions = result.improvementSuggestions,
                    isAnalyzing = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isAnalyzing = false,
                    error = "Analysis failed: ${e.message}"
                )
            }
        }
    }
    
    // Suggestions are derived from the comprehensive analysis result
    
    fun enhanceNote(noteId: Long, originalContent: String, instruction: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Fallback minimal enhancement if EnhancedAIService doesn't provide enhanceNote
                val enhanced = when {
                    instruction.startsWith("Tag as:") -> "$originalContent\n\nTags: ${instruction.removePrefix("Tag as:").trim()}"
                    instruction.contains("Summarize", ignoreCase = true) -> {
                        val lines = originalContent.split(". ")
                        val bullets = lines.take(3).joinToString("\n") { "• ${it.trim()}" }
                        "Summary:\n$bullets"
                    }
                    instruction.contains("Expand", ignoreCase = true) -> "$originalContent\n\n[TODO: Add more details here]"
                    else -> "$originalContent\n\n$instruction"
                }
                onResult(enhanced)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Enhancement error: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartNoteAssistant(
    note: NoteEntity,
    onNoteUpdate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmartNoteAssistantViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Auto-analyze when note content changes
    LaunchedEffect(note.content) {
        if (note.content.isNotBlank()) {
            viewModel.analyzeNote(note)
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error handling
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
@Composable
private fun AnalysisResultCard(analysis: ComprehensiveAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (analysis.recommendedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Suggested Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    analysis.recommendedTags.take(3).forEach { tag ->
                        AssistChip(onClick = { }, label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (analysis.sentiment.sentiment) {
                        com.ainotebuddy.app.ai.Sentiment.POSITIVE -> Icons.Filled.SentimentSatisfied
                        com.ainotebuddy.app.ai.Sentiment.NEGATIVE -> Icons.Filled.SentimentDissatisfied
                        com.ainotebuddy.app.ai.Sentiment.NEUTRAL -> Icons.Filled.SentimentNeutral
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (analysis.sentiment.sentiment) {
                        com.ainotebuddy.app.ai.Sentiment.POSITIVE -> MaterialTheme.colorScheme.tertiary
                        com.ainotebuddy.app.ai.Sentiment.NEGATIVE -> MaterialTheme.colorScheme.error
                        com.ainotebuddy.app.ai.Sentiment.NEUTRAL -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sentiment: ${analysis.sentiment.sentiment.name.lowercase().replaceFirstChar { it.titlecase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (analysis.keyPhrases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Key Phrases",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = analysis.keyPhrases.take(5).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (analysis.actionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Action Items: ${analysis.actionItems.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reading time: ${analysis.readingTimeMinutes} min • Complexity: ${analysis.complexityScore}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
