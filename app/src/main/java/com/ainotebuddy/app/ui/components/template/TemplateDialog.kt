package com.ainotebuddy.app.ui.components.template

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.ui.theme.LocalSpacing

/**
 * Dialog for creating or editing a template
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDialog(
    template: NoteTemplate? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, category: String, icon: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    // State for form fields
    var name by remember { mutableStateOf(template?.name ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var category by remember { mutableStateOf(template?.category ?: "") }
    var icon by remember { mutableStateOf(template?.icon ?: "") }
    
    // Form validation
    val isFormValid = name.isNotBlank()
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = if (template == null) {
                        stringResource(R.string.create_template)
                    } else {
                        stringResource(R.string.edit_template)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = spacing.medium)
                )
                
                // Template name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.template_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.medium)
                )
                
                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.medium)
                )
                
                // Icon (emoji or short code)
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Icon (emoji or short code)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.medium)
                )
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_optional)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.medium)
                )
                
                // Additional fields (optional) can go here
                
                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(spacing.small))
                    
                    Button(
                        onClick = { 
                            onConfirm(
                                name.trim(),
                                description.trim(),
                                category.trim(),
                                icon.trim()
                            )
                        },
                        enabled = isFormValid
                    ) {
                        Text(
                            if (template == null) {
                                stringResource(R.string.create)
                            } else {
                                stringResource(R.string.save)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateDialogPreview() {
    val template = NoteTemplate(
        id = "template_meeting_notes",
        name = "Meeting Notes",
        description = "Template for capturing meeting notes",
        icon = "📝",
        category = "Work",
        content = "",
        variables = emptyList()
    )
    
    MaterialTheme {
        TemplateDialog(
            template = template,
            onDismiss = { },
            onConfirm = { _, _, _, _ -> }
        )
    }
}
