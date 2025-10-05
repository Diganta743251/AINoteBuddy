package com.ainotebuddy.app.data.local.analytics

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.NoteActivity
import com.ainotebuddy.app.data.model.analytics.TagUsage
import java.time.LocalDateTime

@Database(
    entities = [NoteActivityEntity::class, TagUsageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AnalyticsDatabase : RoomDatabase() {
    abstract fun noteActivityDao(): NoteActivityDao
    abstract fun tagUsageDao(): TagUsageDao
}

@Entity(tableName = "note_activities")
data class NoteActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val timestamp: LocalDateTime,
    val activityType: ActivityType,
    val duration: Long = 0,
    val wordCount: Int = 0,
    val characterCount: Int = 0
) {
    fun toModel() = NoteActivity(
        noteId = noteId,
        timestamp = timestamp,
        activityType = activityType,
        duration = duration,
        wordCount = wordCount,
        characterCount = characterCount
    )
}

@Entity(tableName = "tag_usages")
data class TagUsageEntity(
    @PrimaryKey val tag: String,
    val count: Int = 0,
    val lastUsed: LocalDateTime
) {
    fun toModel() = TagUsage(
        tag = tag,
        count = count,
        lastUsed = lastUsed
    )
}

fun NoteActivity.toEntity() = NoteActivityEntity(
    noteId = noteId,
    timestamp = timestamp,
    activityType = activityType,
    duration = duration,
    wordCount = wordCount,
    characterCount = characterCount
)