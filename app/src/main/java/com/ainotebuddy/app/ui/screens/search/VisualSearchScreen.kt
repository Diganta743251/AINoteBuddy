package com.ainotebuddy.app.ui.screens.search

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.ainotebuddy.app.R
import com.ainotebuddy.app.ui.components.*
import com.ainotebuddy.app.ui.theme.LocalSpacing
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSearchScreen(
    onBackClick: () -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var showProgress by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Check and request camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            // Note: You'll need to implement the camera launcher
        } else {
            errorMessage = "Camera permission is required for text recognition"
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    // Process image when selected
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            try {
                showProgress = true
                errorMessage = null
                
                val image = InputImage.fromFilePath(context, uri)
                val result = textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        extractedText = visionText.text
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error processing image: ${e.message}"
                        extractedText = ""
                    }
                    .addOnCompleteListener {
                        showProgress = false
                    }
            } catch (e: IOException) {
                errorMessage = "Error loading image: ${e.message}"
                showProgress = false
            }
        }
    }

    // Check for camera permission
    fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Launch camera
                // Note: Implement camera launcher
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visual Search") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (extractedText.isNotBlank()) {
                        IconButton(onClick = { onSearch(extractedText) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gallery")
                }
                
                Button(
                    onClick = { checkCameraPermission() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera")
                }
            }

            // Extracted Text Preview
            if (showProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (extractedText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.medium)
                ) {
                    Text(
                        text = extractedText,
                        modifier = Modifier.padding(spacing.medium)
                    )
                }
            } else {
                // Empty state
                EmptyState(
                    icon = Icons.Default.ImageSearch,
                    title = "No Image Selected",
                    description = "Select an image from your gallery or take a photo to extract text"
                )
            }

            // Error message
            errorMessage?.let { message ->
                // Inline simple error alert to avoid missing composable
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { androidx.compose.material3.Text("Error") },
                    text = { androidx.compose.material3.Text(message) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { errorMessage = null }) {
                            androidx.compose.material3.Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
