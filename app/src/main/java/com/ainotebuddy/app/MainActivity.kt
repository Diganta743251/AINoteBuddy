package com.ainotebuddy.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotebuddy.app.auth.LoginActivity
import com.ainotebuddy.app.auth.GoogleAuthService
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.ainotebuddy.app.ui.theme.ThemeManager
import com.ainotebuddy.app.ui.theme.NoteScreen
import com.ainotebuddy.app.ui.dashboard.DashboardScreen
import com.ainotebuddy.app.features.NoteEditorActivity
import com.ainotebuddy.app.settings.SettingsScreen
import com.ainotebuddy.app.viewmodel.NoteViewModel
import com.ainotebuddy.app.viewmodel.NoteViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import androidx.compose.runtime.*
import com.ainotebuddy.app.onboarding.OnboardingScreen
import com.ainotebuddy.app.settings.PrivacyPolicyScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.ainotebuddy.app.data.NoteEntity
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ainotebuddy.app.ui.calendar.CalendarNotesScreen
import com.ainotebuddy.app.ui.canvas.InfiniteCanvasScreen
import com.ainotebuddy.app.features.WebClipperScreen
import com.ainotebuddy.app.features.ClipboardManagerScreen
import com.ainotebuddy.app.features.SmartTemplatesScreen
import com.ainotebuddy.app.features.VersionHistoryScreen

enum class Screen {
    DASHBOARD,
    NOTES,
    SETTINGS,
    CALENDAR,
    CANVAS,
    WEB_CLIPPER,
    CLIPBOARD,
    TEMPLATES,
    VERSION_HISTORY
}

// Placeholder screens for functionality
@Composable
fun SearchScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = if (searchQuery.isBlank()) {
        notes
    } else {
        notes.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.content.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        // Search results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(filteredNotes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNoteClick(note) }
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplatesScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Note Templates",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        // Template list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "Coming Soon!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Template functionality will be implemented soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun CategoriesScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val categories = notes.map { it.category }.distinct()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Categories",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        // Categories list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(categories) { category ->
                val noteCount = notes.count { it.category == category }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(category) }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$noteCount notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<NoteViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                    return NoteViewModel(
                        repository = null, // TODO: Implement when Room is available
                        advancedRepository = (application as AINoteBuddyApplication).advancedNoteRepository,
                        googleAuthService = GoogleAuthService(this@MainActivity)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("ainotebuddy_prefs", MODE_PRIVATE)
        val firstRun = prefs.getBoolean("first_run", true)
        // var isSignedIn = (application as AINoteBuddyApplication).googleAuthService.isSignedIn()
        var isSignedIn = false // TODO: Implement when auth is available

        setContent {
            var showOnboarding by remember { mutableStateOf(firstRun) }
            var showPrivacy by remember { mutableStateOf(false) }
            var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
            var signedIn by remember { mutableStateOf(isSignedIn) }

            if (showOnboarding) {
                if (showPrivacy) {
                    PrivacyPolicyScreen(onBack = { showPrivacy = false })
                } else {
                    OnboardingScreen(
                        onFinish = {
                            prefs.edit().putBoolean("first_run", false).apply()
                            showOnboarding = false
                        },
                        onPrivacy = { showPrivacy = true }
                    )
                }
            } else {
                if (!signedIn) {
                    // Show sign-in prompt or screen, but allow skip
                    SignInPrompt(
                        onSignIn = {
                            // TODO: Implement real sign-in logic
                            signedIn = true
                        },
                        onSkip = {
                            signedIn = false
                        }
                    )
                } else {
                    AINoteBuddyTheme(themeState = ThemeManager.themeState) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            MainAppContent(
                                currentScreen = currentScreen,
                                onScreenChange = { currentScreen = it },
                                viewModel = viewModel,
                                onSignOut = {
                                    signOut()
                                    signedIn = false
                                },
                                onSyncToDrive = { syncToDrive() },
                                onSyncFromDrive = { syncFromDrive() },
                                onNoteClick = { note -> openNoteEditor(note) },
                                onNewNote = { openNoteEditor(null) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun openNoteEditor(note: NoteEntity?) {
        val intent = Intent(this, NoteEditorActivity::class.java).apply {
            note?.let { putExtra("note_id", it.id) }
        }
        startActivity(intent)
    }
    
    private fun signOut() {
        // val googleAuthService = (application as AINoteBuddyApplication).googleAuthService
        lifecycleScope.launch {
            // googleAuthService.signOut()
            // startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            // finish()
        }
    }
    
    private fun syncToDrive() {
        // val googleAuthService = (application as AINoteBuddyApplication).googleAuthService
        val syncService = (application as AINoteBuddyApplication).googleDriveSyncService
        
        // val account = googleAuthService.getCurrentAccount()
        // if (account != null) {
        //     Toast.makeText(this, "Syncing to Google Drive...", Toast.LENGTH_SHORT).show()
        //     viewModel.syncToDrive(account, syncService)
        // } else {
        //     Toast.makeText(this, "Please sign in to sync", Toast.LENGTH_SHORT).show()
        // }
        Toast.makeText(this, "Sync not available yet", Toast.LENGTH_SHORT).show()
    }
    
    private fun syncFromDrive() {
        // val googleAuthService = (application as AINoteBuddyApplication).googleAuthService
        val syncService = (application as AINoteBuddyApplication).googleDriveSyncService
        
        // val account = googleAuthService.getCurrentAccount()
        // if (account != null) {
        //     Toast.makeText(this, "Syncing from Google Drive...", Toast.LENGTH_SHORT).show()
        //     viewModel.syncFromDrive(account, syncService)
        // } else {
        //     Toast.makeText(this, "Please sign in to sync", Toast.LENGTH_SHORT).show()
        // }
        Toast.makeText(this, "Sync not available yet", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainAppContent(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    viewModel: NoteViewModel,
    onSignOut: () -> Unit,
    onSyncToDrive: () -> Unit,
    onSyncFromDrive: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onNewNote: () -> Unit
) {
    val context = LocalContext.current
    
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (currentScreen) {
                            Screen.DASHBOARD -> "AINoteBuddy Dashboard"
                            Screen.NOTES -> "My Notes"
                            Screen.SETTINGS -> "Settings"
                        }
                    )
                },
                actions = {
                    // Sync buttons
                    IconButton(onClick = onSyncToDrive) {
                        Icon(Icons.Filled.CloudUpload, "Sync to Drive")
                    }
                    IconButton(onClick = onSyncFromDrive) {
                        Icon(Icons.Filled.CloudDownload, "Sync from Drive")
                    }
                }
            )
        },
        bottomBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Dashboard, "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { onScreenChange(Screen.DASHBOARD) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Note, "Notes") },
                    label = { Text("Notes") },
                    selected = currentScreen == Screen.NOTES,
                    onClick = { onScreenChange(Screen.NOTES) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { onScreenChange(Screen.SETTINGS) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add new note")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    var showSearch by remember { mutableStateOf(false) }
                    var showTemplates by remember { mutableStateOf(false) }
                    var showCategories by remember { mutableStateOf(false) }
                    
                    if (showSearch) {
                        SearchScreen(
                            viewModel = viewModel,
                            onBack = { showSearch = false },
                            onNoteClick = onNoteClick
                        )
                    } else if (showTemplates) {
                        TemplatesScreen(
                            viewModel = viewModel,
                            onBack = { showTemplates = false },
                            onNoteClick = onNoteClick
                        )
                    } else if (showCategories) {
                        CategoriesScreen(
                            viewModel = viewModel,
                            onBack = { showCategories = false },
                            onCategoryClick = { category ->
                                // Filter notes by category
                                showCategories = false
                            }
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNoteClick = onNoteClick,
                            onNewNote = onNewNote,
                            onSearch = { showSearch = true },
                            onCategoryClick = { category -> 
                                // Filter notes by category
                            },
                            onTagClick = { tag -> 
                                // Filter notes by tag
                            },
                            onSignOut = onSignOut,
                            onSyncToDrive = onSyncToDrive,
                            onSyncFromDrive = onSyncFromDrive,
                            onTemplates = { showTemplates = true },
                            onCategories = { showCategories = true }
                        )
                    }
                }
                Screen.NOTES -> {
                    NoteScreen(
                        viewModel = viewModel,
                        onSignOut = onSignOut,
                        onSyncToDrive = onSyncToDrive,
                        onSyncFromDrive = onSyncFromDrive
                    )
                }
                Screen.SETTINGS -> {
                    val app = context.applicationContext as AINoteBuddyApplication
                    SettingsScreen(
                        repository = app.advancedNoteRepository,
                        backupManager = app.backupRestoreManager
                    )
                }
            }
        }
    }
}

@Composable
fun SignInPrompt(onSignIn: () -> Unit, onSkip: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign in for sync, backup, and more!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSignIn) { Text("Sign In with Google") }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onSkip) { Text("Skip for now") }
        }
    }
}
