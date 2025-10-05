package com.ainotebuddy.app.offline

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Entity representing an offline operation for Enhanced Offline-First Architecture
 */
@Entity(tableName = "offline_operations")
@TypeConverters(OfflineOperationConverters::class)
data class OfflineOperationEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String, // CREATE, UPDATE, DELETE, SYNC, etc.
    val entityType: String, // note, tag, folder, etc.
    val entityId: String, // ID of the entity being operated on
    val status: String, // PENDING, SUCCESS, FAILED, CANCELLED
    val priority: Int = 0, // 0 = high, 1 = medium, 2 = low
    val timestamp: Long = System.currentTimeMillis(),
    val scheduledAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val networkRequirement: String = "ANY", // ANY, WIFI, CELLULAR, NONE
    val estimatedSize: Long = 0, // Estimated data size in bytes
    val data: String? = null, // JSON payload for the operation
    val dependencies: List<String> = emptyList(), // IDs of operations this depends on
    val errorMessage: String? = null,
    val conflictResolutionStrategy: String = "MANUAL", // MANUAL, AUTO_MERGE, PREFER_LOCAL, PREFER_REMOTE
    val metadata: Map<String, String> = emptyMap() // Additional metadata
)

class OfflineOperationConverters {
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