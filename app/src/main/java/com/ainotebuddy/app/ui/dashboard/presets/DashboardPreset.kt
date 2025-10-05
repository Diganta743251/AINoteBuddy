package com.ainotebuddy.app.ui.dashboard.presets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ainotebuddy.app.ui.dashboard.DashboardWidgetType
import com.ainotebuddy.app.ui.dashboard.DashboardWidgetConfig
import com.ainotebuddy.app.ui.dashboard.fab.FABActionType
import com.ainotebuddy.app.ui.dashboard.fab.FABActionConfig
import com.ainotebuddy.app.ui.dashboard.fab.FABMenuStyle
import com.ainotebuddy.app.ui.dashboard.fab.FABMenuConfig

/**
 * Represents different dashboard layout presets for various user workflows
 */
enum class DashboardPresetType(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val category: PresetCategory
) {
    // Productivity Presets
    TASK_FOCUSED(
        id = "task_focused",
        displayName = "Task Focused",
        description = "Optimized for task management and productivity tracking",
        icon = Icons.Filled.CheckCircle,
        color = Color(0xFF4CAF50),
        category = PresetCategory.PRODUCTIVITY
    ),
    BUSINESS_PROFESSIONAL(
        id = "business_professional",
        displayName = "Business Professional",
        description = "Perfect for meetings, reports, and business workflows",
        icon = Icons.Filled.BusinessCenter,
        color = Color(0xFF2196F3),
        category = PresetCategory.PRODUCTIVITY
    ),
    PROJECT_MANAGER(
        id = "project_manager",
        displayName = "Project Manager",
        description = "Comprehensive view for managing multiple projects",
        icon = Icons.Filled.AccountTree,
        color = Color(0xFF673AB7),
        category = PresetCategory.PRODUCTIVITY
    ),
    
    // Creative Presets
    CREATIVE_WRITER(
        id = "creative_writer",
        displayName = "Creative Writer",
        description = "Focused on writing, inspiration, and creative workflows",
        icon = Icons.Filled.Edit,
        color = Color(0xFFE91E63),
        category = PresetCategory.CREATIVE
    ),
    VISUAL_ARTIST(
        id = "visual_artist",
        displayName = "Visual Artist",
        description = "Image-focused with drawing and visual note capabilities",
        icon = Icons.Filled.Brush,
        color = Color(0xFF9C27B0),
        category = PresetCategory.CREATIVE
    ),
    CONTENT_CREATOR(
        id = "content_creator",
        displayName = "Content Creator",
        description = "Balanced setup for content planning and creation",
        icon = Icons.Filled.VideoLibrary,
        color = Color(0xFFFF5722),
        category = PresetCategory.CREATIVE
    ),
    
    // Academic Presets
    STUDENT(
        id = "student",
        displayName = "Student",
        description = "Study-focused with reminders and academic organization",
        icon = Icons.Filled.School,
        color = Color(0xFF3F51B5),
        category = PresetCategory.ACADEMIC
    ),
    RESEARCHER(
        id = "researcher",
        displayName = "Researcher",
        description = "Research-oriented with reference management and analysis",
        icon = Icons.Filled.Science,
        color = Color(0xFF009688),
        category = PresetCategory.ACADEMIC
    ),
    TEACHER(
        id = "teacher",
        displayName = "Teacher",
        description = "Lesson planning and educational content management",
        icon = Icons.Filled.MenuBook,
        color = Color(0xFF795548),
        category = PresetCategory.ACADEMIC
    ),
    
    // Personal Presets
    PERSONAL_JOURNAL(
        id = "personal_journal",
        displayName = "Personal Journal",
        description = "Daily journaling and personal reflection focused",
        icon = Icons.Filled.Book,
        color = Color(0xFFFF9800),
        category = PresetCategory.PERSONAL
    ),
    MINIMALIST(
        id = "minimalist",
        displayName = "Minimalist",
        description = "Clean, simple interface with essential features only",
        icon = Icons.Filled.Minimize,
        color = Color(0xFF607D8B),
        category = PresetCategory.PERSONAL
    ),
    POWER_USER(
        id = "power_user",
        displayName = "Power User",
        description = "All features enabled for maximum functionality",
        icon = Icons.Filled.Settings,
        color = Color(0xFF6A82FB),
        category = PresetCategory.PERSONAL
    )
}

/**
 * Categories for organizing presets
 */
enum class PresetCategory(
    val displayName: String,
    val description: String,
    val color: Color
) {
    PRODUCTIVITY(
        displayName = "Productivity",
        description = "Work and task-focused layouts",
        color = Color(0xFF4CAF50)
    ),
    CREATIVE(
        displayName = "Creative",
        description = "Layouts for creative workflows",
        color = Color(0xFFE91E63)
    ),
    ACADEMIC(
        displayName = "Academic",
        description = "Study and research-focused layouts",
        color = Color(0xFF3F51B5)
    ),
    PERSONAL(
        displayName = "Personal",
        description = "Personal use and journaling layouts",
        color = Color(0xFFFF9800)
    )
}

/**
 * Complete dashboard preset configuration
 */
data class DashboardPreset(
    val type: DashboardPresetType,
    val widgetConfigs: List<DashboardWidgetConfig>,
    val fabConfig: FABMenuConfig,
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Preset usage statistics
 */
data class PresetUsageStats(
    val presetId: String,
    val timesApplied: Int = 0,
    val lastApplied: Long = 0L,
    val userRating: Float = 0f,
    val customizations: Int = 0
)

/**
 * Get all available dashboard presets
 */
fun getAllDashboardPresets(): List<DashboardPreset> {
    return listOf(
        getTaskFocusedPreset(),
        getBusinessProfessionalPreset(),
        getProjectManagerPreset(),
        getCreativeWriterPreset(),
        getVisualArtistPreset(),
        getContentCreatorPreset(),
        getStudentPreset(),
        getResearcherPreset(),
        getTeacherPreset(),
        getPersonalJournalPreset(),
        getMinimalistPreset(),
        getPowerUserPreset()
    )
}

/**
 * Get presets by category
 */
fun getPresetsByCategory(category: PresetCategory): List<DashboardPreset> {
    return getAllDashboardPresets().filter { it.type.category == category }
}

// Individual preset configurations

fun getTaskFocusedPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.TASK_FOCUSED,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.QUICK_STATS, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.UPCOMING_REMINDERS, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.PINNED_NOTES, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.PRODUCTIVITY_STATS, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 4)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.LINEAR,
            maxVisibleActions = 5,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.TODO_LIST, true, 0),
                FABActionConfig(FABActionType.REMINDER, true, 1),
                FABActionConfig(FABActionType.NEW_NOTE, true, 2),
                FABActionConfig(FABActionType.QUICK_CAPTURE, true, 3),
                FABActionConfig(FABActionType.MEETING_NOTES, true, 4)
            )
        )
    )
}

fun getBusinessProfessionalPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.BUSINESS_PROFESSIONAL,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.QUICK_STATS, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.UPCOMING_REMINDERS, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 4)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.GRID,
            maxVisibleActions = 6,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.MEETING_NOTES, true, 0),
                FABActionConfig(FABActionType.NEW_NOTE, true, 1),
                FABActionConfig(FABActionType.SCAN_DOCUMENT, true, 2),
                FABActionConfig(FABActionType.VOICE_NOTE, true, 3),
                FABActionConfig(FABActionType.SHARE_QUICK, true, 4),
                FABActionConfig(FABActionType.BACKUP_NOW, true, 5)
            )
        )
    )
}

fun getProjectManagerPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.PROJECT_MANAGER,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.QUICK_STATS, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.PINNED_NOTES, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.UPCOMING_REMINDERS, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.PRODUCTIVITY_STATS, true, 4),
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 5)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.CIRCULAR,
            maxVisibleActions = 7,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.NEW_FOLDER, true, 0),
                FABActionConfig(FABActionType.TODO_LIST, true, 1),
                FABActionConfig(FABActionType.MEETING_NOTES, true, 2),
                FABActionConfig(FABActionType.NEW_NOTE, true, 3),
                FABActionConfig(FABActionType.REMINDER, true, 4),
                FABActionConfig(FABActionType.COLLABORATE, true, 5),
                FABActionConfig(FABActionType.QUICK_SEARCH, true, 6)
            )
        )
    )
}

fun getCreativeWriterPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.CREATIVE_WRITER,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.PRODUCTIVITY_STATS, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.AI_SUGGESTIONS, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.FAVORITE_NOTES, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 4)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.LINEAR,
            maxVisibleActions = 5,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.NEW_NOTE, true, 0),
                FABActionConfig(FABActionType.DAILY_JOURNAL, true, 1),
                FABActionConfig(FABActionType.AI_ASSIST, true, 2),
                FABActionConfig(FABActionType.VOICE_NOTE, true, 3),
                FABActionConfig(FABActionType.FROM_TEMPLATE, true, 4)
            )
        )
    )
}

fun getVisualArtistPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.VISUAL_ARTIST,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.FAVORITE_NOTES, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 3)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.CIRCULAR,
            maxVisibleActions = 6,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.DRAW_NOTE, true, 0),
                FABActionConfig(FABActionType.CAMERA_NOTE, true, 1),
                FABActionConfig(FABActionType.NEW_NOTE, true, 2),
                FABActionConfig(FABActionType.NEW_FOLDER, true, 3),
                FABActionConfig(FABActionType.SHARE_QUICK, true, 4),
                FABActionConfig(FABActionType.SCAN_DOCUMENT, true, 5)
            )
        )
    )
}

fun getContentCreatorPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.CONTENT_CREATOR,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.QUICK_STATS, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.AI_SUGGESTIONS, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 4)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.GRID,
            maxVisibleActions = 6,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.NEW_NOTE, true, 0),
                FABActionConfig(FABActionType.CAMERA_NOTE, true, 1),
                FABActionConfig(FABActionType.VOICE_NOTE, true, 2),
                FABActionConfig(FABActionType.WEB_CLIPPER, true, 3),
                FABActionConfig(FABActionType.AI_ASSIST, true, 4),
                FABActionConfig(FABActionType.SHARE_QUICK, true, 5)
            )
        )
    )
}

fun getStudentPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.STUDENT,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.UPCOMING_REMINDERS, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.PRODUCTIVITY_STATS, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 4)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.LINEAR,
            maxVisibleActions = 5,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.NEW_NOTE, true, 0),
                FABActionConfig(FABActionType.SCAN_DOCUMENT, true, 1),
                FABActionConfig(FABActionType.REMINDER, true, 2),
                FABActionConfig(FABActionType.TODO_LIST, true, 3),
                FABActionConfig(FABActionType.FROM_TEMPLATE, true, 4)
            )
        )
    )
}

fun getResearcherPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.RESEARCHER,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.AI_SUGGESTIONS, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.SEARCH_SHORTCUTS, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 4)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.GRID,
            maxVisibleActions = 6,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.NEW_NOTE, true, 0),
                FABActionConfig(FABActionType.WEB_CLIPPER, true, 1),
                FABActionConfig(FABActionType.SCAN_DOCUMENT, true, 2),
                FABActionConfig(FABActionType.NEW_FOLDER, true, 3),
                FABActionConfig(FABActionType.QUICK_SEARCH, true, 4),
                FABActionConfig(FABActionType.AI_ASSIST, true, 5)
            )
        )
    )
}

fun getTeacherPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.TEACHER,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.UPCOMING_REMINDERS, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 3)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.LINEAR,
            maxVisibleActions = 5,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.FROM_TEMPLATE, true, 0),
                FABActionConfig(FABActionType.NEW_NOTE, true, 1),
                FABActionConfig(FABActionType.SCAN_DOCUMENT, true, 2),
                FABActionConfig(FABActionType.SHARE_QUICK, true, 3),
                FABActionConfig(FABActionType.REMINDER, true, 4)
            )
        )
    )
}

fun getPersonalJournalPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.PERSONAL_JOURNAL,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.PRODUCTIVITY_STATS, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.FAVORITE_NOTES, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 3)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.MINIMAL,
            maxVisibleActions = 3,
            showLabels = false,
            actions = listOf(
                FABActionConfig(FABActionType.DAILY_JOURNAL, true, 0),
                FABActionConfig(FABActionType.NEW_NOTE, true, 1),
                FABActionConfig(FABActionType.VOICE_NOTE, true, 2)
            )
        )
    )
}

fun getMinimalistPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.MINIMALIST,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.QUICK_ACTIONS, true, 1),
            // Disable all other widgets for minimal experience
            DashboardWidgetConfig(DashboardWidgetType.QUICK_STATS, false, 2),
            DashboardWidgetConfig(DashboardWidgetType.PINNED_NOTES, false, 3),
            DashboardWidgetConfig(DashboardWidgetType.AI_SUGGESTIONS, false, 4),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, false, 5),
            DashboardWidgetConfig(DashboardWidgetType.PRODUCTIVITY_STATS, false, 6),
            DashboardWidgetConfig(DashboardWidgetType.UPCOMING_REMINDERS, false, 7)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.MINIMAL,
            maxVisibleActions = 3,
            showLabels = false,
            actions = listOf(
                FABActionConfig(FABActionType.NEW_NOTE, true, 0),
                FABActionConfig(FABActionType.QUICK_CAPTURE, true, 1),
                FABActionConfig(FABActionType.QUICK_SEARCH, true, 2)
            )
        )
    )
}

fun getPowerUserPreset(): DashboardPreset {
    return DashboardPreset(
        type = DashboardPresetType.POWER_USER,
        widgetConfigs = listOf(
            DashboardWidgetConfig(DashboardWidgetType.QUICK_STATS, true, 0),
            DashboardWidgetConfig(DashboardWidgetType.RECENT_NOTES, true, 1),
            DashboardWidgetConfig(DashboardWidgetType.PINNED_NOTES, true, 2),
            DashboardWidgetConfig(DashboardWidgetType.AI_SUGGESTIONS, true, 3),
            DashboardWidgetConfig(DashboardWidgetType.CATEGORIES_OVERVIEW, true, 4),
            DashboardWidgetConfig(DashboardWidgetType.PRODUCTIVITY_STATS, true, 5),
            DashboardWidgetConfig(DashboardWidgetType.UPCOMING_REMINDERS, true, 6),
            DashboardWidgetConfig(DashboardWidgetType.SEARCH_SHORTCUTS, true, 7)
        ),
        fabConfig = FABMenuConfig(
            style = FABMenuStyle.CIRCULAR,
            maxVisibleActions = 8,
            showLabels = true,
            actions = listOf(
                FABActionConfig(FABActionType.NEW_NOTE, true, 0),
                FABActionConfig(FABActionType.VOICE_NOTE, true, 1),
                FABActionConfig(FABActionType.CAMERA_NOTE, true, 2),
                FABActionConfig(FABActionType.SCAN_DOCUMENT, true, 3),
                FABActionConfig(FABActionType.AI_ASSIST, true, 4),
                FABActionConfig(FABActionType.WEB_CLIPPER, true, 5),
                FABActionConfig(FABActionType.QUICK_SEARCH, true, 6),
                FABActionConfig(FABActionType.BACKUP_NOW, true, 7)
            )
        )
    )
}