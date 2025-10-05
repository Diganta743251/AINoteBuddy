package com.ainotebuddy.app.analytics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_activities")
data class NoteActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val activityType: String, // CREATE, UPDATE, DELETE, VIEW, etc.
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0, // in milliseconds
    val metadata: String? = null // JSON for additional data
)