package com.ainotebuddy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a smart folder rule for automatic note organization
 */
data class SmartFolderRule(
    val field: String, // "title", "content", "tags", "createdAt", "updatedAt"
    val operator: String, // "contains", "equals", "startsWith", "endsWith", "matches", "before", "after", "on", "between"
    val value: String,
    val caseSensitive: Boolean = false
)

/**
 * Entity for storing smart folder configurations
 */
@Entity(tableName = "smart_folders")
data class SmartFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val rules: String, // JSON serialized list of SmartFolderRule
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)