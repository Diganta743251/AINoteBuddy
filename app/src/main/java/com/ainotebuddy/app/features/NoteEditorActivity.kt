package com.ainotebuddy.app.features

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.CalendarContract
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Environment
import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.foundation.layout.Arrangement
import com.ainotebuddy.app.ai.VoiceRecordingService
import com.ainotebuddy.app.viewmodel.NoteEditorViewModel
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.workers.ReminderWorker
import com.ainotebuddy.app.ads.AdManager

// QuickActionService stub
class QuickActionService {
    fun scheduleNoteReminder(noteId: Long, reminderTime: Long, context: Context) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(reminderTime - System.currentTimeMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(androidx.work.workDataOf("noteId" to noteId))
            .build()
        
        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
    }
}

@AndroidEntryPoint
class NoteEditorActivity : ComponentActivity() {
    
    private val viewModel: NoteEditorViewModel by viewModels()
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        }
    }
    
    private var currentPhotoUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                viewModel.addImage(uri)
            }
        }
    }
    
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.addImage(it)
        }
    }
    
    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(
                android.speech.RecognizerIntent.EXTRA_RESULTS
            )?.firstOrNull()
            spokenText?.let { text ->
                val current = viewModel.uiState.value.content
                viewModel.updateContent((current + "\n" + text).trim())
            }
        }
    }
    
    private val importNoteLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                val content = reader.readText()
                // Import into current editor state
                viewModel.updateTitle("Imported Note")
                viewModel.updateContent(content)
                viewModel.saveNote()
            }
        }
    }
    
    private val exportNoteLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                val state = viewModel.uiState.value
                val content = "Title: ${state.title}\n\n${state.content}"
                outputStream.write(content.toByteArray())
            }
        }
    }
    private val pdfExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { exportToPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val noteId = intent.getLongExtra("note_id", -1L)
        val voiceText = intent.getStringExtra("voice_text")
        
        if (noteId != -1L) {
            viewModel.loadNote(noteId)
        }
        
        // If we have voice text, set it as initial content
        if (!voiceText.isNullOrEmpty()) {
            viewModel.updateContent(voiceText)
        }

        setContent {
            AINoteBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Local UI state for dialogs
                    var showAdvancedOptions by remember { mutableStateOf(false) }
                    var showFormatDialog by remember { mutableStateOf(false) }
                    var showAiSheet by remember { mutableStateOf(false) }

                    NoteEditorScreenEnhanced(
                        viewModel = viewModel,
                        onBackPressed = {
                            viewModel.saveNote()
                            finish()
                        },
                        onSave = {
                            viewModel.saveNote()
                            showAdAfterSave()
                        },
                        onCameraClick = { requestCameraPermission() },
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onVoiceClick = { startVoiceRecognition() },
                        onFormatClick = { showFormatDialog = true },
                        onMoreClick = { showAdvancedOptions = true },
                        onAiClick = { showAiSheet = true }
                    )

                    if (showAdvancedOptions) {
                        AdvancedOptionsDialog(
                            onDismiss = { showAdvancedOptions = false },
                            onImport = { importNoteLauncher.launch(arrayOf("text/plain", "text/markdown")) },
                            onExport = { exportNoteLauncher.launch("note.txt") },
                            onPdfExport = { pdfExportLauncher.launch("note.pdf") },
                            onReminder = { showReminderDialog() }
                        )
                    }

                    if (showFormatDialog) {
                        AlertDialog(
                            onDismissRequest = { showFormatDialog = false },
                            title = { Text("Formatting") },
                            text = { Text("Formatting options will be available soon.") },
                            confirmButton = {
                                TextButton(onClick = { showFormatDialog = false }) { Text("OK") }
                            }
                        )
                    }
                    
                    if (showAiSheet) {
                        com.ainotebuddy.app.ui.components.ai.AIAssistantPanel(
                            note = com.ainotebuddy.app.data.model.Note(
                                id = viewModel.uiState.value.noteId ?: 0,
                                title = viewModel.uiState.value.title,
                                content = viewModel.uiState.value.content,
                                tags = ""
                            ),
                            onClose = { showAiSheet = false },
                            onSuggestionClick = { },
                            onVoiceCommand = { },
                            onGenerateTags = { emptyList() },
                            onTagsChanged = { },
                            sentimentResult = null,
                            suggestions = emptyList(),
                            voiceCommands = emptyList(),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }
    
    private fun showAdAfterSave() {
        try {
            val adManager = AdManager.getInstance(this)
            val interstitialManager = adManager.retrieveInterstitialAdManager()
            if (adManager.canShowInterstitial() && interstitialManager?.isAdReady() == true) {
                interstitialManager.showAd(this) {
                    // Ad closed, continue
                }
            }
        } catch (e: Exception) {
            // Ad failed to load/show, continue without showing ad
            e.printStackTrace()
        }
    }
    
    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun openCamera() {
        val photoFile = java.io.File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "photo_${System.currentTimeMillis()}.jpg"
        )
        val photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        currentPhotoUri = photoUri
        cameraLauncher.launch(photoUri)
    }
    
    private fun startVoiceRecognition() {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your note...")
        }
        voiceRecognitionLauncher.launch(intent)
    }
    
    private fun exportToPdf(uri: Uri) {
        val state = viewModel.uiState.value
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
        }
        
        canvas.drawText("Title: ${state.title}", 50f, 50f, paint)
        canvas.drawText(state.content, 50f, 100f, paint)
        
        document.finishPage(page)
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()
    }
    
    private fun showReminderDialog() {
        // TODO: Implement reminders via settings and scheduler
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreenEnhanced(
    viewModel: NoteEditorViewModel,
    onBackPressed: () -> Unit,
    onSave: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onFormatClick: () -> Unit,
    onMoreClick: () -> Unit,
    onAiClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header
        ModernEditorHeader(
            onBackPressed = onBackPressed,
            onSave = onSave
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.updateTitle(it) },
                placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rich Text Editor with Image Support
            com.ainotebuddy.app.ui.components.RichTextEditor(
                content = state.content,
                onContentChange = { viewModel.updateContent(it) },
                images = state.images,
                onImageClick = { imageUri -> viewModel.removeImage(imageUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bottom Action Bar with AI
            ModernEditorBottomBarWithAI(
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
                onVoiceClick = onVoiceClick,
                onFormatClick = onFormatClick,
                onMoreClick = onMoreClick,
                onAiClick = onAiClick
            )
        }
    }
}

@Composable
fun ModernEditorHeader(
    onBackPressed: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Edit Note",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ModernEditorBottomBarWithAI(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onFormatClick: () -> Unit,
    onMoreClick: () -> Unit,
    onAiClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EditorActionButton(
                icon = Icons.Filled.Edit,
                label = "Edit",
                onClick = onFormatClick
            )
            EditorActionButton(
                icon = Icons.Filled.CameraAlt,
                label = "Camera",
                onClick = onCameraClick
            )
            EditorActionButton(
                icon = Icons.Filled.Image,
                label = "Gallery",
                onClick = onGalleryClick
            )
            EditorActionButton(
                icon = Icons.Filled.Mic,
                label = "Voice",
                onClick = onVoiceClick
            )
            EditorActionButton(
                icon = Icons.Filled.SmartToy,
                label = "AI",
                onClick = onAiClick
            )
            EditorActionButton(
                icon = Icons.Filled.MoreHoriz,
                label = "More",
                onClick = onMoreClick
            )
        }
    }
}

@Composable
fun EditorActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    CircleShape
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBackPressed: () -> Unit,
    onSave: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onPdfExportClick: () -> Unit,
    onReminderClick: () -> Unit,
    onDrawingModeToggle: () -> Unit,
    onSmartAssistClick: () -> Unit
) {
    val note by viewModel.note.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDrawingMode by viewModel.isDrawingMode.collectAsState()
    val checklistItems by viewModel.checklistItems.collectAsState()
    val images by viewModel.images.collectAsState()
    val pdfUris by viewModel.pdfUris.collectAsState()
    
    // Smart Assist states
    val isSmartAssistVisible by viewModel.isSmartAssistVisible.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAIProcessing by viewModel.isAIProcessing.collectAsState()
    
    var showFormattingOptions by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val aiError by viewModel.aiError.collectAsState()
    LaunchedEffect(aiError) {
        aiError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAiError()
        }
    }

    // Snackbar hookup for reminder confirmations
    val snackbarMessage by viewModel.uiState.collectAsState()
    LaunchedEffect(snackbarMessage.snackbarMessage) {
        snackbarMessage.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NoteEditorBottomBar(
                onFormattingClick = { showFormattingOptions = !showFormattingOptions },
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
                onVoiceClick = onVoiceClick,
                onDrawingClick = onDrawingModeToggle,
                isDrawingMode = isDrawingMode,
                onAdvancedClick = { showAdvancedOptions = !showAdvancedOptions }
            )
        },
        floatingActionButton = {
            var showAiSheet by remember { mutableStateOf(false) }
            FloatingActionButton(
                onClick = { showAiSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI Tools",
                    modifier = Modifier.size(28.dp)
                )
            }
            if (showAiSheet) {
                com.ainotebuddy.app.features.composer.NoteEditorAiSheet(
                    noteId = note.id,
                    show = showAiSheet,
                    onDismiss = { showAiSheet = false },
                    viewModel = viewModel,
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Title field
            OutlinedTextField(
                value = note.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textStyle = MaterialTheme.typography.headlineSmall
            )
            
            // Rich text editor with images
            RichTextEditor(
                content = note.content,
                onContentChange = { viewModel.updateContent(it) },
                images = images,
                onImageClick = { imageUri -> viewModel.removeImage(imageUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            
            // Checklist section
            if (checklistItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    checklistItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { viewModel.toggleChecklistItemChecked(item) }
                            )
                            Text(
                                text = item.text,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        
        // Formatting options dialog
        if (showFormattingOptions) {
            FormattingOptionsDialog(
                onDismiss = { showFormattingOptions = false },
                onBold = { 
                    viewModel.toggleBold()
                    showFormattingOptions = false
                },
                onItalic = { 
                    viewModel.toggleItalic()
                    showFormattingOptions = false
                },
                onUnderline = { 
                    viewModel.toggleUnderline()
                    showFormattingOptions = false
                },
                onBulletList = { 
                    viewModel.addBulletPoint()
                    showFormattingOptions = false
                },
                onCheckbox = { 
                    viewModel.addCheckbox()
                    showFormattingOptions = false
                },
                onCode = { 
                    viewModel.toggleCode()
                    showFormattingOptions = false
                }
            )
        }
        
        // Advanced options dialog
        if (showAdvancedOptions) {
            AdvancedOptionsDialog(
                onDismiss = { showAdvancedOptions = false },
                onImport = onImportClick,
                onExport = onExportClick,
                onPdfExport = onPdfExportClick,
                onReminder = onReminderClick
            )
        }
        
        // Smart Assist Dialog
        // AI Tools – bottom sheet
        var showAiSheet by remember { mutableStateOf(false) }
        if (showAiSheet) {
            com.ainotebuddy.app.features.composer.NoteEditorAiSheet(
                noteId = note.id,
                show = showAiSheet,
                onDismiss = { showAiSheet = false },
                summarizeNote = { id ->
                    viewModel.requestSummary(id)
                    viewModel.aiSummary.value ?: ""
                },
                autoTagNote = { id ->
                    viewModel.requestAutoTags(id)
                    viewModel.aiTags.value
                },
                onApplySummary = {
                    viewModel.applySummaryToContent()
                    showAiSheet = false
                },
                onApplyTags = {
                    viewModel.applyTagsToNote()
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBackPressed: () -> Unit,
    onSave: () -> Unit
    onGalleryClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onDrawingClick: () -> Unit,
    isDrawingMode: Boolean,
    onAdvancedClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onFormattingClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Formatting")
            }
            IconButton(onClick = onCameraClick) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Camera")
            }
            IconButton(onClick = onGalleryClick) {
                Icon(Icons.Filled.Image, contentDescription = "Gallery")
            }
            IconButton(onClick = onVoiceClick) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice")
            }
            IconButton(onClick = onDrawingClick) {
                Icon(
                    Icons.Filled.Brush,
                    contentDescription = "Drawing",
                    tint = if (isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onAdvancedClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@Composable
fun FormattingOptionsDialog(
    onDismiss: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onBulletList: () -> Unit,
    onCheckbox: () -> Unit,
    onCode: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Formatting Options") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onBold) {
                        Icon(Icons.Filled.FormatBold, contentDescription = "Bold")
                    }
                    IconButton(onClick = onItalic) {
                        Icon(Icons.Filled.FormatItalic, contentDescription = "Italic")
                    }
                    IconButton(onClick = onUnderline) {
                        Icon(Icons.Filled.FormatUnderlined, contentDescription = "Underline")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onBulletList) {
                        Icon(Icons.Filled.FormatListBulleted, contentDescription = "Bullet List")
                    }
                    IconButton(onClick = onCheckbox) {
                        Icon(Icons.Filled.CheckBox, contentDescription = "Checkbox")
                    }
                    IconButton(onClick = onCode) {
                        Icon(Icons.Filled.Code, contentDescription = "Code")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
*/

@Composable
fun AdvancedOptionsDialog(
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onPdfExport: () -> Unit,
    onReminder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Options") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Note")
                }
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export as Text")
                }
                Button(
                    onClick = onPdfExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export as PDF")
                }
                Button(
                    onClick = onReminder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Reminder")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
} 