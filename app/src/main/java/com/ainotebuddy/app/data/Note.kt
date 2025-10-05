package com.ainotebuddy.app.data

/**
 * Domain model for notes (not a Room entity)
 */
data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isStarred: Boolean = false,
    val isEncrypted: Boolean = false,
    val encryptionMetadata: String? = null,
    val encryptedContent: String? = null
)