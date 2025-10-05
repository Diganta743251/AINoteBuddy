package com.ainotebuddy.app.data.model.analytics

import java.time.LocalDateTime

data class NoteActivity(
    val noteId: Long,
    val timestamp: LocalDateTime,
    val activityType: ActivityType,
    val duration: Long = 0, // in milliseconds
    val wordCount: Int = 0,
    val characterCount: Int = 0
)

enum class ActivityType {
    CREATED,
    UPDATED,
    VIEWED,
    EDITED,
    DELETED
}

data class TagUsage(
    val tag: String,
    val count: Int,
    val lastUsed: LocalDateTime
)

data class ActivityHeatmap(
    val date: String, // YYYY-MM-DD format
    val hour: Int, // 0-23
    val activityCount: Int
)

data class ProductivityReport(
    val period: String,
    val notesCreated: Int,
    val wordsWritten: Int,
    val mostActiveDay: String,
    val mostUsedTags: List<TagUsage>,
    val activityHeatmap: List<ActivityHeatmap>
)
