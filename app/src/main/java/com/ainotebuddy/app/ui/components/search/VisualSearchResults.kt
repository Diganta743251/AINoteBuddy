package com.ainotebuddy.app.ui.components.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.search.SmartSearchResult
import com.ainotebuddy.app.ui.theme.LocalSpacing

@Composable
fun VisualSearchResults(
    searchResults: List<SmartSearchResult>,
    isLoading: Boolean,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier,
    emptyStateContent: @Composable () -> Unit = {
        EmptySearchResults()
    }
) {
    val spacing = LocalSpacing.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium)
    ) {
        when {
            isLoading -> {
                LoadingSearchResults()
            }
            searchResults.isEmpty() -> {
                emptyStateContent()
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    items(searchResults, key = { it.note.id }) { result ->
                        VisualSearchResultItem(
                            result = result,
                            onClick = { onNoteClick(result.note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualSearchResultItem(
    result: SmartSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val note = result.note
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium)
        ) {
            // Image preview removed: domain Note doesn't expose thumbnailUrl
            
            // Title and metadata
            Text(
                text = note.title.ifEmpty { "Untitled Note" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Snippet of matched content
            result.highlights.firstOrNull()?.let { highlight ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = highlight.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Metadata row
            Spacer(modifier = Modifier.height(spacing.small))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                // Last modified
                Text(
                    text = "Updated ${note.dateModified.relativeTimeString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Match strength indicator
                MatchStrengthIndicator(strength = result.relevanceScore)
            }
        }
    }
}

@Composable
private fun MatchStrengthIndicator(strength: Float) {
    val percentage = (strength * 100).toInt().coerceIn(0, 100)
    val color = when {
        percentage > 70 -> Color.Green
        percentage > 40 -> Color(0xFFFFA500) // Orange
        else -> Color.Red
    }
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun MatchBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .padding(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LoadingSearchResults() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Searching...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptySearchResults(
    message: String = "No results found",
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Long.relativeTimeString(): String {
    val seconds = (System.currentTimeMillis() - this) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        seconds < 604800 -> "${seconds / 86400}d ago"
        seconds < 2592000 -> "${seconds / 604800}w ago"
        seconds < 31536000 -> "${seconds / 2592000}mo ago"
        else -> "${seconds / 31536000}y ago"
    }
}
