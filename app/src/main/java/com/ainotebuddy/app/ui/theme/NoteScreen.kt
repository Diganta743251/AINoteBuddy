package com.ainotebuddy.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.viewmodel.NoteViewModel
import io.noties.markwon.Markwon
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import android.widget.TextView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import com.ainotebuddy.app.AINoteBuddyApplication
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun NoteScreen(
    viewModel: NoteViewModel
) {
    val notes by viewModel.notes.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val AUTO_SYNC_KEY = booleanPreferencesKey("auto_sync_enabled")
    var autoSyncEnabled by remember { mutableStateOf(false) }
    // LaunchedEffect(Unit) {
    //     autoSyncEnabled = AINoteBuddyApplication.dataStore.data.first()[AUTO_SYNC_KEY] == true
    // }

    Column(modifier = Modifier.padding(16.dp)) {
        // Simple header
        Text(
            text = "My Notes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Button(
            onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    scope.launch {
                        viewModel.createQuickNote(title, content, context)
                        title = ""
                        content = ""
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Save Note")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(notes.size) { index ->
                NoteItem(notes[index])
            }
        }
    }
}

@Composable
fun NoteItem(note: NoteEntity, onNoteLinkClick: (String) -> Unit = {}, backlinks: List<NoteEntity> = emptyList(), onBacklinkClick: (NoteEntity) -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = note.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            // Replace MarkdownWithNoteLinks and Markdown usage with a simple Text composable for now
            // MarkdownWithNoteLinks(content = note.content, ...)
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        val markwon = Markwon.create(context)
                        markwon.setMarkdown(this, note.content)
                    }
                },
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(8.dp)
            )
            if (backlinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Backlinks:", style = MaterialTheme.typography.titleSmall)
                Column {
                    backlinks.forEach { backlink ->
                        AssistChip(
                            onClick = { onBacklinkClick(backlink) },
                            label = { Text(backlink.title) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownWithNoteLinks(content: String, onNoteLinkClick: (String) -> Unit, modifier: Modifier = Modifier) {
    // Replace [[note]] with clickable chips
    val regex = Regex("\\[\\[(.+?)\\]\\]")
    val parts = regex.split(content)
    val matches = regex.findAll(content).toList()
    Row(modifier = modifier.wrapContentHeight()) {
        for ((i, part) in parts.withIndex()) {
            Text(part, style = MaterialTheme.typography.bodyMedium)
            if (i < matches.size) {
                val noteRef = matches[i].groupValues[1]
                AssistChip(
                    onClick = { onNoteLinkClick(noteRef) },
                    label = { Text("[[${noteRef}]]") },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
