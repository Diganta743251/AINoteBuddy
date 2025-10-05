package com.ainotebuddy.app.ui.screens.template.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TemplateVariableInput(
    variable: NoteTemplate.TemplateVariable,
    onVariableChange: (NoteTemplate.TemplateVariable) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = spacing()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    // Track if we're in edit mode for the variable name
    var isEditingName by remember { mutableStateOf(false) }
    
    // Track if we're showing the options dialog for CHOICE type
    var showOptionsDialog by remember { mutableStateOf(false) }
    
    // Track the new option being added
    var newOption by remember { mutableStateOf("") }
    
    // Auto-focus the name field when editing starts
    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            focusRequester.requestFocus()
        }
    }
    
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Header row with variable name, type, and delete button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Variable name with edit capability
                if (isEditingName) {
                    BasicTextField(
                        value = variable.name,
                        onValueChange = { newName ->
                            onVariableChange(variable.copy(name = newName))
                        },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                        innerTextField()
                                    
                                    // Show hint when empty
                                    if (variable.name.isEmpty()) {
                                        Text(
                                            "variable_name",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                                
                                // Done button
                                TextButton(
                                    onClick = { 
                                        isEditingName = false 
                                        // Ensure name is not empty
                                        if (variable.name.isBlank()) {
                                            onVariableChange(variable.copy(name = "var_${System.currentTimeMillis()}"))
                                        }
                                    },
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Text("Done")
                                }
                            }
                        }
                    )
                } else {
                    // Display variable name as clickable text
                    Text(
                        text = "{{${variable.name}}}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isEditingName = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Variable type dropdown
                var expanded by remember { mutableStateOf(false) }
                val types = NoteTemplate.VariableType.values()
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.width(120.dp)
                ) {
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = null,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = variable.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(200.dp)
                    ) {
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    ) 
                                },
                                onClick = {
                                    onVariableChange(variable.copy(type = type))
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (type) {
                                            NoteTemplate.VariableType.TEXT -> Icons.Default.TextFields
                                            NoteTemplate.VariableType.NUMBER -> Icons.Default.Numbers
                                            NoteTemplate.VariableType.DATE -> Icons.Default.DateRange
                                            NoteTemplate.VariableType.TIME -> Icons.Default.Schedule
                                            NoteTemplate.VariableType.CHOICE -> Icons.Default.List
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    leadingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
                
                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_variable),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Description field
            OutlinedTextField(
                value = variable.placeholder,
                onValueChange = { newDescription ->
                    onVariableChange(variable.copy(placeholder = newDescription))
                },
                label = { Text("Description (optional)") },
                placeholder = { Text("What's this variable for?") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.small)
            )
            
            // Required toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.medium)
            ) {
                Text(
                    text = "Required",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                Switch(
                    checked = variable.isRequired,
                    onCheckedChange = { isChecked ->
                        onVariableChange(variable.copy(isRequired = isChecked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            
            // Dynamic input based on variable type
            when (variable.type) {
                NoteTemplate.VariableType.TEXT -> {
                    OutlinedTextField(
                        value = variable.defaultValue,
                        onValueChange = { newValue ->
                            onVariableChange(variable.copy(defaultValue = newValue))
                        },
                        label = { Text("Default value (optional)") },
                        placeholder = { Text("Enter default text") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.medium)
                    )
                }
                
                NoteTemplate.VariableType.NUMBER -> {
                    OutlinedTextField(
                        value = variable.defaultValue,
                        onValueChange = { newValue ->
                            // Only allow numbers
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*$"))) {
                                onVariableChange(variable.copy(defaultValue = newValue))
                            }
                        },
                        label = { Text("Default value (optional)") },
                        placeholder = { Text("Enter default number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.medium)
                    )
                }
                
                NoteTemplate.VariableType.DATE -> {
                    // Date picker will be implemented here
                    OutlinedButton(
                        onClick = { /* TODO: Show date picker */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.medium),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (variable.defaultValue.isNotBlank()) {
                                variable.defaultValue
                            } else {
                                "Select default date (optional)"
                            }
                        )
                    }
                }
                
                NoteTemplate.VariableType.TIME -> {
                    // Time picker will be implemented here
                    OutlinedButton(
                        onClick = { /* TODO: Show time picker */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.medium),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (variable.defaultValue.isNotBlank()) {
                                variable.defaultValue
                            } else {
                                "Select default time (optional)"
                            }
                        )
                    }
                }
                
                NoteTemplate.VariableType.CHOICE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.medium)
                    ) {
                        // Display current options
                        if (variable.options.isNotEmpty()) {
                            Text(
                                text = "Options:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                items(variable.options) { option ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                start = 12.dp,
                                                top = 6.dp,
                                                end = 4.dp,
                                                bottom = 6.dp
                                            )
                                        ) {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            IconButton(
                                                onClick = {
                                                    val newOptions = variable.options.toMutableList()
                                                        .apply { remove(option) }
                                                    onVariableChange(
                                                        variable.copy(options = newOptions)
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Add option button
                        OutlinedButton(
                            onClick = { showOptionsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add option")
                        }
                    }
                }
            }
        }
    }
    
    // Options dialog for CHOICE type
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Add Option") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newOption,
                        onValueChange = { newOption = it },
                        label = { Text("Option text") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (variable.options.contains(newOption)) {
                        Text(
                            "This option already exists",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newOption.isNotBlank() && !variable.options.contains(newOption)) {
                            val newOptions = variable.options + newOption
                            onVariableChange(variable.copy(options = newOptions))
                            newOption = ""
                            showOptionsDialog = false
                        }
                    },
                    enabled = newOption.isNotBlank() && !variable.options.contains(newOption)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        newOption = ""
                        showOptionsDialog = false 
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}
