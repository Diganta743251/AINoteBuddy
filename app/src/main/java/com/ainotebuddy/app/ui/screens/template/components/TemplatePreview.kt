package com.ainotebuddy.app.ui.screens.template.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.NoteTemplate.VariableType
import com.ainotebuddy.app.ui.theme.LocalSpacing

import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TemplatePreview(
    template: NoteTemplate,
    onTemplateChange: (NoteTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    
    // Track focus for the content field only
    val contentFocusRequester = remember { FocusRequester() }
    
    // Title editing state removed; NoteTemplate has no title field
    val isEditingTitle = false
    
    // Track current date for preview
    val currentDate = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()) }
    val currentTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }
    
    // Generate preview values for variables
    val previewValues = remember(template.variables) {
        template.variables.associate { variable ->
            variable.name to when (variable.type) {
                NoteTemplate.VariableType.TEXT -> 
                    variable.defaultValue.ifEmpty { "[${variable.name}]" }
                NoteTemplate.VariableType.NUMBER -> 
                    variable.defaultValue.ifEmpty { "123" }
                NoteTemplate.VariableType.DATE -> 
                    currentDate
                NoteTemplate.VariableType.TIME -> 
                    currentTime
                NoteTemplate.VariableType.CHOICE -> 
                    variable.options.firstOrNull() ?: "[Select ${variable.name}]"
            }
        }
    }
    
    // Replace variables in the content with their preview values
    val previewContent = remember(template.content, previewValues) {
        var result = template.content
        previewValues.forEach { (name, value) ->
            result = result.replace("{{$name}}", value)
        }
        result
    }
    
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.medium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Preview header
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.medium)
            )
            
            // Title section (NoteTemplate.name)
            Text(
                text = template.name.ifEmpty { "Template" },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            
            // Divider
            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = spacing.medium)
            )
            
            // Content section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                if (previewContent.isNotEmpty()) {
                    // Display the preview content with syntax highlighting for variables
                    val parts = remember(previewContent, template.variables) {
                        val variableNames = template.variables.map { it.name }
                        val regex = Regex("\\{\\{(.*?)\\}}")
                        val parts = mutableListOf<@Composable () -> Unit>()
                        
                        var lastIndex = 0
                        regex.findAll(previewContent).forEach { matchResult ->
                            // Add text before the match
                            if (matchResult.range.first > lastIndex) {
                                val text = previewContent.substring(lastIndex, matchResult.range.first)
                                parts.add({ 
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ) 
                                })
                            }
                            
                            // Add the variable
                            val varName = matchResult.groupValues[1].trim()
                            val variable = template.variables.find { it.name == varName }
                            val previewValue = previewValues[varName] ?: ""
                            
                            parts.add({
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    // BorderStroke not needed; simplify to avoid extra import
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = previewValue,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            })
                            
                            lastIndex = matchResult.range.last + 1
                        }
                        
                        // Add remaining text
                        if (lastIndex < previewContent.length) {
                            val text = previewContent.substring(lastIndex)
                            parts.add({ 
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            })
                        }
                        
                        parts
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        parts.forEach { part ->
                            part()
                        }
                    }
                } else {
                    // Empty state
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Preview will appear here as you edit the template",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Preview footer
            if (template.variables.isNotEmpty()) {
                Text(
                    text = "Variables used: ${template.variables.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(top = spacing.medium)
                        .align(Alignment.End)
                )
            }
        }
    }
}
