package com.ainotebuddy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val category: String = "General",
    val tags: String = "", // Comma-separated tags
    val folderId: Long? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val color: Int = 0, // Note color theme
    val format: String = "plain", // plain, markdown, rich
    val wordCount: Int = 0,
    val readTime: Int = 0, // Estimated reading time in minutes
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val lastSyncedAt: Long = 0,
    val shareId: String = "", // For sharing notes
    val collaborators: String = "", // JSON array of user IDs
    val version: Int = 1, // For conflict resolution
    val isInVault: Boolean = false, // Add this line for encrypted vaults
    val reminderTime: Long? = null
)
