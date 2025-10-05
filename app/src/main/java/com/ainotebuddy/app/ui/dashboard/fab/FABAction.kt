package com.ainotebuddy.app.ui.dashboard.fab

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents different types of FAB actions available to users
 */
enum class FABActionType(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val category: FABActionCategory,
    val defaultEnabled: Boolean = false,
    val isPremium: Boolean = false
) {
    // Core Actions
    NEW_NOTE(
        id = "new_note",
        displayName = "New Note",
        description = "Create a new text note",
        icon = Icons.Filled.Add,
        color = Color(0xFF6A82FB),
        category = FABActionCategory.CORE,
        defaultEnabled = true
    ),
    VOICE_NOTE(
        id = "voice_note",
        displayName = "Voice Note",
        description = "Record a voice note",
        icon = Icons.Filled.Mic,
        color = Color(0xFFFF5722),
        category = FABActionCategory.CORE,
        defaultEnabled = true
    ),
    QUICK_CAPTURE(
        id = "quick_capture",
        displayName = "Quick Capture",
        description = "Quickly jot down thoughts",
        icon = Icons.Filled.FlashOn,
        color = Color(0xFFFFD700),
        category = FABActionCategory.CORE,
        defaultEnabled = true
    ),
    
    // Media Actions
    CAMERA_NOTE(
        id = "camera_note",
        displayName = "Camera Note",
        description = "Take a photo note",
        icon = Icons.Filled.CameraAlt,
        color = Color(0xFF4CAF50),
        category = FABActionCategory.MEDIA
    ),
    SCAN_DOCUMENT(
        id = "scan_document",
        displayName = "Scan Document",
        description = "Scan and OCR documents",
        icon = Icons.Filled.Scanner,
        color = Color(0xFF2196F3),
        category = FABActionCategory.MEDIA
    ),
    DRAW_NOTE(
        id = "draw_note",
        displayName = "Draw Note",
        description = "Create a drawing or sketch",
        icon = Icons.Filled.Brush,
        color = Color(0xFF9C27B0),
        category = FABActionCategory.MEDIA
    ),
    
    // Organization Actions
    NEW_FOLDER(
        id = "new_folder",
        displayName = "New Folder",
        description = "Create a new folder",
        icon = Icons.Filled.CreateNewFolder,
        color = Color(0xFF795548),
        category = FABActionCategory.ORGANIZATION
    ),
    NEW_TAG(
        id = "new_tag",
        displayName = "New Tag",
        description = "Create a new tag",
        icon = Icons.Filled.Label,
        color = Color(0xFF607D8B),
        category = FABActionCategory.ORGANIZATION
    ),
    QUICK_SEARCH(
        id = "quick_search",
        displayName = "Smart Search",
        description = "AI-powered natural language search",
        icon = Icons.Filled.Search,
        color = Color(0xFF2196F3),
        category = FABActionCategory.ORGANIZATION,
        defaultEnabled = true
    ),
    
    // Templates & Productivity
    FROM_TEMPLATE(
        id = "from_template",
        displayName = "From Template",
        description = "Create note from template",
        icon = Icons.Filled.Description,
        color = Color(0xFF673AB7),
        category = FABActionCategory.TEMPLATES
    ),
    MEETING_NOTES(
        id = "meeting_notes",
        displayName = "Meeting Notes",
        description = "Quick meeting template",
        icon = Icons.Filled.BusinessCenter,
        color = Color(0xFF009688),
        category = FABActionCategory.TEMPLATES
    ),
    TODO_LIST(
        id = "todo_list",
        displayName = "Todo List",
        description = "Create a todo list",
        icon = Icons.Filled.CheckCircle,
        color = Color(0xFF8BC34A),
        category = FABActionCategory.TEMPLATES
    ),
    DAILY_JOURNAL(
        id = "daily_journal",
        displayName = "Daily Journal",
        description = "Today's journal entry",
        icon = Icons.Filled.Today,
        color = Color(0xFFFF9800),
        category = FABActionCategory.TEMPLATES
    ),
    
    // Advanced Actions
    AI_ASSIST(
        id = "ai_assist",
        displayName = "AI Assistant",
        description = "Get AI writing help",
        icon = Icons.Filled.Psychology,
        color = Color(0xFFE91E63),
        category = FABActionCategory.AI,
        isPremium = true
    ),
    SMART_SUMMARY(
        id = "smart_summary",
        displayName = "Smart Summary",
        description = "AI-powered note summary",
        icon = Icons.Filled.AutoAwesome,
        color = Color(0xFFFF6B6B),
        category = FABActionCategory.AI,
        isPremium = true
    ),
    WEB_CLIPPER(
        id = "web_clipper",
        displayName = "Web Clipper",
        description = "Save web content",
        icon = Icons.Filled.ContentPaste,
        color = Color(0xFF00BCD4),
        category = FABActionCategory.ADVANCED
    ),
    LOCATION_NOTE(
        id = "location_note",
        displayName = "Location Note",
        description = "Note with current location",
        icon = Icons.Filled.LocationOn,
        color = Color(0xFFFF5722),
        category = FABActionCategory.ADVANCED
    ),
    
    // Sharing & Collaboration
    SHARE_QUICK(
        id = "share_quick",
        displayName = "Quick Share",
        description = "Share note quickly",
        icon = Icons.Filled.Share,
        color = Color(0xFF4CAF50),
        category = FABActionCategory.SHARING
    ),
    COLLABORATE(
        id = "collaborate",
        displayName = "Collaborate",
        description = "Invite others to collaborate",
        icon = Icons.Filled.Group,
        color = Color(0xFF2196F3),
        category = FABActionCategory.SHARING,
        isPremium = true
    ),
    
    // Utilities
    REMINDER(
        id = "reminder",
        displayName = "Set Reminder",
        description = "Create a reminder note",
        icon = Icons.Filled.Alarm,
        color = Color(0xFFFF9800),
        category = FABActionCategory.UTILITIES
    ),
    BACKUP_NOW(
        id = "backup_now",
        displayName = "Backup Now",
        description = "Backup notes to cloud",
        icon = Icons.Filled.CloudUpload,
        color = Color(0xFF607D8B),
        category = FABActionCategory.UTILITIES
    )
}

/**
 * Categories for organizing FAB actions
 */
enum class FABActionCategory(
    val displayName: String,
    val description: String,
    val color: Color
) {
    CORE(
        displayName = "Core Actions",
        description = "Essential note-taking actions",
        color = Color(0xFF6A82FB)
    ),
    MEDIA(
        displayName = "Media & Capture",
        description = "Photo, voice, and drawing actions",
        color = Color(0xFF4CAF50)
    ),
    ORGANIZATION(
        displayName = "Organization",
        description = "Folders, tags, and search",
        color = Color(0xFF795548)
    ),
    TEMPLATES(
        displayName = "Templates & Productivity",
        description = "Pre-built note templates",
        color = Color(0xFF673AB7)
    ),
    AI(
        displayName = "AI Features",
        description = "AI-powered assistance",
        color = Color(0xFFE91E63)
    ),
    ADVANCED(
        displayName = "Advanced Features",
        description = "Power user features",
        color = Color(0xFF00BCD4)
    ),
    SHARING(
        displayName = "Sharing & Collaboration",
        description = "Share and collaborate on notes",
        color = Color(0xFF2196F3)
    ),
    UTILITIES(
        displayName = "Utilities",
        description = "Backup, reminders, and tools",
        color = Color(0xFF607D8B)
    )
}

/**
 * Configuration for a FAB action
 */
data class FABActionConfig(
    val type: FABActionType,
    val isEnabled: Boolean = type.defaultEnabled,
    val position: Int = 0,
    val customLabel: String? = null,
    val customIcon: String? = null,
    val customColor: String? = null,
    val shortcutKey: String? = null
)

/**
 * FAB menu display styles
 */
enum class FABMenuStyle {
    CIRCULAR,      // Actions arranged in a circle
    LINEAR,        // Actions in a vertical line
    GRID,          // Actions in a grid layout
    MINIMAL        // Only show most important actions
}

/**
 * FAB menu configuration
 */
data class FABMenuConfig(
    val style: FABMenuStyle = FABMenuStyle.LINEAR,
    val maxVisibleActions: Int = 6,
    val showLabels: Boolean = true,
    val animationDuration: Int = 300,
    val hapticFeedback: Boolean = true,
    val actions: List<FABActionConfig> = getDefaultFABActions()
)

/**
 * Get default FAB actions configuration
 */
fun getDefaultFABActions(): List<FABActionConfig> {
    return FABActionType.values()
        .filter { it.defaultEnabled }
        .mapIndexed { index, type ->
            FABActionConfig(
                type = type,
                isEnabled = true,
                position = index
            )
        }
}

/**
 * Usage analytics for FAB actions
 */
data class FABActionUsage(
    val actionId: String,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L,
    val averageUsagePerDay: Float = 0f
)

/**
 * Smart suggestions based on usage patterns
 */
data class FABActionSuggestion(
    val action: FABActionType,
    val reason: String,
    val confidence: Float,
    val category: SuggestionCategory
)

enum class SuggestionCategory {
    FREQUENTLY_USED,
    TIME_BASED,
    CONTEXT_BASED,
    TRENDING,
    COMPLEMENTARY
}