package com.ainotebuddy.app.ui.components.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.theme.LocalSpacing

/**
 * Displays a card for a generated template with preview and options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedTemplateCard(
    template: NoteTemplate,
    onUseTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current
    
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
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
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) {
                            stringResource(R.string.collapse)
                        } else {
                            stringResource(R.string.expand)
                        },
                    )
                }
            }
            
            // Category and description
            if (template.category.isNotBlank() || template.description.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.small)
                ) {
                    if (template.category.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = template.category,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    if (template.description.isNotBlank()) {
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Template preview (collapsed)
            if (!isExpanded) {
                Text(
                    text = template.content.take(150).let { content ->
                        if (content.length < template.content.length) "$content..." else content
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Expanded template content
            if (isExpanded) {
                // Template content
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.small)
                ) {
                    Text(
                        text = template.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(spacing.medium)
                    )
                }
                
                // Variables section
                if (template.variables.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.variables),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.small)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.small)
                    ) {
                        items(template.variables) { variable ->
                            VariableItem(variable = variable)
                        }
                    }
                }
            }
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { /* TODO: Show preview */ },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = stringResource(R.string.preview))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onUseTemplate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.use_template))
                }
            }
        }
    }
}

/**
 * Displays a form for creating or editing a template
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditor(
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
        // Template name
        OutlinedTextField(
            value = template.name,
            onValueChange = { onTemplateChange(template.copy(name = it)) },
            label = { Text(stringResource(R.string.template_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        // Template description
        OutlinedTextField(
            value = template.description,
            onValueChange = { onTemplateChange(template.copy(description = it)) },
            label = { Text(stringResource(R.string.description_optional)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        // Template category
        var categoryExpanded by remember { mutableStateOf(false) }
        val categories = listOf(
            "Meeting Notes",
            "Project",
            "Personal",
            "Work",
            "Ideas",
            "Journal",
            "Checklist",
            "Other"
        )
        
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = template.category,
                onValueChange = { onTemplateChange(template.copy(category = it)) },
                label = { Text(stringResource(R.string.category_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                readOnly = true
            )
            
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onTemplateChange(template.copy(category = category))
                            categoryExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        // Template content
        Text(
            text = stringResource(R.string.template_content),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        // Custom text field for template content with syntax highlighting for variables
        var content by remember { mutableStateOf(template.content) }
        val focusRequester = remember { FocusRequester() }
        
        LaunchedEffect(template) {
            content = template.content
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 240.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                )
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(1.dp)
        ) {
            // Syntax highlighting overlay
            val processedContent = remember(content) {
                // This is a simplified implementation
                // In a real app, you'd want to parse the content and highlight variables
                content
            }
            
            BasicTextField(
                value = content,
                onValueChange = {
                    content = it
                    onTemplateChange(template.copy(content = it))
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Variable helper
        Text(
            text = stringResource(R.string.variable_helper_text),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = spacing.medium)
        )
        
        // Variables section
        Text(
            text = stringResource(R.string.variables),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .padding(vertical = spacing.small)
        ) {
            items(template.variables) { variable ->
                VariableEditor(
                    variable = variable,
                    onVariableChange = { updatedVar ->
                        val updatedVars = template.variables.toMutableList().apply {
                            val index = indexOfFirst { it.name == updatedVar.name }
                            if (index >= 0) {
                                set(index, updatedVar)
                            } else {
                                add(updatedVar)
                            }
                        }
                        onTemplateChange(template.copy(variables = updatedVars))
                    },
                    onDelete = {
                        val updatedVars = template.variables.toMutableList().apply {
                            removeIf { it.name == variable.name }
                        }
                        onTemplateChange(template.copy(variables = updatedVars))
                    }
                )
            }
        }
        
        // Add variable button
        Button(
            onClick = {
                val newVarName = "var_${template.variables.size + 1}"
                val newVar = NoteTemplate.TemplateVariable(
                    name = newVarName,
                    defaultValue = "",
                    placeholder = ""
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
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.add_variable))
        }
    }
}

/**
 * Displays a variable in a template with its name and description
 */
@Composable
private fun VariableItem(
    variable: NoteTemplate.TemplateVariable,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Variable name
            Text(
                text = "{{${variable.name}}}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )
            
            // Default value (if any)
            if (variable.defaultValue.isNotBlank()) {
                Text(
                    text = variable.defaultValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }
            
            // Description (if any)
            if (variable.placeholder.isNotBlank()) {
                Text(
                    text = variable.placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

/**
 * Editor for a single template variable
 */
@Composable
private fun VariableEditor(
    variable: NoteTemplate.TemplateVariable,
    onVariableChange: (NoteTemplate.TemplateVariable) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
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
                // Variable name with {{ }} syntax
                Text(
                    text = "{{",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Variable name input
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
                )
                
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
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Default value
            OutlinedTextField(
                value = variable.defaultValue,
                onValueChange = { value ->
                    onVariableChange(variable.copy(defaultValue = value))
                },
                label = { Text(stringResource(R.string.default_value_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Description
            OutlinedTextField(
                value = variable.placeholder,
                onValueChange = { desc ->
                    onVariableChange(variable.copy(placeholder = desc))
                },
                label = { Text(stringResource(R.string.description_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Preview for a template with variables filled in
 */
@Composable
fun TemplatePreview(
    template: NoteTemplate,
    variableValues: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val processedContent = remember(template.content, variableValues) {
        var result = template.content
        template.variables.forEach { variable ->
            val value = variableValues[variable.name] ?: variable.defaultValue
            result = result.replace("{{${variable.name}}}", value, ignoreCase = false)
        }
        result
    }
    
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = processedContent.lines().firstOrNull() ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Content
            Text(
                text = processedContent.lines().drop(1).joinToString("\n").trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
