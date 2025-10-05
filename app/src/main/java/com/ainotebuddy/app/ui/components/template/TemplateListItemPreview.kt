package com.ainotebuddy.app.ui.components.template

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme

@Preview(showBackground = true)
@Composable
fun TemplateListItemPreview() {
    val template = NoteTemplate(
        id = "template_meeting_notes",
        name = "Meeting Notes",
        description = "Template for capturing meeting notes with action items and decisions",
        icon = "📝",
        category = "Work",
        content = "# {{meeting_title}}\n\n## Attendees\n- {{attendee1}}\n- {{attendee2}}\n\n## Agenda\n1. {{agenda_item1}}\n2. {{agenda_item2}}\n\n## Decisions\n- {{decision1}}\n\n## Action Items\n- [ ] {{action_item1}} ({{owner}} by {{due_date}})",
        variables = listOf(
            NoteTemplate.TemplateVariable(
                name = "meeting_title",
                defaultValue = "Team Sync",
                placeholder = "Title of the meeting",
                isRequired = true,
                type = NoteTemplate.VariableType.TEXT
            ),
            NoteTemplate.TemplateVariable(
                name = "attendee1",
                defaultValue = "",
                placeholder = "First attendee name",
                isRequired = true,
                type = NoteTemplate.VariableType.TEXT
            ),
            NoteTemplate.TemplateVariable(
                name = "agenda_item1",
                defaultValue = "Project updates",
                placeholder = "First agenda item",
                isRequired = true,
                type = NoteTemplate.VariableType.TEXT
            )
        ),
        isDefault = true
    )
    
    AINoteBuddyTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Regular template item
            TemplateListItem(
                template = template,
                onClick = { },
                onDelete = { },
                onDuplicate = { },
                onShare = { }
            )
            
            // Template with long text
            TemplateListItem(
                template = template.copy(
                    name = "Very long template name that should be truncated with ellipsis",
                    description = "This is a very long description that should be truncated to two lines with ellipsis. It contains additional details about the template that might be useful for users to understand its purpose and usage.",
                    variables = emptyList()
                ),
                onClick = { },
                onDelete = { },
                onDuplicate = { },
                onShare = { }
            )
            
            // Minimal template
            TemplateListItem(
                template = template.copy(
                    name = "Quick Note",
                    description = "",
                    category = "Personal",
                    variables = emptyList()
                ),
                onClick = { },
                onDelete = null, // No delete action
                onDuplicate = null, // No duplicate action
                onShare = null // No share action
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E, uiMode = 2)
@Composable
fun TemplateListItemDarkPreview() {
    TemplateListItemPreview()
}
