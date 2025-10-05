package com.ainotebuddy.app.data.local.entity.organization

import androidx.room.*
import com.ainotebuddy.app.data.model.organization.NoteTemplate

/**
 * Room entity for NoteTemplate
 */
@Entity(tableName = "note_templates")
data class NoteTemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "icon")
    val icon: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "variables")
    val variablesJson: String = "[]",
    
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this entity to a domain model
     */
    fun toModel(): NoteTemplate {
        return NoteTemplate(
            id = id,
            name = name,
            description = description,
            icon = icon,
            category = category,
            content = content,
            variables = emptyList(), // Will be populated by the mapper
            isDefault = isDefault,
            isEnabled = isEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Converts a domain model to an entity
     */
    companion object {
        fun fromModel(model: NoteTemplate): NoteTemplateEntity {
            return NoteTemplateEntity(
                id = model.id,
                name = model.name,
                description = model.description,
                icon = model.icon,
                category = model.category,
                content = model.content,
                variablesJson = "[]", // Will be handled by the mapper
                isDefault = model.isDefault,
                isEnabled = model.isEnabled,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt
            )
        }
    }
}

/**
 * Type converter for TemplateVariable list
 */
class TemplateVariablesConverter {
    @TypeConverter
    fun fromJson(json: String): List<NoteTemplate.TemplateVariable> {
        // Implement JSON parsing using Moshi or Gson
        return emptyList()
    }
    
    @TypeConverter
    fun toJson(variables: List<NoteTemplate.TemplateVariable>): String {
        // Implement JSON serialization using Moshi or Gson
        return "[]"
    }
}
