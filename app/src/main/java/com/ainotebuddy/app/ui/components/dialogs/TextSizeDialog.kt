package com.ainotebuddy.app.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ainotebuddy.app.R
import com.ainotebuddy.app.utils.TextScaler
import com.ainotebuddy.app.utils.rememberTextScaler

/**
 * Dialog for adjusting text size across the app
 */
@Composable
fun TextSizeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    textScaler: TextScaler = rememberTextScaler()
) {
    val currentScale by remember { derivedStateOf { textScaler.getFontScale() } }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.text_size),
                style = MaterialTheme.typography.headlineSmall
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sample text that shows the current size
                Text(
                    text = stringResource(R.string.text_size_sample),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * currentScale,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * currentScale
                    ),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                // Slider for adjusting text size
                Slider(
                    value = currentScale,
                    onValueChange = { textScaler.setFontScale(it) },
                    valueRange = 0.8f..2.0f,
                    steps = 11, // 0.8, 0.9, 1.0, ..., 1.9, 2.0
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                // Scale indicator (percentage)
                Text(
                    text = "${(currentScale * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * 1.2f
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Quick action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { 
                            textScaler.decreaseFontScale() 
                        },
                        enabled = currentScale > 0.8f
                    ) {
                        Text(stringResource(R.string.decrease))
                    }
                    
                    TextButton(
                        onClick = { 
                            textScaler.resetFontScale() 
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                    
                    TextButton(
                        onClick = { 
                            textScaler.increaseFontScale() 
                        },
                        enabled = currentScale < 2.0f
                    ) {
                        Text(stringResource(R.string.increase))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TextSizeDialogPreview() {
    MaterialTheme {
        Surface {
            var showDialog by remember { mutableStateOf(true) }
            
            if (showDialog) {
                TextSizeDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { showDialog = false }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Dialog will be shown in preview")
                }
            }
        }
    }
}
