package com.ainotebuddy.app.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MarkdownMode {
    EDIT, PREVIEW, SPLIT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorScreen(
    content: String,
    onContentChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var mode by remember { mutableStateOf(MarkdownMode.EDIT) }
    var showSyntaxHelp by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Markdown Editor") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = { showSyntaxHelp = true }) {
                    Icon(Icons.Filled.Help, "Syntax Help")
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Filled.Save, "Save")
                }
            }
        )
        
        // Mode selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarkdownMode.values().forEach { markdownMode ->
                FilterChip(
                    selected = mode == markdownMode,
                    onClick = { mode = markdownMode },
                    label = { 
                        Text(when (markdownMode) {
                            MarkdownMode.EDIT -> "Edit"
                            MarkdownMode.PREVIEW -> "Preview"
                            MarkdownMode.SPLIT -> "Split"
                        })
                    },
                    leadingIcon = {
                        Icon(
                            when (markdownMode) {
                                MarkdownMode.EDIT -> Icons.Filled.Edit
                                MarkdownMode.PREVIEW -> Icons.Filled.Visibility
                                MarkdownMode.SPLIT -> Icons.Filled.ViewColumn
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
        
        // Markdown toolbar
        MarkdownToolbar(
            onInsertSyntax = { syntax ->
                onContentChange(content + syntax)
            }
        )
        
        Divider()
        
        // Content area
        when (mode) {
            MarkdownMode.EDIT -> {
                MarkdownEditor(
                    content = content,
                    onContentChange = onContentChange,
                    modifier = Modifier.weight(1f)
                )
            }
            MarkdownMode.PREVIEW -> {
                MarkdownPreview(
                    content = content,
                    modifier = Modifier.weight(1f)
                )
            }
            MarkdownMode.SPLIT -> {
                Row(modifier = Modifier.weight(1f)) {
                    MarkdownEditor(
                        content = content,
                        onContentChange = onContentChange,
                        modifier = Modifier.weight(1f)
                    )
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )
                    MarkdownPreview(
                        content = content,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    
    // Syntax help dialog
    if (showSyntaxHelp) {
        MarkdownSyntaxHelpDialog(
            onDismiss = { showSyntaxHelp = false }
        )
    }
}

@Composable
fun MarkdownToolbar(
    onInsertSyntax: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(8.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolbarButton(
                    icon = Icons.Filled.FormatBold,
                    tooltip = "Bold",
                    onClick = { onInsertSyntax("**bold text**") }
                )
                ToolbarButton(
                    icon = Icons.Filled.FormatItalic,
                    tooltip = "Italic",
                    onClick = { onInsertSyntax("*italic text*") }
                )
                ToolbarButton(
                    icon = Icons.Filled.FormatUnderlined,
                    tooltip = "Strikethrough",
                    onClick = { onInsertSyntax("~~strikethrough~~") }
                )
                ToolbarButton(
                    icon = Icons.Filled.Code,
                    tooltip = "Inline Code",
                    onClick = { onInsertSyntax("`code`") }
                )
                ToolbarButton(
                    icon = Icons.Filled.Title,
                    tooltip = "Heading",
                    onClick = { onInsertSyntax("# Heading") }
                )
                ToolbarButton(
                    icon = Icons.Filled.FormatListBulleted,
                    tooltip = "Bullet List",
                    onClick = { onInsertSyntax("\n- List item") }
                )
                ToolbarButton(
                    icon = Icons.Filled.FormatListNumbered,
                    tooltip = "Numbered List",
                    onClick = { onInsertSyntax("\n1. List item") }
                )
                ToolbarButton(
                    icon = Icons.Filled.Link,
                    tooltip = "Link",
                    onClick = { onInsertSyntax("[link text](url)") }
                )
                ToolbarButton(
                    icon = Icons.Filled.Image,
                    tooltip = "Image",
                    onClick = { onInsertSyntax("![alt text](image-url)") }
                )
                ToolbarButton(
                    icon = Icons.Filled.FormatQuote,
                    tooltip = "Quote",
                    onClick = { onInsertSyntax("\n> Quote") }
                )
            }
        }
    }
}

@Composable
fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            icon,
            contentDescription = tooltip,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (content.isEmpty()) {
                    Text(
                        text = "Start typing your markdown here...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (content.isEmpty()) {
                    Text(
                        text = "Preview will appear here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    MarkdownText(content = content)
                }
            }
        }
    }
}

@Composable
fun MarkdownText(content: String) {
    val lines = content.split("\n")
    
    lines.forEach { line ->
        when {
            line.startsWith("# ") -> {
                Text(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            line.startsWith("## ") -> {
                Text(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            line.startsWith("### ") -> {
                Text(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            line.startsWith("> ") -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = line.removePrefix("> "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                Row {
                    Text("• ", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = line.removePrefix("- ").removePrefix("* "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            line.matches(Regex("\\d+\\. .*")) -> {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            line.startsWith("```") -> {
                // Code block start/end - would need more complex parsing
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            line.isNotBlank() -> {
                Text(
                    text = parseInlineMarkdown(line),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    var currentIndex = 0
    val length = text.length
    
    while (currentIndex < length) {
        when {
            // Bold **text**
            text.substring(currentIndex).startsWith("**") -> {
                val endIndex = text.indexOf("**", currentIndex + 2)
                if (endIndex != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(currentIndex + 2, endIndex))
                    }
                    currentIndex = endIndex + 2
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            // Italic *text*
            text.substring(currentIndex).startsWith("*") -> {
                val endIndex = text.indexOf("*", currentIndex + 1)
                if (endIndex != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(currentIndex + 1, endIndex))
                    }
                    currentIndex = endIndex + 1
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            // Strikethrough ~~text~~
            text.substring(currentIndex).startsWith("~~") -> {
                val endIndex = text.indexOf("~~", currentIndex + 2)
                if (endIndex != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(currentIndex + 2, endIndex))
                    }
                    currentIndex = endIndex + 2
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            // Inline code `text`
            text.substring(currentIndex).startsWith("`") -> {
                val endIndex = text.indexOf("`", currentIndex + 1)
                if (endIndex != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.2f)
                        )
                    ) {
                        append(text.substring(currentIndex + 1, endIndex))
                    }
                    currentIndex = endIndex + 1
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            else -> {
                append(text[currentIndex])
                currentIndex++
            }
        }
    }
}

@Composable
fun MarkdownSyntaxHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Markdown Syntax Help") },
        text = {
            LazyColumn {
                item {
                    SyntaxHelpItem("# Heading 1", "Large heading")
                    SyntaxHelpItem("## Heading 2", "Medium heading")
                    SyntaxHelpItem("### Heading 3", "Small heading")
                    SyntaxHelpItem("**bold text**", "Bold text")
                    SyntaxHelpItem("*italic text*", "Italic text")
                    SyntaxHelpItem("~~strikethrough~~", "Strikethrough text")
                    SyntaxHelpItem("`inline code`", "Inline code")
                    SyntaxHelpItem("```\ncode block\n```", "Code block")
                    SyntaxHelpItem("> Quote", "Blockquote")
                    SyntaxHelpItem("- List item", "Bullet list")
                    SyntaxHelpItem("1. List item", "Numbered list")
                    SyntaxHelpItem("[link text](url)", "Link")
                    SyntaxHelpItem("![alt text](image-url)", "Image")
                    SyntaxHelpItem("---", "Horizontal rule")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SyntaxHelpItem(syntax: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = syntax,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}