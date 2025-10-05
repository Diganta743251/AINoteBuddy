package com.ainotebuddy.app.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onImageSearch: () -> Unit,
    onDocumentSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search notes, images, and documents",
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Search field
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = { Text(placeholder) },
                singleLine = true,
                colors = TextFieldDefaults.colors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(query)
                        focusManager.clearFocus()
                    }
                ),
                enabled = enabled
            )
            
            // Visual search dropdown
            var expanded by remember { mutableStateOf(false) }
            
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ImageSearch,
                        contentDescription = "Visual search options",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(200.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Search in image") },
                        onClick = {
                            onImageSearch()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Search in image"
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Search in document") },
                        onClick = {
                            onDocumentSearch()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Search in document"
                            )
                        }
                    )
                }
            }
        }
    }
}
