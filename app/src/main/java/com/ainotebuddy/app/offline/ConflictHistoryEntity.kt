package com.ainotebuddy.app.offline

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Entity representing the history of conflict resolution attempts
 */
@Entity(tableName = "conflict_history")
@TypeConverters(ConflictHistoryConverters::class)
data class ConflictHistoryEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val entityType: String, // note, tag, folder, etc.
    val entityId: String, // ID of the entity that had conflicts
    val conflictType: String = "DATA_CONFLICT", // DATA_CONFLICT, SCHEMA_CONFLICT, SYNC_CONFLICT
    val conflictDetails: String, // JSON details of the conflict
    val localData: String? = null, // Local version data
    val remoteData: String? = null, // Remote version data
    val resolution: String = "MANUAL", // MANUAL, AUTO_MERGE, PREFER_LOCAL, PREFER_REMOTE
    val resolvedAt: Long = System.currentTimeMillis(),
    val resolvedBy: String? = null, // User ID or system that resolved it
    val confidence: Float = 0.0f, // Confidence in the resolution (0.0 - 1.0)
    val mergedData: String? = null, // Final merged data
    val isResolved: Boolean = true,
    val resolutionNotes: String? = null,
    val affectedFields: List<String> = emptyList(), // Fields that were in conflict
    val resolutionStrategy: Map<String, String> = emptyMap() // Strategy details per field
)

class ConflictHistoryConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String> = value?.let {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson<List<String>>(it, type)
    } ?: emptyList()

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> = value?.let {
        val type = object : TypeToken<Map<String, String>>() {}.type
        gson.fromJson<Map<String, String>>(it, type)
    } ?: emptyMap()
}

/**
 * Enums for better type safety (renamed to avoid collision with other offline models)
 */
enum class HistoryConflictType {
    DATA_CONFLICT,
    SCHEMA_CONFLICT,
    SYNC_CONFLICT,
    BUSINESS_RULE_CONFLICT
}

enum class HistoryResolution {
    MANUAL,
    AUTO_MERGE,
    PREFER_LOCAL,
    PREFER_REMOTE,
    CUSTOM_STRATEGY
}