package com.ainotebuddy.app.analytics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_usage")
data class TagUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tagName: String,
    val noteId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val usageType: String = "APPLIED" // APPLIED, REMOVED, etc.
)