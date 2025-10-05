package com.ainotebuddy.app.data.local.entity.organization

import androidx.room.*
import com.ainotebuddy.app.data.model.organization.SmartFolder

/**
 * Room entity for SmartFolder
 */
@Entity(tableName = "smart_folders")
data class SmartFolderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "icon")
    val icon: String,
    
    @ColumnInfo(name = "color")
    val color: Int,
    
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "rules")
    val rulesJson: String = "[]"
) {
    /**
     * Converts this entity to a domain model
     */
    fun toModel(): SmartFolder {
        return SmartFolder(
            id = id,
            name = name,
            description = description,
            icon = icon,
            color = color,
            isEnabled = isEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt,
            rules = emptyList() // Will be populated by the mapper
        )
    }
    
    /**
     * Converts a domain model to an entity
     */
    companion object {
        fun fromModel(model: SmartFolder): SmartFolderEntity {
            return SmartFolderEntity(
                id = model.id,
                name = model.name,
                description = model.description,
                icon = model.icon,
                color = model.color,
                isEnabled = model.isEnabled,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                rulesJson = "[]" // Will be handled by the mapper
            )
        }
    }
}

/**
 * Type converter for SmartFolder rules
 */
class SmartFolderRulesConverter {
    @TypeConverter
    fun fromJson(json: String): List<SmartFolder.SmartFolderRule> {
        // Implement JSON parsing using Moshi or Gson
        return emptyList()
    }
    
    @TypeConverter
    fun toJson(rules: List<SmartFolder.SmartFolderRule>): String {
        // Implement JSON serialization using Moshi or Gson
        return "[]"
    }
}
