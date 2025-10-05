package com.ainotebuddy.app.ui.components.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.theme.LocalSpacing

/**
 * Composable for editing template variables
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateVariableEditor(
    template: NoteTemplate,
    onTemplateChange: (NoteTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.template_variables),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Add variable button
            FilledTonalIconButton(
                onClick = {
                    val newVarName = "var_${template.variables.size + 1}"
                    val newVar = NoteTemplate.TemplateVariable(
                        name = newVarName,
                        type = NoteTemplate.VariableType.TEXT,
                        defaultValue = "",
                        placeholder = "",
                        isRequired = false
                    )
                    
                    onTemplateChange(
                        template.copy(
                            variables = template.variables + newVar,
                            content = if (template.content.isBlank()) {
                                "{{$newVarName}}"
                            } else {
                                template.content
                            }
                        )
                    )
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_variable)
                )
            }
        }
        
        // Variables list
        if (template.variables.isEmpty()) {
            EmptyVariablesView(
                onAddVariable = {
                    val newVarName = "var_1"
                    val newVar = NoteTemplate.TemplateVariable(
                        name = newVarName,
                        type = NoteTemplate.VariableType.TEXT,
                        defaultValue = "",
                        placeholder = "",
                        isRequired = false
                    )
                    
                    onTemplateChange(
                        template.copy(
                            variables = listOf(newVar),
                            content = "{{$newVarName}}"
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.large)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.medium)
            ) {
                items(template.variables) { variable ->
                    VariableItem(
                        variable = variable,
                        onVariableChange = { updatedVar ->
                            val updatedVars = template.variables.toMutableList().apply {
                                val index = indexOfFirst { it.name == updatedVar.name }
                                if (index >= 0) {
                                    set(index, updatedVar)
                                }
                            }
                            onTemplateChange(template.copy(variables = updatedVars))
                        },
                        onDelete = {
                            val updatedVars = template.variables.toMutableList().apply {
                                removeIf { it.name == variable.name }
                            }
                            
                            // Remove variable references from content
                            var updatedContent = template.content
                            updatedContent = updatedContent.replace("{{${variable.name}}}", "")
                            
                            onTemplateChange(
                                template.copy(
                                    variables = updatedVars,
                                    content = updatedContent
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariableItem(
    variable: NoteTemplate.TemplateVariable,
    onVariableChange: (NoteTemplate.TemplateVariable) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Header with name and delete button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = variable.name,
                    onValueChange = { name ->
                        onVariableChange(
                            variable.copy(
                                name = name.filter { it.isLetterOrDigit() || it == '_' }
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.variable_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(spacing.small))
                
                // Variable type selector
                var typeExpanded by remember { mutableStateOf(false) }
                val types = NoteTemplate.VariableType.values()
                
                Box {
                    OutlinedButton(
                        onClick = { typeExpanded = true },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(
                            text = variable.type.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                    
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }) },
                                onClick = {
                                    onVariableChange(variable.copy(type = type))
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_variable),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            // Variable type-specific options
            when (variable.type) {
                NoteTemplate.VariableType.TEXT -> {
                    // Default text field
                    OutlinedTextField(
                        value = variable.defaultValue,
                        onValueChange = { value ->
                            onVariableChange(variable.copy(defaultValue = value))
                        },
                        label = { Text(stringResource(R.string.default_value)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                
                NoteTemplate.VariableType.NUMBER -> {
                    // Number input
                    OutlinedTextField(
                        value = variable.defaultValue.ifEmpty { "0" },
                        onValueChange = { value ->
                            if (value.isEmpty() || value.matches(Regex("-?\\d*"))) {
                                onVariableChange(variable.copy(defaultValue = value))
                            }
                        },
                        label = { Text(stringResource(R.string.default_value)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                
                NoteTemplate.VariableType.DATE -> {
                    // Date picker
                    OutlinedButton(
                        onClick = { /* Show date picker */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (variable.defaultValue.isNotEmpty()) {
                                variable.defaultValue
                            } else {
                                stringResource(R.string.select_date)
                            },
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                }
                
                NoteTemplate.VariableType.TIME -> {
                    // Time picker
                    OutlinedButton(
                        onClick = { /* Show time picker */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (variable.defaultValue.isNotEmpty()) {
                                variable.defaultValue
                            } else {
                                stringResource(R.string.select_time)
                            },
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                }
                
                NoteTemplate.VariableType.CHOICE -> {
                    // Choices list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Add choice button
                        OutlinedButton(
                            onClick = {
                                val newChoices = variable.options.toMutableList()
                                newChoices.add("")
                                onVariableChange(
                                    variable.copy(
                                        options = newChoices,
                                        defaultValue = if (newChoices.size == 1) "" else variable.defaultValue
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(stringResource(R.string.add_choice))
                        }
                        
                        // Choices list
                        variable.options.forEachIndexed { index, choice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.small)
                            ) {
                                RadioButton(
                                    selected = variable.defaultValue == choice,
                                    onClick = {
                                        onVariableChange(
                                            variable.copy(defaultValue = choice)
                                        )
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = choice,
                                    onValueChange = { value ->
                                        val newChoices = variable.options.toMutableList()
                                        newChoices[index] = value
                                        onVariableChange(
                                            variable.copy(
                                                options = newChoices,
                                                defaultValue = if (variable.defaultValue == choice) value else variable.defaultValue
                                            )
                                        )
                                    },
                                    label = { Text("Choice ${index + 1}") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    trailingIcon = {
                                        if (variable.options.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    val newChoices = variable.options.toMutableList()
                                                    newChoices.removeAt(index)
                                                    onVariableChange(
                                                        variable.copy(
                                                            options = newChoices,
                                                            defaultValue = if (variable.defaultValue == choice) "" else variable.defaultValue
                                                        )
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.remove_choice),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            // Description
            OutlinedTextField(
                value = variable.placeholder,
                onValueChange = { description ->
                    onVariableChange(variable.copy(placeholder = description))
                },
                label = { Text(stringResource(R.string.description_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            // Required toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.small)
            ) {
                Checkbox(
                    checked = variable.isRequired,
                    onCheckedChange = { isRequired ->
                        onVariableChange(variable.copy(isRequired = isRequired))
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                
                Text(
                    text = stringResource(R.string.required_field),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EmptyVariablesView(
    onAddVariable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAddVariable)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.large)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            Text(
                text = stringResource(R.string.no_variables_defined),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            Text(
                text = stringResource(R.string.click_to_add_first_variable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
