package com.ainotebuddy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R

/**
 * A section header with an optional icon and action
 * 
 * @param title The title of the section
 * @param icon Optional icon to display before the title
 * @param action Optional composable to display at the end of the header
 * @param modifier Modifier for the section header
 */
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon (if provided)
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
            }
            
            // Title
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            // Action (if provided)
            action?.invoke()
        }
    }
}

/**
 * A section header with an info icon that shows a tooltip on click
 * 
 * @param title The title of the section
 * @param infoText The text to show in the tooltip
 * @param modifier Modifier for the section header
 */
@Composable
fun InfoSectionHeader(
    title: String,
    infoText: String,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }
    
    SectionHeader(
        title = title,
        icon = Icons.Default.Info,
        action = {
            if (showTooltip) {
                AlertDialog(
                    onDismissRequest = { showTooltip = false },
                    title = { Text(title) },
                    text = { Text(infoText) },
                    confirmButton = {
                        TextButton(
                            onClick = { showTooltip = false }
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                )
            }
        },
        modifier = modifier.clickable { showTooltip = true }
    )
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    MaterialTheme {
        Column {
            SectionHeader(
                title = "General Settings"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SectionHeader(
                title = "Account",
                icon = Icons.Default.Person
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoSectionHeader(
                title = "Advanced Settings",
                infoText = "These settings are for advanced users. Be careful when changing them."
            )
        }
    }
}
