package com.ainotebuddy.app.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents different types of dashboard widgets
 */
enum class DashboardWidgetType(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val defaultEnabled: Boolean = true
) {
    QUICK_STATS(
        id = "quick_stats",
        displayName = "Quick Stats",
        description = "Overview of your notes count",
        icon = Icons.Filled.Analytics,
        color = Color(0xFF6A82FB)
    ),
    RECENT_NOTES(
        id = "recent_notes", 
        displayName = "Recent Notes",
        description = "Your latest notes",
        icon = Icons.Filled.Schedule,
        color = Color(0xFF667eea)
    ),
    PINNED_NOTES(
        id = "pinned_notes",
        displayName = "Pinned Notes", 
        description = "Your pinned notes",
        icon = Icons.Filled.Star,
        color = Color(0xFFFFD700)
    ),
    FAVORITE_NOTES(
        id = "favorite_notes",
        displayName = "Favorite Notes",
        description = "Your favorite notes",
        icon = Icons.Filled.Favorite,
        color = Color(0xFFFC5C7D)
    ),
    QUICK_ACTIONS(
        id = "quick_actions",
        displayName = "Quick Actions",
        description = "Shortcuts to common tasks",
        icon = Icons.Filled.Speed,
        color = Color(0xFF00FFC6)
    ),
    AI_SUGGESTIONS(
        id = "ai_suggestions",
        displayName = "AI Suggestions",
        description = "Smart recommendations",
        icon = Icons.Filled.Psychology,
        color = Color(0xFF9C27B0),
        defaultEnabled = false
    ),
    UPCOMING_REMINDERS(
        id = "upcoming_reminders",
        displayName = "Upcoming Reminders",
        description = "Notes with reminders",
        icon = Icons.Filled.Notifications,
        color = Color(0xFFFF9800),
        defaultEnabled = false
    ),
    CATEGORIES_OVERVIEW(
        id = "categories_overview",
        displayName = "Categories Overview",
        description = "Notes by category",
        icon = Icons.Filled.Category,
        color = Color(0xFF4CAF50),
        defaultEnabled = false
    ),
    SEARCH_SHORTCUTS(
        id = "search_shortcuts",
        displayName = "Search Shortcuts",
        description = "Quick search filters",
        icon = Icons.Filled.Search,
        color = Color(0xFF2196F3),
        defaultEnabled = false
    ),
    PRODUCTIVITY_STATS(
        id = "productivity_stats",
        displayName = "Productivity Stats",
        description = "Writing activity insights",
        icon = Icons.Filled.TrendingUp,
        color = Color(0xFFE91E63),
        defaultEnabled = false
    )
}

/**
 * Configuration for a dashboard widget
 */
data class DashboardWidgetConfig(
    val type: DashboardWidgetType,
    val isEnabled: Boolean = type.defaultEnabled,
    val position: Int = 0,
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Widget size options
 */
enum class WidgetSize {
    SMALL,   // 1x1 grid
    MEDIUM,  // 2x1 grid  
    LARGE    // 2x2 grid
}

/**
 * Widget display preferences
 */
data class WidgetDisplayConfig(
    val size: WidgetSize = WidgetSize.MEDIUM,
    val showTitle: Boolean = true,
    val compactMode: Boolean = false,
    val maxItems: Int = 5
)