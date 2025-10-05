package com.ainotebuddy.app.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CodeLanguage(val displayName: String, val extension: String) {
    KOTLIN("Kotlin", "kt"),
    JAVA("Java", "java"),
    PYTHON("Python", "py"),
    JAVASCRIPT("JavaScript", "js"),
    TYPESCRIPT("TypeScript", "ts"),
    HTML("HTML", "html"),
    CSS("CSS", "css"),
    JSON("JSON", "json"),
    XML("XML", "xml"),
    SQL("SQL", "sql"),
    MARKDOWN("Markdown", "md"),
    PLAIN_TEXT("Plain Text", "txt")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    content: String,
    language: CodeLanguage = CodeLanguage.PLAIN_TEXT,
    onContentChange: (String) -> Unit,
    onLanguageChange: (CodeLanguage) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var showLanguageSelector by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Code Editor") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                TextButton(onClick = { showLanguageSelector = true }) {
                    Text(language.displayName)
                    Icon(Icons.Filled.ArrowDropDown, null)
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Filled.Save, "Save")
                }
            }
        )
        
        // Language selector
        if (showLanguageSelector) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CodeLanguage.values()) { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = {
                                onLanguageChange(lang)
                                showLanguageSelector = false
                            },
                            label = { Text(lang.displayName) }
                        )
                    }
                }
            }
        }
        
        // Code editor
        SyntaxHighlightedEditor(
            content = content,
            language = language,
            onContentChange = onContentChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SyntaxHighlightedEditor(
    content: String,
    language: CodeLanguage,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightedText = remember(content, language) {
        highlightSyntax(content, language)
    }
    
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = Color.White,
            lineHeight = 20.sp
        ),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (content.isEmpty()) {
                    Text(
                        text = "Start typing your ${language.displayName.lowercase()} code here...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    )
                }
                // Show highlighted text as overlay (simplified approach)
                if (content.isNotEmpty()) {
                    Text(
                        text = highlightedText,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

fun highlightSyntax(code: String, language: CodeLanguage): AnnotatedString {
    return buildAnnotatedString {
        val keywords = getKeywords(language)
        val lines = code.split("\n")
        
        lines.forEachIndexed { lineIndex, line ->
            if (lineIndex > 0) append("\n")
            
            var currentIndex = 0
            while (currentIndex < line.length) {
                when {
                    // Comments
                    line.substring(currentIndex).startsWith("//") -> {
                        withStyle(SpanStyle(color = Color(0xFF6A9955))) {
                            append(line.substring(currentIndex))
                        }
                        break
                    }
                    line.substring(currentIndex).startsWith("/*") -> {
                        val endIndex = line.indexOf("*/", currentIndex + 2)
                        if (endIndex != -1) {
                            withStyle(SpanStyle(color = Color(0xFF6A9955))) {
                                append(line.substring(currentIndex, endIndex + 2))
                            }
                            currentIndex = endIndex + 2
                        } else {
                            withStyle(SpanStyle(color = Color(0xFF6A9955))) {
                                append(line.substring(currentIndex))
                            }
                            break
                        }
                    }
                    // Strings
                    line[currentIndex] == '"' -> {
                        val endIndex = line.indexOf('"', currentIndex + 1)
                        if (endIndex != -1) {
                            withStyle(SpanStyle(color = Color(0xFFCE9178))) {
                                append(line.substring(currentIndex, endIndex + 1))
                            }
                            currentIndex = endIndex + 1
                        } else {
                            append(line[currentIndex])
                            currentIndex++
                        }
                    }
                    line[currentIndex] == '\'' -> {
                        val endIndex = line.indexOf('\'', currentIndex + 1)
                        if (endIndex != -1) {
                            withStyle(SpanStyle(color = Color(0xFFCE9178))) {
                                append(line.substring(currentIndex, endIndex + 1))
                            }
                            currentIndex = endIndex + 1
                        } else {
                            append(line[currentIndex])
                            currentIndex++
                        }
                    }
                    // Numbers
                    line[currentIndex].isDigit() -> {
                        var endIndex = currentIndex
                        while (endIndex < line.length && (line[endIndex].isDigit() || line[endIndex] == '.')) {
                            endIndex++
                        }
                        withStyle(SpanStyle(color = Color(0xFFB5CEA8))) {
                            append(line.substring(currentIndex, endIndex))
                        }
                        currentIndex = endIndex
                    }
                    // Keywords
                    line[currentIndex].isLetter() -> {
                        var endIndex = currentIndex
                        while (endIndex < line.length && (line[endIndex].isLetterOrDigit() || line[endIndex] == '_')) {
                            endIndex++
                        }
                        val word = line.substring(currentIndex, endIndex)
                        if (keywords.contains(word)) {
                            withStyle(SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                        currentIndex = endIndex
                    }
                    else -> {
                        append(line[currentIndex])
                        currentIndex++
                    }
                }
            }
        }
    }
}

fun getKeywords(language: CodeLanguage): Set<String> {
    return when (language) {
        CodeLanguage.KOTLIN -> setOf(
            "class", "fun", "val", "var", "if", "else", "when", "for", "while", "do",
            "try", "catch", "finally", "throw", "return", "break", "continue",
            "object", "interface", "abstract", "open", "final", "override",
            "private", "protected", "public", "internal", "companion",
            "import", "package", "as", "is", "in", "out", "by", "where",
            "suspend", "inline", "crossinline", "noinline", "reified"
        )
        CodeLanguage.JAVA -> setOf(
            "class", "interface", "enum", "extends", "implements", "package", "import",
            "public", "private", "protected", "static", "final", "abstract", "synchronized",
            "volatile", "transient", "native", "strictfp", "if", "else", "switch", "case",
            "default", "for", "while", "do", "break", "continue", "return", "try", "catch",
            "finally", "throw", "throws", "new", "this", "super", "null", "true", "false"
        )
        CodeLanguage.PYTHON -> setOf(
            "def", "class", "if", "elif", "else", "for", "while", "break", "continue",
            "return", "try", "except", "finally", "raise", "with", "as", "import", "from",
            "global", "nonlocal", "lambda", "yield", "assert", "del", "pass", "and", "or",
            "not", "in", "is", "True", "False", "None"
        )
        CodeLanguage.JAVASCRIPT -> setOf(
            "function", "var", "let", "const", "if", "else", "switch", "case", "default",
            "for", "while", "do", "break", "continue", "return", "try", "catch", "finally",
            "throw", "new", "this", "typeof", "instanceof", "in", "delete", "void",
            "true", "false", "null", "undefined", "class", "extends", "super", "static",
            "async", "await", "yield", "import", "export", "from", "default"
        )
        CodeLanguage.HTML -> setOf(
            "html", "head", "body", "title", "meta", "link", "script", "style",
            "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6", "a", "img",
            "ul", "ol", "li", "table", "tr", "td", "th", "form", "input", "button"
        )
        CodeLanguage.CSS -> setOf(
            "color", "background", "font", "margin", "padding", "border", "width", "height",
            "display", "position", "top", "left", "right", "bottom", "float", "clear",
            "text-align", "font-size", "font-weight", "line-height", "opacity", "z-index"
        )
        CodeLanguage.SQL -> setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP",
            "ALTER", "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "NOT", "NULL", "UNIQUE", "DEFAULT", "CHECK",
            "CONSTRAINT", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "ON",
            "GROUP", "BY", "HAVING", "ORDER", "ASC", "DESC", "LIMIT", "OFFSET"
        )
        else -> emptySet()
    }
}