package com.ainotebuddy.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.search.SmartSearchResult
import com.ainotebuddy.app.search.VisualSearchEngine
import com.ainotebuddy.app.ui.components.search.EmptySearchResults
import com.ainotebuddy.app.ui.components.search.VisualSearchResults
import com.ainotebuddy.app.ui.theme.LocalSpacing
import com.ainotebuddy.app.util.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSearchScreen(
    onBackClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val visualSearchEngine = remember { VisualSearchEngine(context) }
    var searchResults by remember { mutableStateOf<List<SmartSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Document picker
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            performVisualSearch(uri, visualSearchEngine, onResults = {
                searchResults = it
            }, onError = {
                error = it
            }, onLoading = {
                isLoading = it
            })
        }
    }
    
    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            performVisualSearch(uri, visualSearchEngine, onResults = {
                searchResults = it
            }, onError = {
                error = it
            }, onLoading = {
                isLoading = it
            })
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visual Search") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add any additional actions if needed
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Search in image button
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search in Image")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Search in document button
                    Button(
                        onClick = { 
                            documentPicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search in Doc")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    // Show loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    // Show error message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: "An error occurred",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                searchResults.isNotEmpty() -> {
                    // Show search results
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResults) { result ->
                            // Use the VisualSearchResultItem from VisualSearchResults.kt
                            VisualSearchResults(
                                searchResults = searchResults,
                                isLoading = false,
                                onNoteClick = onNoteClick
                            )
                        }
                    }
                }
                else -> {
                    // Show empty state
                    EmptySearchResults(
                        message = "Search within images and documents to find related notes"
                    )
                }
            }
        }
    }
}

private fun performVisualSearch(
    uri: Uri,
    visualSearchEngine: VisualSearchEngine,
    onResults: (List<SmartSearchResult>) -> Unit,
    onError: (String) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    // This would be implemented to use the SmartSearchEngine to perform the search
    // For now, we'll just simulate a search
    onLoading(true)
    
    // In a real implementation, you would:
    // 1. Extract text using visualSearchEngine
    // 2. Search notes using SmartSearchEngine
    // 3. Return the results
    
    // Simulate network/database delay
    android.os.Handler().postDelayed({
        try {
            // This would be replaced with actual search logic
            onResults(emptyList())
        } catch (e: Exception) {
            onError(e.message ?: "An error occurred")
        } finally {
            onLoading(false)
        }
    }, 1000)
}
