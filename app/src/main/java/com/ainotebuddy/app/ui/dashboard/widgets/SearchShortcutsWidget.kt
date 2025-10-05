package com.ainotebuddy.app.ui.dashboard.widgets

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.search.*
import com.ainotebuddy.app.ui.components.GlassCard
import kotlinx.coroutines.launch

@Composable
fun SearchShortcutsWidget(
    onSearchClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedSearchManager = remember { SavedSearchManager(context) }
    val scope = rememberCoroutineScope()
    
    val savedSearches by savedSearchManager.savedSearches.collectAsState()
    val searchHistory by savedSearchManager.searchHistory.collectAsState()
    
    var selectedTab by remember { mutableStateOf(SearchShortcutTab.SAVED) }
    
    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Widget Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.SavedSearch,
                        contentDescription = null,
                        tint = Color(0xFF6A82FB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search Shortcuts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                TextButton(
                    onClick = onViewAllClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF6A82FB)
                    )
                ) {
                    Text("View All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tab Row
            SearchShortcutTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content based on selected tab
            when (selectedTab) {
                SearchShortcutTab.SAVED -> {
                    SavedSearchShortcuts(
                        savedSearches = savedSearches.take(5),
                        onSearchClick = { preset ->
                            onSearchClick(preset.query.rawQuery)
                            scope.launch {
                                savedSearchManager.recordSearchUsage(preset.id)
                            }
                        }
                    )
                }
                SearchShortcutTab.RECENT -> {
                    RecentSearchShortcuts(
                        recentSearches = savedSearchManager.getRecentSearches(5),
                        onSearchClick = onSearchClick
                    )
                }
                SearchShortcutTab.POPULAR -> {
                    PopularSearchShortcuts(
                        popularSearches = savedSearchManager.getPopularSearches(5),
                        onSearchClick = onSearchClick
                    )
                }
            }
        }
    }
}

enum class SearchShortcutTab {
    SAVED, RECENT, POPULAR
}

@Composable
fun SearchShortcutTabs(
    selectedTab: SearchShortcutTab,
    onTabSelected: (SearchShortcutTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchShortcutTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            
            Card(
                onClick = { onTabSelected(tab) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) 
                        Color(0xFF6A82FB).copy(alpha = 0.3f) 
                    else 
                        Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (tab) {
                            SearchShortcutTab.SAVED -> Icons.Filled.Bookmark
                            SearchShortcutTab.RECENT -> Icons.Filled.History
                            SearchShortcutTab.POPULAR -> Icons.Filled.TrendingUp
                        },
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF6A82FB) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (tab) {
                            SearchShortcutTab.SAVED -> "Saved"
                            SearchShortcutTab.RECENT -> "Recent"
                            SearchShortcutTab.POPULAR -> "Popular"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color(0xFF6A82FB) else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SavedSearchShortcuts(
    savedSearches: List<SavedSearchPreset>,
    onSearchClick: (SavedSearchPreset) -> Unit
) {
    if (savedSearches.isEmpty()) {
        EmptySearchShortcuts(
            icon = Icons.Filled.BookmarkBorder,
            message = "No saved searches yet",
            subtitle = "Save your frequent searches for quick access"
        )
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedSearches) { preset ->
                SavedSearchCard(
                    preset = preset,
                    onClick = { onSearchClick(preset) }
                )
            }
        }
    }
}

@Composable
fun RecentSearchShortcuts(
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit
) {
    if (recentSearches.isEmpty()) {
        EmptySearchShortcuts(
            icon = Icons.Filled.History,
            message = "No recent searches",
            subtitle = "Your recent searches will appear here"
        )
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recentSearches) { query ->
                RecentSearchCard(
                    query = query,
                    onClick = { onSearchClick(query) }
                )
            }
        }
    }
}

@Composable
fun PopularSearchShortcuts(
    popularSearches: List<String>,
    onSearchClick: (String) -> Unit
) {
    if (popularSearches.isEmpty()) {
        EmptySearchShortcuts(
            icon = Icons.Filled.TrendingUp,
            message = "No popular searches yet",
            subtitle = "Popular searches will appear as you use the app"
        )
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(popularSearches) { query ->
                PopularSearchCard(
                    query = query,
                    onClick = { onSearchClick(query) }
                )
            }
        }
    }
}

@Composable
fun SavedSearchCard(
    preset: SavedSearchPreset,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = preset.category.color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    preset.category.icon,
                    contentDescription = null,
                    tint = preset.category.color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = preset.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = preset.category.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (preset.usageCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Used ${preset.usageCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun RecentSearchCard(
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PopularSearchCard(
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EmptySearchShortcuts(
    icon: ImageVector,
    message: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

// Extension properties for SearchPresetCategory
val SearchPresetCategory.color: Color
    get() = when (this) {
        SearchPresetCategory.WORK -> Color(0xFF2196F3)
        SearchPresetCategory.PERSONAL -> Color(0xFF9C27B0)
        SearchPresetCategory.ACADEMIC -> Color(0xFF4CAF50)
        SearchPresetCategory.CREATIVE -> Color(0xFFFF9800)
        SearchPresetCategory.PRODUCTIVITY -> Color(0xFFE91E63)
        SearchPresetCategory.RECENT -> Color(0xFF607D8B)
        SearchPresetCategory.FAVORITES -> Color(0xFFFFD700)
    }

val SearchPresetCategory.icon: ImageVector
    get() = when (this) {
        SearchPresetCategory.WORK -> Icons.Filled.Work
        SearchPresetCategory.PERSONAL -> Icons.Filled.Person
        SearchPresetCategory.ACADEMIC -> Icons.Filled.School
        SearchPresetCategory.CREATIVE -> Icons.Filled.Palette
        SearchPresetCategory.PRODUCTIVITY -> Icons.Filled.TrendingUp
        SearchPresetCategory.RECENT -> Icons.Filled.History
        SearchPresetCategory.FAVORITES -> Icons.Filled.Star
    }

val SearchPresetCategory.displayName: String
    get() = when (this) {
        SearchPresetCategory.WORK -> "Work"
        SearchPresetCategory.PERSONAL -> "Personal"
        SearchPresetCategory.ACADEMIC -> "Academic"
        SearchPresetCategory.CREATIVE -> "Creative"
        SearchPresetCategory.PRODUCTIVITY -> "Productivity"
        SearchPresetCategory.RECENT -> "Recent"
        SearchPresetCategory.FAVORITES -> "Favorites"
    }