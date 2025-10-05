package com.ainotebuddy.app.ui.components.ai
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AITagGenerator(
    currentTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    onGenerateTags: suspend (currentTags: List<String>) -> List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxTags: Int = 10,
    label: String = "Tags"
) {
    val spacing = LocalSpacing.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var isGenerating by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }
    var generatedTags by remember { mutableStateOf<List<String>>(emptyList()) }

    val toggleTagInput: () -> Unit = {
        if (showTagInput) {
            keyboardController?.hide()
            newTag = ""
            showTagInput = false
        } else {
            showTagInput = true
            scope.launch {
                delay(50)
                focusRequester.requestFocus()
            }
        }
        Unit
    }

    val generateAITags: () -> Unit = {
        if (!isGenerating) {
            scope.launch {
                isGenerating = true
                try {
                    val tags = onGenerateTags(currentTags)
                    generatedTags = tags.take(maxTags)
                } finally {
                    isGenerating = false
                }
            }
        }
        Unit
    }

    val addTag = { tag: String ->
        if (tag.isNotBlank() && !currentTags.contains(tag)) {
            onTagsChanged(currentTags + tag)
            generatedTags = generatedTags - tag
        }
    }

    val removeTag = { tag: String ->
        onTagsChanged(currentTags - tag)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(spacing.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (enabled) {
                IconButton(
                    onClick = generateAITags,
                    enabled = !isGenerating,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Generate tags with AI",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (currentTags.isNotEmpty()) {
            TagList(
                tags = currentTags,
                onRemoveClick = { if (enabled) removeTag(it) },
                showRemoveButton = enabled,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        if (generatedTags.isNotEmpty()) {
            Text(
                text = "Suggested tags:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = spacing.medium, bottom = spacing.small)
            )
            TagList(
                tags = generatedTags,
                onTagClick = { if (enabled) addTag(it) },
                modifier = Modifier.padding(bottom = if (showTagInput) 0.dp else spacing.small)
            )
        }

        if (showTagInput) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.small)
            ) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { if (it.length <= 20) newTag = it },
                    placeholder = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                showTagInput = false
                                newTag = ""
                            }
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTag.isNotBlank()) {
                                addTag(newTag)
                                newTag = ""
                            } else {
                                toggleTagInput()
                            }
                        }
                    )
                )
                IconButton(
                    onClick = {
                        if (newTag.isNotBlank()) {
                            addTag(newTag)
                            newTag = ""
                        } else {
                            toggleTagInput()
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Add tag")
                }
            }
        } else if (enabled) {
            TextButton(
                onClick = toggleTagInput,
                modifier = Modifier.padding(top = if (currentTags.isEmpty()) 0.dp else spacing.small)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add tag")
            }
        }
    }
}

@Composable
private fun TagList(
    tags: List<String>,
    modifier: Modifier = Modifier,
    onTagClick: ((String) -> Unit)? = null,
    onRemoveClick: ((String) -> Unit)? = null,
    showRemoveButton: Boolean = false
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = modifier
    ) {
        items(tags) { tag ->
            TagChip(
                tag = tag,
                onClick = onTagClick?.let { { onTagClick(tag) } },
                onRemoveClick = onRemoveClick?.let { { onRemoveClick(tag) } },
                showRemoveButton = showRemoveButton
            )
        }
    }
}

@Composable
private fun TagChip(
    tag: String,
    onClick: (() -> Unit)? = null,
    onRemoveClick: (() -> Unit)? = null,
    showRemoveButton: Boolean = false
) {
    val isClickable = onClick != null
    val isRemovable = onRemoveClick != null && showRemoveButton

    Surface(
        onClick = { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        enabled = isClickable,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 6.dp,
                end = if (isRemovable) 4.dp else 12.dp,
                bottom = 6.dp
            )
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isRemovable) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onRemoveClick?.invoke() }, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove tag",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.PIXEL_4)
@Composable
private fun AITagGeneratorPreview() {
    val tags = remember { mutableStateListOf("work", "important") }
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AITagGenerator(
                currentTags = tags,
                onTagsChanged = { tags.clear(); tags.addAll(it) },
                onGenerateTags = { currentTags ->
                    // Simulate AI tag generation
                    listOf("meeting", "project", "urgent").filter { !currentTags.contains(it) }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
