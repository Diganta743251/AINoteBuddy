package com.ainotebuddy.app.data.model.organization

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * Represents a smart folder that automatically categorizes notes based on AI analysis
 */
@Entity(tableName = "smart_folders")
data class SmartFolder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val icon: String,
    val color: Int,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val rules: List<SmartFolderRule> = emptyList(),
    val noteCount: Int = 0
) {
    /**
     * Represents a rule that defines which notes belong to this smart folder
     */
    data class SmartFolderRule(
        val field: String, // e.g., "title", "content", "tags", "createdAt"
        val operator: String, // e.g., "contains", "equals", "startsWith", "in", "after", "before"
        val value: String, // The value to compare against
        val isAIEnabled: Boolean = false // Whether to use AI for matching
    )
}

/**
 * Default smart folders that are available to all users
 */
val defaultSmartFolders = listOf(
    SmartFolder(
        id = "smart_folder_recent",
        name = "Recent",
        description = "Recently viewed or edited notes",
        icon = "⏱️",
        color = 0xFF2196F3.toInt(),
        rules = listOf(
            SmartFolder.SmartFolderRule(
                field = "updatedAt",
                operator = "after",
                value = (System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)).toString()
            )
        )
    ),
    SmartFolder(
        id = "smart_folder_important",
        name = "Important",
        description = "Important notes with high priority",
        icon = "⭐",
        color = 0xFFFFC107.toInt(),
        rules = listOf(
            SmartFolder.SmartFolderRule(
                field = "tags",
                operator = "contains",
                value = "important",
                isAIEnabled = true
            )
        )
    ),
    SmartFolder(
        id = "smart_folder_work",
        name = "Work",
        description = "Work-related notes and tasks",
        icon = "💼",
        color = 0xFF4CAF50.toInt(),
        rules = listOf(
            SmartFolder.SmartFolderRule(
                field = "content",
                operator = "contains",
                value = "work",
                isAIEnabled = true
            )
        )
    )
)
