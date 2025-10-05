package com.ainotebuddy.app.data.model

/**
 * Lightweight UI Note model used by AI Assistant panel and editor.
 * This intentionally differs from the domain Note (com.ainotebuddy.app.data.Note).
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val tags: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
