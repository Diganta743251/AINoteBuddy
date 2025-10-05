package com.ainotebuddy.app.ui.components.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.theme.LocalSpacing

/**
 * Composable for editing a single template variable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateVariableInput(
    variable: NoteTemplate.TemplateVariable,
    onVariableChange: (NoteTemplate.TemplateVariable) -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Variable name and type
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Variable name
                OutlinedTextField(
                    value = variable.name,
                    onValueChange = { 
                        onVariableChange(
                            variable.copy(name = it.filter { c -> c.isLetterOrDigit() || c == '_' })
                        )
                    },
                    label = { Text(stringResource(R.string.variable_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = spacing.small)
                )
                
                // Variable type dropdown
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .padding(start = spacing.small)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = variable.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            label = { Text(stringResource(R.string.type)) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            NoteTemplate.VariableType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        onVariableChange(variable.copy(type = type))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Remove button
                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .padding(start = spacing.small)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_variable),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Default value and required toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Default value input
                OutlinedTextField(
                    value = variable.defaultValue,
                    onValueChange = { 
                        onVariableChange(variable.copy(defaultValue = it))
                    },
                    label = { Text(stringResource(R.string.default_value)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (variable.type) {
                            NoteTemplate.VariableType.NUMBER -> KeyboardType.Number
                            NoteTemplate.VariableType.DATE -> KeyboardType.Number
                            NoteTemplate.VariableType.TIME -> KeyboardType.Number
                            else -> KeyboardType.Text
                        },
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = spacing.small)
                )
                
                // Required toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onVariableChange(variable.copy(isRequired = !variable.isRequired))
                        }
                        .padding(horizontal = spacing.small, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = variable.isRequired,
                        onCheckedChange = {
                            onVariableChange(variable.copy(isRequired = it))
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.required_field),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
            
            // Description
            if (variable.placeholder.isNotBlank() || true) {
                Spacer(modifier = Modifier.height(spacing.small))
                OutlinedTextField(
                    value = variable.placeholder,
                    onValueChange = { 
                        onVariableChange(variable.copy(placeholder = it))
                    },
                    label = { Text(stringResource(R.string.description_optional)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Type-specific options
            when (variable.type) {
                NoteTemplate.VariableType.CHOICE -> {
                    Spacer(modifier = Modifier.height(spacing.small))
                    ChoiceOptionsEditor(
                        choices = variable.options,
                        onChoicesChange = { newChoices ->
                            onVariableChange(variable.copy(options = newChoices))
                        }
                    )
                }
                NoteTemplate.VariableType.DATE -> {
                    // Date format selector could go here
                }
                NoteTemplate.VariableType.TIME -> {
                    // Time format selector could go here
                }
                else -> {}
            }
        }
    }
}

/**
 * Composable for editing choice options for a CHOICE variable type
 */
@Composable
private fun ChoiceOptionsEditor(
    choices: List<String>,
    onChoicesChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var newChoice by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium)
    ) {
        Text(
            text = stringResource(R.string.options),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        // List of choices
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(4.dp)
        ) {
            items(choices, key = { it }) { choice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = choice,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            onChoicesChange(choices - choice)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_choice),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (choice != choices.lastOrNull()) {
                    Divider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        
        // Add new choice
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small)
        ) {
            OutlinedTextField(
                value = newChoice,
                onValueChange = { newChoice = it },
                label = { Text(stringResource(R.string.add_choice)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = {
                    if (newChoice.isNotBlank() && newChoice !in choices) {
                        onChoicesChange(choices + newChoice)
                        newChoice = ""
                    }
                },
                enabled = newChoice.isNotBlank() && newChoice !in choices,
                modifier = Modifier.padding(start = spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_choice)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateVariableInputPreview_Components() {
    val variable = remember {
        NoteTemplate.TemplateVariable(
            name = "meeting_title",
            type = NoteTemplate.VariableType.TEXT,
            defaultValue = "Team Sync",
            placeholder = "The title of the meeting",
            isRequired = true
        )
    }
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text variable
            var textVar by remember { mutableStateOf(variable) }
            TemplateVariableInput(
                variable = textVar,
                onVariableChange = { textVar = it },
                onRemove = {}
            )
            
            // Choice variable
            var choiceVar by remember {
                mutableStateOf(
                    variable.copy(
                        type = NoteTemplate.VariableType.CHOICE,
                        options = listOf("Option 1", "Option 2", "Option 3"),
                        defaultValue = "Option 1"
                    )
                )
            }
            TemplateVariableInput(
                variable = choiceVar,
                onVariableChange = { choiceVar = it },
                onRemove = {}
            )
        }
    }
}
