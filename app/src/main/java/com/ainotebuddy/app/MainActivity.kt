package com.ainotebuddy.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.features.NoteEditorActivity
import com.ainotebuddy.app.features.SearchActivity
import com.ainotebuddy.app.features.SettingsActivity
import com.ainotebuddy.app.features.VoiceRecorderActivity
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.ainotebuddy.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AINoteBuddyTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val viewModel: MainViewModel = viewModel { MainViewModel(context) }
        val notes by viewModel.allNotes.collectAsState()
        val currentPage = remember { mutableStateOf("Dashboard") }

        Scaffold(
            bottomBar = {
                ModernBottomNavigation(
                    currentPage = currentPage.value,
                    onPageSelected = { page -> currentPage.value = page },
                    onNotesClick = { /* Show all notes */ },
                    onSettingsClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val intent = Intent(context, NoteEditorActivity::class.java)
                        startActivity(intent)
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Filled.Add, 
                        contentDescription = "Add Note",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) { paddingValues ->
            when (currentPage.value) {
                "Dashboard" -> DashboardContent(
                    modifier = Modifier.padding(paddingValues),
                    notes = notes,
                    onCreateNote = {
                        val intent = Intent(context, NoteEditorActivity::class.java)
                        startActivity(intent)
                    },
                    onSearchClick = {
                        val intent = Intent(context, SearchActivity::class.java)
                        startActivity(intent)
                    },
                    onVoiceClick = {
                        val intent = Intent(context, VoiceRecorderActivity::class.java)
                        startActivity(intent)
                    },
                    onNoteClick = { note ->
                        val intent = Intent(context, NoteEditorActivity::class.java).apply {
                            putExtra("note_id", note.id)
                        }
                        startActivity(intent)
                    }
                )
                "Notes" -> NotesListContent(
                    modifier = Modifier.padding(paddingValues),
                    notes = notes,
                    onNoteClick = { note ->
                        val intent = Intent(context, NoteEditorActivity::class.java).apply {
                            putExtra("note_id", note.id)
                        }
                        startActivity(intent)
                    },
                    onPinClick = { note -> viewModel.togglePin(note.id) },
                    onFavoriteClick = { note -> viewModel.toggleFavorite(note.id) }
                )
                "Settings" -> {
                    LaunchedEffect(Unit) {
                        val intent = Intent(context, SettingsActivity::class.java)
                        startActivity(intent)
                        currentPage.value = "Dashboard"
                    }
                }
            }
        }
    }

    @Composable
    fun DashboardContent(
        modifier: Modifier = Modifier,
        notes: List<NoteEntity>,
        onCreateNote: () -> Unit,
        onSearchClick: () -> Unit,
        onVoiceClick: () -> Unit,
        onNoteClick: (NoteEntity) -> Unit
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                WelcomeCard(
                    onCreateNote = onCreateNote,
                    onSearchClick = onSearchClick
                )
            }
            
            item {
                StatsRow(
                    totalNotes = notes.size,
                    favorites = notes.count { it.isFavorite },
                    pinned = notes.count { it.isPinned },
                    inVault = 0
                )
            }
            
            item {
                QuickActionsCard(
                    onVoiceClick = onVoiceClick,
                    onTemplatesClick = { /* TODO */ },
                    onCategoriesClick = { /* TODO */ }
                )
            }
            
            if (notes.isNotEmpty()) {
                item {
                    CategoriesSection(notes = notes, onNoteClick = onNoteClick)
                }
            }
        }
    }
    
    @Composable
    fun NotesListContent(
        modifier: Modifier = Modifier,
        notes: List<NoteEntity>,
        onNoteClick: (NoteEntity) -> Unit,
        onPinClick: (NoteEntity) -> Unit,
        onFavoriteClick: (NoteEntity) -> Unit
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "${notes.size} notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(notes) { note ->
                ModernNoteCard(
                    note = note,
                    onClick = { onNoteClick(note) },
                    onPinClick = { onPinClick(note) },
                    onFavoriteClick = { onFavoriteClick(note) }
                )
            }
        }
    }
    
    @Composable
    fun WelcomeCard(
        onCreateNote: () -> Unit,
        onSearchClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back! 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ready to capture your thoughts?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onCreateNote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create New Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    
    @Composable
    fun StatsRow(
        totalNotes: Int,
        favorites: Int,
        pinned: Int,
        inVault: Int
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatCard(
                    icon = Icons.Filled.Description,
                    count = totalNotes,
                    label = "Total Notes",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                StatCard(
                    icon = Icons.Filled.Favorite,
                    count = favorites,
                    label = "Favorites",
                    color = Color(0xFFFC5C7D)
                )
            }
            item {
                StatCard(
                    icon = Icons.Filled.Star,
                    count = pinned,
                    label = "Pinned",
                    color = Color(0xFFFFD700)
                )
            }
            item {
                StatCard(
                    icon = Icons.Filled.Lock,
                    count = inVault,
                    label = "In Vault",
                    color = Color(0xFF00BCD4)
                )
            }
        }
    }
    
    @Composable
    fun StatCard(
        icon: ImageVector,
        count: Int,
        label: String,
        color: Color
    ) {
        Card(
            modifier = Modifier.width(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    @Composable
    fun QuickActionsCard(
        onVoiceClick: () -> Unit,
        onTemplatesClick: () -> Unit,
        onCategoriesClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Description,
                        label = "Templates",
                        color = Color(0xFF00BCD4),
                        onClick = onTemplatesClick
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Folder,
                        label = "Categories",
                        color = Color(0xFFFFD700),
                        onClick = onCategoriesClick
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                QuickActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Mic,
                    label = "Voice Note",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onVoiceClick
                )
            }
        }
    }
    
    @Composable
    fun QuickActionButton(
        modifier: Modifier = Modifier,
        icon: ImageVector,
        label: String,
        color: Color,
        onClick: () -> Unit
    ) {
        Card(
            modifier = modifier
                .clickable { onClick() }
                .height(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    
    @Composable
    fun CategoriesSection(
        notes: List<NoteEntity>,
        onNoteClick: (NoteEntity) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNoteClick(notes.first()) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "General",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${notes.size} notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun ModernNoteCard(
        note: NoteEntity,
        onClick: () -> Unit,
        onPinClick: () -> Unit,
        onFavoriteClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = note.title.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (note.content.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                    
                    Row {
                        if (note.isPinned) {
                            IconButton(
                                onClick = onPinClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "Unpin",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (note.isFavorite) {
                            IconButton(
                                onClick = onFavoriteClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = "Remove favorite",
                                    tint = Color(0xFFFC5C7D),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(note.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "General",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun ModernBottomNavigation(
        currentPage: String,
        onPageSelected: (String) -> Unit,
        onNotesClick: () -> Unit,
        onSettingsClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem(
                    icon = Icons.Filled.Dashboard,
                    label = "Dashboard",
                    isSelected = currentPage == "Dashboard",
                    onClick = { onPageSelected("Dashboard") }
                )
                BottomNavItem(
                    icon = Icons.Filled.Description,
                    label = "Notes",
                    isSelected = currentPage == "Notes",
                    onClick = { onPageSelected("Notes") }
                )
                BottomNavItem(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    isSelected = currentPage == "Settings",
                    onClick = onSettingsClick
                )
            }
        }
    }
    
    @Composable
    fun BottomNavItem(
        icon: ImageVector,
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val backgroundColor = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        }
        
        val contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = backgroundColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
