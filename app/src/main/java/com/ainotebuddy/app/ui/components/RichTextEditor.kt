package com.ainotebuddy.app.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun RichTextEditor(
    content: String,
    onContentChange: (String) -> Unit,
    images: List<Uri>,
    onImageClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var showImageDialog by remember { mutableStateOf<Uri?>(null) }
    var textFieldValue by remember { 
        mutableStateOf(TextFieldValue(content))
    }
    
    // Sync textFieldValue with content changes from outside
    LaunchedEffect(content) {
        if (textFieldValue.text != content) {
            textFieldValue = textFieldValue.copy(text = content)
        }
    }
    
    Column(modifier = modifier) {
        // Text field first - main content area with proper cursor handling
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onContentChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            ),
            placeholder = {
                Text(
                    "Start writing your note...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = Int.MAX_VALUE,
            singleLine = false
        )
        
        // Images section at bottom - scrollable but limited height
        if (images.isNotEmpty()) {
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(images) { imageUri ->
                    ImagePreview(
                        imageUri = imageUri,
                        onClick = { onImageClick(imageUri) },
                        onFullScreenClick = { showImageDialog = imageUri }
                    )
                }
            }
        }
    }
    
    // Full screen image dialog
    showImageDialog?.let { uri ->
        ImageFullScreenDialog(
            imageUri = uri,
            onDismiss = { showImageDialog = null }
        )
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val content = text
        
        // Process markdown formatting
        while (currentIndex < content.length) {
            when {
                // Bold text **text**
                content.startsWith("**", currentIndex) -> {
                    val endIndex = content.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        val boldText = content.substring(currentIndex + 2, endIndex)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldText)
                        }
                        currentIndex = endIndex + 2
                    } else {
                        append(content[currentIndex])
                        currentIndex++
                    }
                }
                
                // Italic text *text*
                content.startsWith("*", currentIndex) && !content.startsWith("**", currentIndex) -> {
                    val endIndex = content.indexOf("*", currentIndex + 1)
                    if (endIndex != -1) {
                        val italicText = content.substring(currentIndex + 1, endIndex)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicText)
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(content[currentIndex])
                        currentIndex++
                    }
                }
                
                // Underline text <u>text</u>
                content.startsWith("<u>", currentIndex) -> {
                    val endIndex = content.indexOf("</u>", currentIndex + 3)
                    if (endIndex != -1) {
                        val underlineText = content.substring(currentIndex + 3, endIndex)
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(underlineText)
                        }
                        currentIndex = endIndex + 4
                    } else {
                        append(content[currentIndex])
                        currentIndex++
                    }
                }
                
                // Code text `code`
                content.startsWith("`", currentIndex) -> {
                    val endIndex = content.indexOf("`", currentIndex + 1)
                    if (endIndex != -1) {
                        val codeText = content.substring(currentIndex + 1, endIndex)
                        withStyle(
                            SpanStyle(
                                background = Color.Gray.copy(alpha = 0.2f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        ) {
                            append(codeText)
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(content[currentIndex])
                        currentIndex++
                    }
                }
                
                // Heading # text
                content.startsWith("# ", currentIndex) -> {
                    val lineEnd = content.indexOf('\n', currentIndex).let { 
                        if (it == -1) content.length else it 
                    }
                    val headingText = content.substring(currentIndex + 2, lineEnd)
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    ) {
                        append(headingText)
                    }
                    currentIndex = lineEnd
                }
                
                // Bullet point - text
                content.startsWith("- ", currentIndex) -> {
                    append("• ")
                    currentIndex += 2
                }
                
                // Checkbox [ ] or [x]
                content.startsWith("[ ]", currentIndex) -> {
                    append("☐ ")
                    currentIndex += 3
                }
                content.startsWith("[x]", currentIndex) -> {
                    append("☑ ")
                    currentIndex += 3
                }
                
                else -> {
                    append(content[currentIndex])
                    currentIndex++
                }
            }
        }
    }
    
    SelectionContainer {
        Text(
            text = annotatedString,
            modifier = modifier,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun ImagePreview(
    imageUri: Uri,
    onClick: (Uri) -> Unit,
    onFullScreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onFullScreenClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Note image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Delete button
            IconButton(
                onClick = { onClick(imageUri) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove image",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ImageFullScreenDialog(
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Full screen image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}