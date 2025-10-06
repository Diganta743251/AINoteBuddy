package com.ainotebuddy.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

// Advanced editing tool data classes
data class TextAnalysis(
    val wordCount: Int,
    val characterCount: Int,
    val paragraphCount: Int,
    val sentenceCount: Int,
    val averageWordsPerSentence: Double,
    val readingTimeMinutes: Int,
    val complexity: TextComplexity,
    val sentiment: TextSentiment,
    val mostUsedWords: List<WordFrequency>
)

data class WordFrequency(
    val word: String,
    val count: Int,
    val percentage: Double
)

enum class TextComplexity {
    SIMPLE, MODERATE, COMPLEX, VERY_COMPLEX
}

enum class TextSentiment {
    POSITIVE, NEGATIVE, NEUTRAL
}

data class SmartSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: SuggestionAction
)

enum class SuggestionAction {
    FIX_GRAMMAR, IMPROVE_CLARITY, ADD_STRUCTURE, 
    EXPAND_CONTENT, SUMMARIZE, CHANGE_TONE
}

data class TypingStats(
    val wordsPerMinute: Int,
    val accuracy: Double,
    val totalKeystrokes: Int,
    val sessionStartTime: Long,
    val isActive: Boolean
)

@Composable
fun AdvancedEditingToolsPanel(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTool by remember { mutableStateOf(EditingTool.NONE) }
    var showAnalysis by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showSmartTools by remember { mutableStateOf(false) }
    
    val analysis = remember(text) { analyzeText(text) }
    val suggestions = remember(text) { generateSmartSuggestions(text) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Tool selection tabs
            EditingToolTabs(
                selectedTool = selectedTool,
                onToolSelect = { selectedTool = it },
                onShowAnalysis = { showAnalysis = true },
                onShowTemplates = { showTemplates = true },
                onShowSmartTools = { showSmartTools = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tool content based on selection
            when (selectedTool) {
                EditingTool.FORMATTING -> FormattingToolsPanel(
                    text = text,
                    onTextChange = onTextChange
                )
                EditingTool.STRUCTURE -> StructureToolsPanel(
                    text = text,
                    onTextChange = onTextChange
                )
                EditingTool.PRODUCTIVITY -> ProductivityToolsPanel(
                    text = text,
                    onTextChange = onTextChange
                )
                EditingTool.COLLABORATION -> CollaborationToolsPanel(
                    text = text
                )
                EditingTool.NONE -> {
                    // Quick stats display
                    QuickStatsDisplay(analysis = analysis)
                }
            }
        }
    }
    
    // Analysis dialog
    if (showAnalysis) {
        TextAnalysisDialog(
            analysis = analysis,
            onDismiss = { showAnalysis = false }
        )
    }
    
    // Templates dialog
    if (showTemplates) {
        TemplatesDialog(
            onTemplateSelect = { template ->
                onTextChange(template)
                showTemplates = false
            },
            onDismiss = { showTemplates = false }
        )
    }
    
    // Smart tools dialog
    if (showSmartTools) {
        SmartToolsDialog(
            suggestions = suggestions,
            onApplySuggestion = { suggestion ->
                // Apply the suggestion
                applySuggestion(suggestion, text, onTextChange)
                showSmartTools = false
            },
            onDismiss = { showSmartTools = false }
        )
    }
}

enum class EditingTool {
    NONE, FORMATTING, STRUCTURE, PRODUCTIVITY, COLLABORATION
}

@Composable
fun EditingToolTabs(
    selectedTool: EditingTool,
    onToolSelect: (EditingTool) -> Unit,
    onShowAnalysis: () -> Unit,
    onShowTemplates: () -> Unit,
    onShowSmartTools: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main tool tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(EditingTool.values().filter { it != EditingTool.NONE }) { tool ->
                FilterChip(
                    onClick = { onToolSelect(if (selectedTool == tool) EditingTool.NONE else tool) },
                    label = { Text(tool.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    selected = selectedTool == tool,
                    leadingIcon = {
                        Icon(
                            imageVector = when (tool) {
                                EditingTool.FORMATTING -> Icons.Default.FormatBold
                                EditingTool.STRUCTURE -> Icons.Default.AccountTree
                                EditingTool.PRODUCTIVITY -> Icons.Default.Speed
                                EditingTool.COLLABORATION -> Icons.Default.People
                                else -> Icons.Default.Edit
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onShowAnalysis) {
                Icon(Icons.Default.Analytics, contentDescription = "Analysis")
            }
            IconButton(onClick = onShowTemplates) {
                Icon(Icons.Default.Description, contentDescription = "Templates")
            }
            IconButton(onClick = onShowSmartTools) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Smart Tools")
            }
        }
    }
}

@Composable
fun FormattingToolsPanel(
    text: String,
    onTextChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Formatting Tools",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Quick formatting options
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getFormattingOptions()) { option ->
                OutlinedButton(
                    onClick = { applyFormatting(option, text, onTextChange) },
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(option.label, fontSize = 12.sp)
                }
            }
        }
        
        // Text case options
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("UPPERCASE", "lowercase", "Capitalize").forEach { caseType ->
                AssistChip(
                    onClick = { applyCaseTransformation(caseType, text, onTextChange) },
                    label = { Text(caseType, fontSize = 10.sp) }
                )
            }
        }
    }
}

@Composable
fun StructureToolsPanel(
    text: String,
    onTextChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Structure Tools",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Structure suggestions
        LazyColumn(
            modifier = Modifier.height(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getStructureSuggestions(text)) { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTextChange(suggestion.content) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = suggestion.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggestion.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = suggestion.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductivityToolsPanel(
    text: String,
    onTextChange: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Productivity Tools",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Quick actions
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getProductivityActions()) { action ->
                ElevatedButton(
                    onClick = { 
                        when (action.type) {
                            ProductivityActionType.WORD_COUNT -> {
                                // Show word count
                            }
                            ProductivityActionType.DUPLICATE_LINE -> {
                                onTextChange(duplicateLastLine(text))
                            }
                            ProductivityActionType.REMOVE_EMPTY_LINES -> {
                                onTextChange(removeEmptyLines(text))
                            }
                            ProductivityActionType.COPY_TO_CLIPBOARD -> {
                                clipboardManager.setText(AnnotatedString(text))
                            }
                            ProductivityActionType.SORT_LINES -> {
                                onTextChange(sortLines(text))
                            }
                        }
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(action.label, fontSize = 11.sp)
                }
            }
        }
        
        // Text transformations
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Text Transformations",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Remove URLs", "Extract URLs", "Number Lines").forEach { transform ->
                        AssistChip(
                            onClick = { 
                                val transformed = when (transform) {
                                    "Remove URLs" -> removeUrls(text)
                                    "Extract URLs" -> extractUrls(text)
                                    "Number Lines" -> numberLines(text)
                                    else -> text
                                }
                                onTextChange(transformed)
                            },
                            label = { Text(transform, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollaborationToolsPanel(
    text: String
) {
    val context = LocalContext.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Collaboration Tools",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Sharing options
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getSharingOptions()) { option ->
                Card(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable {
                            // Handle sharing
                            when (option.type) {
                                SharingType.EMAIL -> shareViaEmail(context, text)
                                SharingType.LINK -> generateShareLink(text)
                                SharingType.QR_CODE -> generateQRCode(text)
                                SharingType.EXPORT -> exportAsFile(context, text)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Version history (placeholder)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Recent Changes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Modified 2 minutes ago\n• Added formatting 5 minutes ago\n• Created note 1 hour ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickStatsDisplay(analysis: TextAnalysis) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = "Words",
            value = analysis.wordCount.toString(),
            icon = Icons.Default.TextFields
        )
        StatItem(
            label = "Characters",
            value = analysis.characterCount.toString(),
            icon = Icons.Default.Spellcheck
        )
        StatItem(
            label = "Reading",
            value = "${analysis.readingTimeMinutes}m",
            icon = Icons.Default.Schedule
        )
        StatItem(
            label = "Complexity",
            value = analysis.complexity.name.lowercase(),
            icon = Icons.Default.TrendingUp
        )
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TextAnalysisDialog(
    analysis: TextAnalysis,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Text Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Detailed stats
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalysisRow("Words", analysis.wordCount.toString())
                    AnalysisRow("Characters", analysis.characterCount.toString())
                    AnalysisRow("Paragraphs", analysis.paragraphCount.toString())
                    AnalysisRow("Sentences", analysis.sentenceCount.toString())
                    AnalysisRow("Avg words/sentence", "%.1f".format(analysis.averageWordsPerSentence))
                    AnalysisRow("Reading time", "${analysis.readingTimeMinutes} minutes")
                    AnalysisRow("Complexity", analysis.complexity.name.lowercase())
                    AnalysisRow("Sentiment", analysis.sentiment.name.lowercase())
                }
                
                // Most used words
                if (analysis.mostUsedWords.isNotEmpty()) {
                    Text(
                        text = "Most Used Words",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    LazyColumn(
                        modifier = Modifier.height(100.dp)
                    ) {
                        items(analysis.mostUsedWords.take(5)) { wordFreq ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(wordFreq.word)
                                Text("${wordFreq.count} (${wordFreq.percentage.toInt()}%)")
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Data classes and helper functions
data class FormattingOption(
    val label: String,
    val icon: ImageVector,
    val action: String
)

data class StructureSuggestion(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val content: String
)

data class ProductivityAction(
    val label: String,
    val icon: ImageVector,
    val type: ProductivityActionType
)

enum class ProductivityActionType {
    WORD_COUNT, DUPLICATE_LINE, REMOVE_EMPTY_LINES, COPY_TO_CLIPBOARD, SORT_LINES
}

data class SharingOption(
    val label: String,
    val icon: ImageVector,
    val type: SharingType
)

enum class SharingType {
    EMAIL, LINK, QR_CODE, EXPORT
}

// Helper functions (simplified implementations)
private fun analyzeText(text: String): TextAnalysis {
    val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val sentences = text.split("[.!?]+".toRegex()).filter { it.isNotBlank() }
    val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
    
    val wordCount = words.size
    val characterCount = text.length
    val paragraphCount = paragraphs.size
    val sentenceCount = sentences.size
    val avgWordsPerSentence = if (sentenceCount > 0) wordCount.toDouble() / sentenceCount else 0.0
    val readingTime = (wordCount / 200).coerceAtLeast(1) // Assume 200 words per minute
    
    val complexity = when {
        avgWordsPerSentence < 10 -> TextComplexity.SIMPLE
        avgWordsPerSentence < 15 -> TextComplexity.MODERATE
        avgWordsPerSentence < 20 -> TextComplexity.COMPLEX
        else -> TextComplexity.VERY_COMPLEX
    }
    
    val wordFreq = words.groupBy { it.lowercase() }
        .map { (word, occurrences) ->
            WordFrequency(
                word = word,
                count = occurrences.size,
                percentage = (occurrences.size.toDouble() / wordCount) * 100
            )
        }
        .sortedByDescending { it.count }
    
    return TextAnalysis(
        wordCount = wordCount,
        characterCount = characterCount,
        paragraphCount = paragraphCount,
        sentenceCount = sentenceCount,
        averageWordsPerSentence = avgWordsPerSentence,
        readingTimeMinutes = readingTime,
        complexity = complexity,
        sentiment = TextSentiment.NEUTRAL, // Simplified
        mostUsedWords = wordFreq
    )
}

private fun generateSmartSuggestions(text: String): List<SmartSuggestion> {
    return listOf(
        SmartSuggestion(
            "grammar",
            "Check Grammar",
            "Review and fix grammatical errors",
            Icons.Default.Spellcheck,
            SuggestionAction.FIX_GRAMMAR
        ),
        SmartSuggestion(
            "clarity",
            "Improve Clarity",
            "Make your text clearer and more concise",
            Icons.Default.Lightbulb,
            SuggestionAction.IMPROVE_CLARITY
        ),
        SmartSuggestion(
            "structure",
            "Add Structure",
            "Organize content with headings and lists",
            Icons.Default.List,
            SuggestionAction.ADD_STRUCTURE
        )
    )
}

private fun getFormattingOptions(): List<FormattingOption> {
    return listOf(
        FormattingOption("Bold", Icons.Default.FormatBold, "bold"),
        FormattingOption("Italic", Icons.Default.FormatItalic, "italic"),
        FormattingOption("Header", Icons.Default.Title, "header"),
        FormattingOption("List", Icons.Default.FormatListBulleted, "list")
    )
}

private fun getStructureSuggestions(text: String): List<StructureSuggestion> {
    return listOf(
        StructureSuggestion(
            "Add Introduction",
            "Start with a clear introduction",
            Icons.Default.PlayArrow,
            "# Introduction\n\n$text"
        ),
        StructureSuggestion(
            "Create Outline",
            "Structure with bullet points",
            Icons.Default.List,
            text.split("\n").joinToString("\n") { "• $it" }
        )
    )
}

private fun getProductivityActions(): List<ProductivityAction> {
    return listOf(
        ProductivityAction("Count", Icons.Default.Numbers, ProductivityActionType.WORD_COUNT),
        ProductivityAction("Duplicate", Icons.Default.ContentCopy, ProductivityActionType.DUPLICATE_LINE),
        ProductivityAction("Clean", Icons.Default.CleaningServices, ProductivityActionType.REMOVE_EMPTY_LINES),
        ProductivityAction("Copy", Icons.Default.ContentPaste, ProductivityActionType.COPY_TO_CLIPBOARD),
        ProductivityAction("Sort", Icons.Default.Sort, ProductivityActionType.SORT_LINES)
    )
}

private fun getSharingOptions(): List<SharingOption> {
    return listOf(
        SharingOption("Email", Icons.Default.Email, SharingType.EMAIL),
        SharingOption("Link", Icons.Default.Link, SharingType.LINK),
        SharingOption("QR Code", Icons.Default.QrCode, SharingType.QR_CODE),
        SharingOption("Export", Icons.Default.FileDownload, SharingType.EXPORT)
    )
}

// Text transformation functions (simplified implementations)
private fun applyFormatting(option: FormattingOption, text: String, onTextChange: (String) -> Unit) {
    val formatted = when (option.action) {
        "bold" -> "**$text**"
        "italic" -> "*$text*"
        "header" -> "# $text"
        "list" -> text.split("\n").joinToString("\n") { "• $it" }
        else -> text
    }
    onTextChange(formatted)
}

private fun applyCaseTransformation(caseType: String, text: String, onTextChange: (String) -> Unit) {
    val transformed = when (caseType) {
        "UPPERCASE" -> text.uppercase()
        "lowercase" -> text.lowercase()
        "Capitalize" -> text.split(" ").joinToString(" ") { 
            it.lowercase().replaceFirstChar { char -> char.uppercase() } 
        }
        else -> text
    }
    onTextChange(transformed)
}

private fun duplicateLastLine(text: String): String {
    val lines = text.split("\n")
    return if (lines.isNotEmpty()) {
        text + "\n" + lines.last()
    } else text
}

private fun removeEmptyLines(text: String): String {
    return text.split("\n").filter { it.isNotBlank() }.joinToString("\n")
}

private fun sortLines(text: String): String {
    return text.split("\n").sorted().joinToString("\n")
}

private fun removeUrls(text: String): String {
    return text.replace(Regex("https?://\\S+"), "")
}

private fun extractUrls(text: String): String {
    val urls = Regex("https?://\\S+").findAll(text).map { it.value }
    return urls.joinToString("\n")
}

private fun numberLines(text: String): String {
    return text.split("\n").mapIndexed { index, line ->
        "${index + 1}. $line"
    }.joinToString("\n")
}

private fun applySuggestion(suggestion: SmartSuggestion, text: String, onTextChange: (String) -> Unit) {
    // Simplified suggestion application
    when (suggestion.action) {
        SuggestionAction.FIX_GRAMMAR -> onTextChange(text) // Placeholder
        SuggestionAction.IMPROVE_CLARITY -> onTextChange(text) // Placeholder
        SuggestionAction.ADD_STRUCTURE -> onTextChange("# Main Topic\n\n$text")
        else -> onTextChange(text)
    }
}

// Placeholder functions for sharing
private fun shareViaEmail(context: android.content.Context, text: String) {
    // Implement email sharing
}

private fun generateShareLink(text: String): String {
    // Implement share link generation
    return "https://example.com/shared/note"
}

private fun generateQRCode(text: String): String {
    // Implement QR code generation
    return "QR code data"
}

private fun exportAsFile(context: android.content.Context, text: String) {
    // Implement file export
}

// Template and smart tools dialogs (simplified)
@Composable
fun TemplatesDialog(
    onTemplateSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Templates", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                // Template list would go here
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun SmartToolsDialog(
    suggestions: List<SmartSuggestion>,
    onApplySuggestion: (SmartSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Smart Tools", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn {
                    items(suggestions) { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onApplySuggestion(suggestion) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = suggestion.icon,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = suggestion.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = suggestion.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}