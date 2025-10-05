package com.ainotebuddy.app.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.data.toDomain
import com.ainotebuddy.app.ui.components.GlassCard
import com.ainotebuddy.app.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchScreen(
    onBackClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onVisualSearchClick: () -> Unit,
    viewModel: NoteViewModel = viewModel()
) {
    val context = LocalContext.current
    val searchEngine = remember { SmartSearchEngine(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var showVisualSearch by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<SearchResults?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<SearchSuggestion>>(emptyList()) }
    var selectedFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAdvancedFilters by remember { mutableStateOf(false) }
    
    val notes by viewModel.notes.collectAsState()
    
    // Auto-focus search field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Handle visual search navigation
    LaunchedEffect(showVisualSearch) {
        if (showVisualSearch) {
            onVisualSearchClick()
            // Reset the state after navigation
            showVisualSearch = false
        }
    }

    // Perform search when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(300) // Debounce
            if (searchQuery.isNotBlank()) { // Check again after delay
                isSearching = true
                try {
                    val results = searchEngine.search(searchQuery, notes.map { it.toDomain() })
                    searchResults = results
                    suggestions = results.suggestions
                } catch (e: Exception) {
                    // Handle search error
                } finally {
                    isSearching = false
                }
            }
        } else {
            searchResults = null
            suggestions = emptyList()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0f0f23),
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
    ) {
        // Enhanced Search Header
        SmartSearchHeader(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onBackClick = onBackClick,
            onClearClick = { searchQuery = "" },
            isSearching = isSearching,
            focusRequester = focusRequester,
            onSearch = { /* Search is handled automatically */ },
            onAdvancedFiltersClick = { showAdvancedFilters = true },
            onVisualSearchClick = { showVisualSearch = true },
        )
        
        // Advanced Filters Panel
        AnimatedVisibility(
            visible = showAdvancedFilters,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            AdvancedFiltersPanel(
                selectedFilters = selectedFilters,
                onFiltersChanged = { selectedFilters = it },
                onApplyFilters = {
                    // Re-run search with filters
                    if (searchQuery.isNotBlank()) {
                        scope.launch {
                            isSearching = true
                            try {
                                val results = searchEngine.search(searchQuery, notes.map { it.toDomain() })
                                searchResults = results
                            } finally {
                                isSearching = false
                            }
                        }
                    }
                }
            )
        }
        
        // Search Suggestions
        AnimatedVisibility(
            visible = showSuggestions && suggestions.isNotEmpty() && searchQuery.isNotBlank(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            SearchSuggestionsPanel(
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    searchQuery = suggestion.suggestion
                    showSuggestions = false
                    keyboardController?.hide()
                }
            )
        }
        
        // Search Results or Empty State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when {
                searchQuery.isBlank() -> {
                    SearchEmptyState(
                        onRecentSearchClick = { query ->
                            searchQuery = query
                        },
                        onSavedSearchClick = { query ->
                            searchQuery = query
                        }
                    )
                }
                isSearching -> {
                    SearchLoadingState()
                }
                searchResults != null -> {
                    SearchResultsList(
                        results = searchResults!!,
                        onNoteClick = onNoteClick,
                        onSaveSearch = { query ->
                            // Save search functionality
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    isSearching: Boolean,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    onAdvancedFiltersClick: () -> Unit,
    onVisualSearchClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { 
                        Text(
                            "Search notes, try 'meeting notes from last week'...",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6A82FB)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF6A82FB)
                            )
                        }
                    },
                    trailingIcon = {
                        Row {
                            if (query.isNotBlank()) {
                                IconButton(
                                    onClick = onClearClick,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = onAdvancedFiltersClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.FilterList,
                                    contentDescription = "Advanced Filters",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            // Visual Search Button
                            IconButton(
                                onClick = onVisualSearchClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ImageSearch,
                                    contentDescription = "Visual Search",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6A82FB),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFF6A82FB)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            // Quick Search Tips
            if (query.isBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(getSearchTips()) { tip ->
                        SearchTipChip(
                            text = tip,
                            onClick = { onQueryChange(tip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTipChip(
    text: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6A82FB).copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6A82FB)
        )
    }
}

@Composable
fun AdvancedFiltersPanel(
    selectedFilters: Set<String>,
    onFiltersChanged: (Set<String>) -> Unit,
    onApplyFilters: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Advanced Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Date Range Filters
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(getDateRangeFilters()) { filter ->
                    FilterChip(
                        selected = filter in selectedFilters,
                        onClick = {
                            val newFilters = if (filter in selectedFilters) {
                                selectedFilters - filter
                            } else {
                                selectedFilters + filter
                            }
                            onFiltersChanged(newFilters)
                        },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6A82FB),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
            
            // Note Type Filters
            Text(
                text = "Note Types",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(getNoteTypeFilters()) { filter ->
                    FilterChip(
                        selected = filter in selectedFilters,
                        onClick = {
                            val newFilters = if (filter in selectedFilters) {
                                selectedFilters - filter
                            } else {
                                selectedFilters + filter
                            }
                            onFiltersChanged(newFilters)
                        },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE91E63),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
            
            // Apply Filters Button
            Button(
                onClick = onApplyFilters,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Filters")
            }
        }
    }
}

@Composable
fun SearchSuggestionsPanel(
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            suggestions.take(5).forEach { suggestion ->
                SearchSuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
fun SearchSuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (suggestion.type) {
                SuggestionType.QUERY_COMPLETION -> Icons.Filled.Search
                SuggestionType.RECENT_SEARCH -> Icons.Filled.History
                SuggestionType.SAVED_SEARCH -> Icons.Filled.Bookmark
                SuggestionType.SEMANTIC_EXPANSION -> Icons.Filled.Psychology
                SuggestionType.FILTER_SUGGESTION -> Icons.Filled.FilterList
                else -> Icons.Filled.Lightbulb
            },
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = suggestion.suggestion,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = when (suggestion.type) {
                SuggestionType.QUERY_COMPLETION -> "Complete"
                SuggestionType.RECENT_SEARCH -> "Recent"
                SuggestionType.SAVED_SEARCH -> "Saved"
                SuggestionType.SEMANTIC_EXPANSION -> "Related"
                SuggestionType.FILTER_SUGGESTION -> "Filter"
                else -> ""
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SearchEmptyState(
    onRecentSearchClick: (String) -> Unit,
    onSavedSearchClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Smart Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Try natural language queries like:",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            getExampleQueries().forEach { example ->
                ExampleQueryCard(
                    query = example,
                    onClick = { onRecentSearchClick(example) }
                )
            }
        }
    }
}

@Composable
fun ExampleQueryCard(
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
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
                Icons.Filled.TipsAndUpdates,
                contentDescription = null,
                tint = Color(0xFF6A82FB),
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SearchLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF6A82FB),
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Searching...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Analyzing your notes with AI",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SearchResultsList(
    results: SearchResults,
    onNoteClick: (Note) -> Unit,
    onSaveSearch: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Search Results Header
        item {
            SearchResultsHeader(
                results = results,
                onSaveSearch = onSaveSearch
            )
        }
        
        // Search Results
        items(results.results, key = { it.note.id }) { result ->
            SearchResultCard(
                result = result,
                onClick = { onNoteClick(result.note) }
            )
        }
        
        // Load More or End Message
        if (results.results.isNotEmpty()) {
            item {
                SearchResultsFooter(results = results)
            }
        }
    }
}

@Composable
fun SearchResultsHeader(
    results: SearchResults,
    onSaveSearch: (String) -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${results.totalResults} results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Found in ${results.searchTime}ms${if (results.fromCache) " (cached)" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            IconButton(
                onClick = { onSaveSearch(results.query.rawQuery) }
            ) {
                Icon(
                    Icons.Filled.BookmarkAdd,
                    contentDescription = "Save Search",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(
    result: SmartSearchResult,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Note Title with Highlights
            Text(
                text = buildHighlightedText(result.note.title, result.highlights, FieldType.TITLE),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content Preview with Highlights
            if (result.contextSnippets.isNotEmpty()) {
                Text(
                    text = buildHighlightedText(
                        result.contextSnippets.first(), 
                        result.highlights, 
                        FieldType.CONTENT
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = result.note.content.take(150) + if (result.note.content.length > 150) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Relevance Score
                    RelevanceIndicator(score = result.relevanceScore)
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Category
                    if (result.note.category.isNotBlank()) {
                        CategoryChip(category = result.note.category)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Tags
                    result.note.tags.take(2).forEach { tag ->
                        TagChip(tag = tag)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                
                // Date
                Text(
                    text = formatDate(result.note.dateModified),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun RelevanceIndicator(score: Float) {
    val color = when {
        score >= 5f -> Color(0xFF4CAF50)
        score >= 3f -> Color(0xFFFF9800)
        else -> Color(0xFFE91E63)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            Icon(
                Icons.Filled.Circle,
                contentDescription = null,
                tint = if (index < (score / 2).toInt()) color else Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(6.dp)
            )
            if (index < 4) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
fun CategoryChip(category: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6A82FB).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6A82FB)
        )
    }
}

@Composable
fun TagChip(tag: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE91E63).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "#$tag",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE91E63)
        )
    }
}

@Composable
fun SearchResultsFooter(results: SearchResults) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "End of results",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            if (results.suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try: ${results.suggestions.first().suggestion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6A82FB)
                )
            }
        }
    }
}

// Helper functions
private fun buildHighlightedText(
    text: String, 
    highlights: List<SearchHighlight>, 
    fieldType: FieldType
) = buildAnnotatedString {
    val relevantHighlights = highlights.filter { it.fieldType == fieldType }
        .sortedBy { it.startIndex }
    
    if (relevantHighlights.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    
    var lastIndex = 0
    relevantHighlights.forEach { highlight ->
        if (highlight.startIndex > lastIndex) {
            append(text.substring(lastIndex, highlight.startIndex))
        }
        
        withStyle(
            style = SpanStyle(
                background = Color(0xFF6A82FB).copy(alpha = 0.3f),
                color = Color(0xFF6A82FB)
            )
        ) {
            append(text.substring(highlight.startIndex, highlight.endIndex))
        }
        
        lastIndex = highlight.endIndex
    }
    
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getSearchTips(): List<String> = listOf(
    "meeting notes",
    "today",
    "important",
    "#work",
    "last week"
)

private fun getDateRangeFilters(): List<String> = listOf(
    "Today", "Yesterday", "This Week", "Last Week", "This Month", "Last Month"
)

private fun getNoteTypeFilters(): List<String> = listOf(
    "Text", "Voice", "Image", "Drawing", "Document", "Checklist"
)

private fun getExampleQueries(): List<String> = listOf(
    "meeting notes from last week",
    "important tasks #work",
    "voice notes about project",
    "images from yesterday",
    "notes similar to productivity"
)