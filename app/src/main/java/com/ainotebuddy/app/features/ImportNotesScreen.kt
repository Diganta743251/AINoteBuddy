package com.ainotebuddy.app.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity

data class ImportSource(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val supportedFormats: List<String>,
    val isAvailable: Boolean = true
)

data class ImportedNote(
    val title: String,
    val content: String,
    val createdAt: Long,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportNotesScreen(
    onBack: () -> Unit,
    onImportCompleted: (List<NoteEntity>) -> Unit
) {
    var selectedSource by remember { mutableStateOf<ImportSource?>(null) }
    var importedNotes by remember { mutableStateOf<List<ImportedNote>>(emptyList()) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0f) }
    
    val importSources = remember {
        listOf(
            ImportSource(
                id = "google_keep",
                name = "Google Keep",
                description = "Import notes from Google Keep takeout",
                icon = Icons.Filled.Note,
                supportedFormats = listOf("JSON", "HTML")
            ),
            ImportSource(
                id = "evernote",
                name = "Evernote",
                description = "Import from Evernote export files",
                icon = Icons.Filled.Description,
                supportedFormats = listOf("ENEX", "HTML")
            ),
            ImportSource(
                id = "notion",
                name = "Notion",
                description = "Import from Notion export",
                icon = Icons.Filled.Article,
                supportedFormats = listOf("Markdown", "HTML", "CSV")
            ),
            ImportSource(
                id = "onenote",
                name = "OneNote",
                description = "Import from OneNote export",
                icon = Icons.Filled.Book,
                supportedFormats = listOf("DOCX", "PDF")
            ),
            ImportSource(
                id = "simplenote",
                name = "Simplenote",
                description = "Import from Simplenote export",
                icon = Icons.Filled.TextFields,
                supportedFormats = listOf("JSON", "TXT")
            ),
            ImportSource(
                id = "markdown_files",
                name = "Markdown Files",
                description = "Import individual markdown files",
                icon = Icons.Filled.InsertDriveFile,
                supportedFormats = listOf("MD", "TXT")
            ),
            ImportSource(
                id = "text_files",
                name = "Text Files",
                description = "Import plain text files",
                icon = Icons.Filled.TextSnippet,
                supportedFormats = listOf("TXT")
            ),
            ImportSource(
                id = "csv_export",
                name = "CSV Export",
                description = "Import from CSV files",
                icon = Icons.Filled.TableChart,
                supportedFormats = listOf("CSV")
            )
        )
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Import Notes") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            }
        )
        
        when {
            selectedSource == null -> {
                // Source selection
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Choose Import Source",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    items(importSources) { source ->
                        ImportSourceCard(
                            source = source,
                            onClick = { selectedSource = source }
                        )
                    }
                }
            }
            
            importedNotes.isEmpty() && !isImporting -> {
                // File selection and import
                ImportFileSelectionScreen(
                    source = selectedSource!!,
                    onBack = { selectedSource = null },
                    onStartImport = { notes ->
                        importedNotes = notes
                    }
                )
            }
            
            isImporting -> {
                // Import progress
                ImportProgressScreen(
                    progress = importProgress,
                    onCancel = { 
                        isImporting = false
                        importProgress = 0f
                    }
                )
            }
            
            else -> {
                // Review and confirm import
                ImportReviewScreen(
                    source = selectedSource!!,
                    notes = importedNotes,
                    onBack = { 
                        importedNotes = emptyList()
                        selectedSource = null
                    },
                    onConfirmImport = { selectedNotes ->
                        val notesToImport = selectedNotes.map { importedNote ->
                            NoteEntity(
                                title = importedNote.title,
                                content = importedNote.content,
                                createdAt = importedNote.createdAt,
                                tags = importedNote.tags.joinToString(","),
                                category = importedNote.category ?: "Imported"
                            )
                        }
                        onImportCompleted(notesToImport)
                    }
                )
            }
        }
    }
}

@Composable
fun ImportSourceCard(
    source: ImportSource,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = source.isAvailable) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                source.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (source.isAvailable) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = source.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    source.supportedFormats.forEach { format ->
                        AssistChip(
                            onClick = { },
                            label = { Text(format, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImportFileSelectionScreen(
    source: ImportSource,
    onBack: () -> Unit,
    onStartImport: (List<ImportedNote>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Text(
                text = "Import from ${source.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getImportInstructions(source),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Button(
            onClick = {
                // Simulate file selection and parsing
                val sampleNotes = generateSampleImportedNotes(source)
                onStartImport(sampleNotes)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.FileOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select ${source.supportedFormats.first()} File")
        }
    }
}

@Composable
fun ImportProgressScreen(
    progress: Float,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Importing notes...",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${(progress * 100).toInt()}% complete",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
fun ImportReviewScreen(
    source: ImportSource,
    notes: List<ImportedNote>,
    onBack: () -> Unit,
    onConfirmImport: (List<ImportedNote>) -> Unit
) {
    var selectedNotes by remember { mutableStateOf(notes.map { it.copy(isSelected = true) }) }
    val selectedCount = selectedNotes.count { it.isSelected }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Review Import",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$selectedCount of ${notes.size} notes selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { onConfirmImport(selectedNotes.filter { it.isSelected }) },
                enabled = selectedCount > 0
            ) {
                Text("Import")
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(selectedNotes.withIndex().toList()) { (index, note) ->
                ImportedNoteCard(
                    note = note,
                    onToggleSelection = {
                        selectedNotes = selectedNotes.toMutableList().apply {
                            this[index] = note.copy(isSelected = !note.isSelected)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ImportedNoteCard(
    note: ImportedNote,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isSelected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = note.isSelected,
                onCheckedChange = { onToggleSelection() }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (note.content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.content.take(100) + if (note.content.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        note.tags.take(3).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getImportInstructions(source: ImportSource): String {
    return when (source.id) {
        "google_keep" -> "1. Go to Google Takeout\n2. Select Google Keep\n3. Download your archive\n4. Extract and select the JSON file"
        "evernote" -> "1. Open Evernote\n2. Go to File > Export Notes\n3. Select ENEX format\n4. Choose the exported file"
        "notion" -> "1. In Notion, go to Settings & Members\n2. Select Export content\n3. Choose Markdown & CSV format\n4. Select the exported folder"
        "onenote" -> "1. In OneNote, select File > Export\n2. Choose the notebook to export\n3. Select Word format\n4. Choose the exported files"
        "simplenote" -> "1. In Simplenote, go to Settings\n2. Select Export Notes\n3. Download the JSON file\n4. Select the downloaded file"
        "markdown_files" -> "Select one or more Markdown (.md) files from your device"
        "text_files" -> "Select one or more text (.txt) files from your device"
        "csv_export" -> "Select a CSV file with columns: title, content, created_date, tags"
        else -> "Follow the app-specific export instructions and select the exported file"
    }
}

fun generateSampleImportedNotes(source: ImportSource): List<ImportedNote> {
    // This would normally parse the actual imported file
    return listOf(
        ImportedNote(
            title = "Sample Note 1",
            content = "This is a sample imported note from ${source.name}",
            createdAt = System.currentTimeMillis() - 86400000,
            tags = listOf("imported", source.name.lowercase()),
            category = "Imported"
        ),
        ImportedNote(
            title = "Sample Note 2",
            content = "Another sample note with some content",
            createdAt = System.currentTimeMillis() - 172800000,
            tags = listOf("imported", "sample"),
            category = "Imported"
        )
    )
}