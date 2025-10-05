package com.ainotebuddy.app.features

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

data class ClipboardItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: ClipboardItemType = ClipboardItemType.TEXT
)

enum class ClipboardItemType {
    TEXT, URL, EMAIL, PHONE
}

class ClipboardManagerService(private val context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val _clipboardHistory = mutableStateListOf<ClipboardItem>()
    val clipboardHistory: List<ClipboardItem> = _clipboardHistory
    
    init {
        // Monitor clipboard changes
        clipboardManager.addPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text.length < 10000) { // Limit size
                    addToHistory(text)
                }
            }
        }
    }
    
    private fun addToHistory(content: String) {
        // Don't add duplicates or if it's already the most recent
        if (_clipboardHistory.firstOrNull()?.content != content) {
            val type = detectContentType(content)
            _clipboardHistory.add(0, ClipboardItem(content = content, type = type))
            
            // Keep only last 50 items
            if (_clipboardHistory.size > 50) {
                _clipboardHistory.removeAt(_clipboardHistory.size - 1)
            }
        }
    }
    
    private fun detectContentType(content: String): ClipboardItemType {
        return when {
            content.startsWith("http://") || content.startsWith("https://") -> ClipboardItemType.URL
            content.contains("@") && content.contains(".") -> ClipboardItemType.EMAIL
            content.matches(Regex("^[+]?[0-9\\s\\-()]+$")) -> ClipboardItemType.PHONE
            else -> ClipboardItemType.TEXT
        }
    }
    
    fun copyToClipboard(content: String) {
        val clip = ClipData.newPlainText("AINoteBuddy", content)
        clipboardManager.setPrimaryClip(clip)
    }
    
    fun clearHistory() {
        _clipboardHistory.clear()
    }
    
    fun removeItem(item: ClipboardItem) {
        _clipboardHistory.remove(item)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardManagerScreen(
    onBack: () -> Unit,
    onAddToNote: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardService = remember { ClipboardManagerService(context) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Clipboard Manager") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Filled.Clear, "Clear History")
                }
            }
        )
        
        if (clipboardService.clipboardHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No clipboard history",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Copy some text to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clipboardService.clipboardHistory) { item ->
                    ClipboardItemCard(
                        item = item,
                        onCopy = { clipboardService.copyToClipboard(item.content) },
                        onAddToNote = { onAddToNote(item.content) },
                        onDelete = { clipboardService.removeItem(item) }
                    )
                }
            }
        }
    }
    
    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Clipboard History") },
            text = { Text("Are you sure you want to clear all clipboard history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardService.clearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ClipboardItemCard(
    item: ClipboardItem,
    onCopy: () -> Unit,
    onAddToNote: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when (item.type) {
                            ClipboardItemType.URL -> Icons.Filled.Link
                            ClipboardItemType.EMAIL -> Icons.Filled.Email
                            ClipboardItemType.PHONE -> Icons.Filled.Phone
                            ClipboardItemType.TEXT -> Icons.Filled.TextFields
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
                
                Button(
                    onClick = onAddToNote,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.NoteAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Note")
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}