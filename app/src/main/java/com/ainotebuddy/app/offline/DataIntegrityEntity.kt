package com.ainotebuddy.app.offline

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Entity representing data integrity check results
 */
@Entity(tableName = "data_integrity_checks")
@TypeConverters(DataIntegrityConverters::class)
data class DataIntegrityEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val entityType: String, // note, tag, folder, etc.
    val entityId: String, // ID of the checked entity
    val validatedAt: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val validationRules: List<String> = emptyList(), // Rules that were checked
    val failedRules: List<String> = emptyList(), // Rules that failed
    val validationDetails: Map<String, String> = emptyMap(), // Detailed validation info
    val correctionApplied: Boolean = false,
    val correctionDetails: String? = null,
    val checksum: String? = null, // For data consistency checks
    val schemaVersion: String? = null, // For migration validation
    val severity: String = "INFO", // INFO, WARNING, ERROR, CRITICAL
    val autoFixable: Boolean = false,
    val fixDescription: String? = null
)

/**
 * Converters local to DataIntegrityEntity for lists and maps
 */
class DataIntegrityConverters {
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
 * Data class for integrity check statistics (used in DAO queries)
 */
data class IntegrityStatistic(
    val entityType: String,
    val count: Int
)

/**
 * Enums for better type safety
 */
enum class ValidationSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

enum class ValidationRule {
    SCHEMA_COMPLIANCE,
    FOREIGN_KEY_INTEGRITY,
    DATA_CONSISTENCY,
    CHECKSUM_VALIDATION,
    BUSINESS_RULE_VALIDATION
}