package com.ainotebuddy.app.ui.components.template

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme

@Preview(showBackground = true)
@Composable
fun TemplateVariableInputPreview() {
    // Text variable
    val textVariable = NoteTemplate.TemplateVariable(
        name = "meeting_title",
        type = NoteTemplate.VariableType.TEXT,
        defaultValue = "Team Sync",
        placeholder = "The title of the meeting",
        isRequired = true
    )
    
    // Number variable
    val numberVariable = NoteTemplate.TemplateVariable(
        name = "participant_count",
        type = NoteTemplate.VariableType.NUMBER,
        defaultValue = "5",
        placeholder = "Number of participants",
        isRequired = true
    )
    
    // Date variable
    val dateVariable = NoteTemplate.TemplateVariable(
        name = "meeting_date",
        type = NoteTemplate.VariableType.DATE,
        defaultValue = "2023-12-31",
        placeholder = "Date of the meeting",
        isRequired = true
    )
    
    // Time variable
    val timeVariable = NoteTemplate.TemplateVariable(
        name = "meeting_time",
        type = NoteTemplate.VariableType.TIME,
        defaultValue = "14:30",
        placeholder = "Time of the meeting",
        isRequired = false
    )
    
    // Choice variable
    val choiceVariable = NoteTemplate.TemplateVariable(
        name = "meeting_type",
        type = NoteTemplate.VariableType.CHOICE,
        defaultValue = "In-person",
        placeholder = "Type of meeting",
        isRequired = true,
        options = listOf("In-person", "Video Call", "Phone Call", "Hybrid")
    )
    
    AINoteBuddyTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text variable preview
            var textVar by remember { mutableStateOf(textVariable) }
            TemplateVariableInput(
                variable = textVar,
                onVariableChange = { textVar = it },
                onRemove = {}
            )
            
            // Number variable preview
            var numberVar by remember { mutableStateOf(numberVariable) }
            TemplateVariableInput(
                variable = numberVar,
                onVariableChange = { numberVar = it },
                onRemove = {}
            )
            
            // Date variable preview
            var dateVar by remember { mutableStateOf(dateVariable) }
            TemplateVariableInput(
                variable = dateVar,
                onVariableChange = { dateVar = it },
                onRemove = {}
            )
            
            // Time variable preview
            var timeVar by remember { mutableStateOf(timeVariable) }
            TemplateVariableInput(
                variable = timeVar,
                onVariableChange = { timeVar = it },
                onRemove = {}
            )
            
            // Choice variable preview
            var choiceVar by remember { mutableStateOf(choiceVariable) }
            TemplateVariableInput(
                variable = choiceVar,
                onVariableChange = { choiceVar = it },
                onRemove = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E, uiMode = 2)
@Composable
fun TemplateVariableInputDarkPreview() {
    val variable = NoteTemplate.TemplateVariable(
        name = "meeting_title",
        type = NoteTemplate.VariableType.TEXT,
        defaultValue = "Team Sync",
        placeholder = "The title of the meeting",
        isRequired = true
    )
    
    var state by remember { mutableStateOf(variable) }
    
    AINoteBuddyTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TemplateVariableInput(
                variable = state,
                onVariableChange = { state = it },
                onRemove = {}
            )
        }
    }
}
