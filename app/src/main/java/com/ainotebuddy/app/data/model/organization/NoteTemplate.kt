package com.ainotebuddy.app.data.model.organization

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * Represents a template for creating new notes with predefined content and structure
 */
@Entity(tableName = "note_templates")
data class NoteTemplate(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val icon: String,
    val category: String,
    val content: String,
    val variables: List<TemplateVariable> = emptyList(),
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Represents a variable that can be replaced in the template
     */
    data class TemplateVariable(
        val name: String,
        val defaultValue: String = "",
        val placeholder: String = "",
        val isRequired: Boolean = true,
        // Optional type and options to support UI screens using CHOICE etc.
        val type: VariableType = VariableType.TEXT,
        val options: List<String> = emptyList()
    )

    enum class VariableType {
        TEXT,
        NUMBER,
        DATE,
        TIME,
        CHOICE
    }
}

/**
 * Default note templates that are available to all users
 */
val defaultNoteTemplates = listOf(
    NoteTemplate(
        id = "template_meeting_notes",
        name = "Meeting Notes",
        description = "Template for taking meeting minutes",
        icon = "📝",
        category = "Work",
        content = """# {{meeting_title}}
          |
          |**Date:** {{date}}
          |**Time:** {{time}}
          |**Location:** {{location}}
          |**Attendees:** {{attendees}}
          |
          |## Agenda
          |1. 
          |
          |## Discussion Points
          |### 
          |- 
          |
          |## Action Items
          |- [ ] 
          |
          |## Next Meeting
          |**Date:** {{next_meeting_date}}
          |**Time:** {{next_meeting_time}}
        """.trimMargin(),
        variables = listOf(
            NoteTemplate.TemplateVariable("meeting_title", "Team Meeting", "Enter meeting title"),
            NoteTemplate.TemplateVariable("date", "", "MM/DD/YYYY"),
            NoteTemplate.TemplateVariable("time", "", "HH:MM AM/PM"),
            NoteTemplate.TemplateVariable("location", "Conference Room A", "Meeting location"),
            NoteTemplate.TemplateVariable("attendees", "", "Comma-separated list of attendees"),
            NoteTemplate.TemplateVariable("next_meeting_date", "", "Next meeting date"),
            NoteTemplate.TemplateVariable("next_meeting_time", "", "Next meeting time")
        ),
        isDefault = true
    ),
    NoteTemplate(
        id = "template_quick_note",
        name = "Quick Note",
        description = "Simple note template for quick thoughts",
        icon = "✏️",
        category = "Personal",
        content = "# {{title}}\n\n{{content}}",
        variables = listOf(
            NoteTemplate.TemplateVariable("title", "", "Note title"),
            NoteTemplate.TemplateVariable("content", "", "Your note content here...")
        ),
        isDefault = true
    ),
    NoteTemplate(
        id = "template_project_plan",
        name = "Project Plan",
        description = "Template for project planning and tracking",
        icon = "📊",
        category = "Work",
        content = """# {{project_name}}
          |
          |**Project Owner:** {{owner}}
          |**Start Date:** {{start_date}}
          |**Target Date:** {{target_date}}
          |**Status:** {{status}}
          |
          |## Project Overview
          |{{overview}}
          |
          |## Goals & Objectives
          |1. 
          |
          |## Key Deliverables
          |- [ ] 
          |
          |## Timeline
          |### Phase 1: Planning
          |- [ ] 
          |
          |### Phase 2: Execution
          |- [ ] 
          |
          |### Phase 3: Review
          |- [ ] 
          |
          |## Resources
          |- 
          |
          |## Notes
          |- 
        """.trimMargin(),
        variables = listOf(
            NoteTemplate.TemplateVariable("project_name", "", "Project name"),
            NoteTemplate.TemplateVariable("owner", "", "Project owner"),
            NoteTemplate.TemplateVariable("start_date", "", "Start date"),
            NoteTemplate.TemplateVariable("target_date", "", "Target completion date"),
            NoteTemplate.TemplateVariable("status", "Not Started", "Project status"),
            NoteTemplate.TemplateVariable("overview", "", "Project overview")
        ),
        isDefault = true
    )
)
