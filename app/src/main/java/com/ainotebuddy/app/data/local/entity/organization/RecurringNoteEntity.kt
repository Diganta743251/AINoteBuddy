package com.ainotebuddy.app.data.local.entity.organization

import androidx.room.*
import com.ainotebuddy.app.data.model.organization.RecurringNote

/**
 * Room entity for RecurringNote
 */
@Entity(
    tableName = "recurring_notes",
    foreignKeys = [
        ForeignKey(
            entity = NoteTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class RecurringNoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "template_id")
    val templateId: String? = null,
    
    @ColumnInfo(name = "template_variables")
    val templateVariablesJson: String = "{}",
    
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String,
    
    @ColumnInfo(name = "start_date")
    val startDate: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,
    
    @ColumnInfo(name = "last_triggered")
    val lastTriggered: Long? = null,
    
    @ColumnInfo(name = "next_trigger")
    val nextTrigger: Long,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this entity to a domain model
     */
    fun toModel(): RecurringNote {
        return RecurringNote(
            id = id,
            title = title,
            templateId = templateId,
            templateVariables = emptyMap(), // Will be populated by the mapper
            recurrenceRule = RecurringNote.RecurrenceRule.valueOf(recurrenceRule),
            startDate = startDate,
            endDate = endDate,
            lastTriggered = lastTriggered,
            nextTrigger = nextTrigger,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Converts a domain model to an entity
     */
    companion object {
        fun fromModel(model: RecurringNote): RecurringNoteEntity {
            return RecurringNoteEntity(
                id = model.id,
                title = model.title,
                templateId = model.templateId,
                templateVariablesJson = "{}", // Will be handled by the mapper
                recurrenceRule = model.recurrenceRule.name,
                startDate = model.startDate,
                endDate = model.endDate,
                lastTriggered = model.lastTriggered,
                nextTrigger = model.nextTrigger,
                isActive = model.isActive,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt
            )
        }
    }
}

/**
 * Type converter for template variables map
 */
class TemplateVariablesMapConverter {
    @TypeConverter
    fun fromJson(json: String): Map<String, String> {
        // Implement JSON parsing using Moshi or Gson
        return emptyMap()
    }
    
    @TypeConverter
    fun toJson(variables: Map<String, String>): String {
        // Implement JSON serialization using Moshi or Gson
        return "{}"
    }
}
