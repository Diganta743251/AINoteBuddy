package com.ainotebuddy.app.features

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.AdvancedNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.jsoup.Jsoup
import java.net.URL

class WebClipperService(private val repository: AdvancedNoteRepository) {
    
    suspend fun clipWebPage(url: String, title: String? = null): NoteEntity {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(url).get()
                val pageTitle = title ?: doc.title()
                val content = extractMainContent(doc)
                
                val note = NoteEntity(
                    title = pageTitle,
                    content = content,
                    tags = "web-clip,${extractDomain(url)}",
                    format = "markdown"
                )
                
                repository.insertNote(note)
                note
            } catch (e: Exception) {
                NoteEntity(
                    title = title ?: "Web Clip Error",
                    content = "Failed to clip: ${e.message}\nURL: $url",
                    tags = "web-clip,error"
                )
            }
        }
    }
    
    private fun extractMainContent(doc: org.jsoup.nodes.Document): String {
        // Remove unwanted elements
        doc.select("script, style, nav, header, footer, aside, .advertisement").remove()
        
        // Try to find main content
        val mainContent = doc.select("main, article, .content, .post, .entry").first()
            ?: doc.select("body").first()
        
        return mainContent?.let { element ->
            val text = element.text()
            val images = element.select("img").map { it.attr("src") }
            val links = element.select("a").map { "${it.text()} (${it.attr("href")})" }
            
            buildString {
                appendLine(text)
                if (images.isNotEmpty()) {
                    appendLine("\n## Images")
                    images.forEach { appendLine("- $it") }
                }
                if (links.isNotEmpty()) {
                    appendLine("\n## Links")
                    links.take(10).forEach { appendLine("- $it") }
                }
            }
        } ?: "No content found"
    }
    
    private fun extractDomain(url: String): String {
        return try {
            URL(url).host.removePrefix("www.")
        } catch (e: Exception) {
            "unknown"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebClipperScreen(
    onBack: () -> Unit,
    onClipSaved: (NoteEntity) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var isClipping by remember { mutableStateOf(false) }
    var recentClips by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Handle shared URL from browser
    LaunchedEffect(Unit) {
        val intent = (context as? android.app.Activity)?.intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedUrl != null) {
                url = sharedUrl
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Web Clipper") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Input
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Website URL") },
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Filled.Link, contentDescription = null)
                }
            )
            
            // Title Input (optional)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Custom Title (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Filled.Title, contentDescription = null)
                }
            )
            
            // Clip Button
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        isClipping = true
                        scope.launch {
                            try {
                                val clipperService = WebClipperService(
                                    AdvancedNoteRepository(
                                        noteDao = null,
                                        categoryDao = null,
                                        tagDao = null,
                                        templateDao = null,
                                        checklistItemDao = null,
                                        aiService = null
                                    )
                                )
                                val note = clipperService.clipWebPage(url, title.ifBlank { null })
                                onClipSaved(note)
                            } catch (_: Exception) {
                                // no-op for now
                            } finally {
                                isClipping = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && !isClipping
            ) {
                if (isClipping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isClipping) "Clipping..." else "Clip Web Page")
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentClips) { clip ->
                    WebClipCard(
                        clip = clip,
                        onClick = { onClipSaved(clip) }
                    )
                }
                
                if (recentClips.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No web clips yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Share a webpage from your browser to get started",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebClipCard(
    clip: NoteEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = clip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = clip.content.take(100) + if (clip.content.length > 100) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                clip.tags.split(",").forEach { tag ->
                    if (tag.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(tag.trim()) }
                        )
                    }
                }
            }
        }
    }
}