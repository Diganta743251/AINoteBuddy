package com.ainotebuddy.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.ui.components.GlassCard
import com.ainotebuddy.app.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDashboardScreen(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    onNewNote: () -> Unit,
    onSearch: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTemplates: () -> Unit = {},
    onCategories: () -> Unit = {},
    onShowAllNotes: () -> Unit = {},
    onShowFavorites: () -> Unit = {},
    onShowPinned: () -> Unit = {},
    onShowVault: () -> Unit = {},
    onCustomizeDashboard: () -> Unit = {},
    onCustomizeFAB: () -> Unit = {},
    onShowPresets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
                    IconButton(onClick = onNewNote) { Icon(Icons.Filled.Add, contentDescription = "New Note") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welcome back! 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Notes: ${notes.size}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes.take(10)) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                note.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (note.content.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(note.content.take(100) + if (note.content.length > 100) "..." else "")
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onNoteClick(note) }) { Text("Open") }
                            }
                        }
                    }
                }
            }
        }
    }
}

 

@Composable
fun EnhancedDashboardHeader(
    onSearch: () -> Unit,
    onNewNote: () -> Unit,
    onCustomize: () -> Unit,
    onPresets: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Welcome back! 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Ready to capture your thoughts?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onPresets,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Dashboard,
                            contentDescription = "Presets",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = onCustomize,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = "Customize",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = onSearch,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNewNote,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF667eea)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Note", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickCustomizationMenu(
    modifier: Modifier = Modifier,
    onCustomizeDashboard: () -> Unit,
    onResetLayout: () -> Unit,
    onPresets: () -> Unit = {},
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            QuickActionItem(
                icon = Icons.Filled.Dashboard,
                text = "Layout Presets",
                onClick = onPresets
            )
            
            QuickActionItem(
                icon = Icons.Filled.Tune,
                text = "Customize Widgets",
                onClick = onCustomizeDashboard
            )
            
            QuickActionItem(
                icon = Icons.Filled.Save,
                text = "Save as Preset",
                onClick = {
                    // This will be handled by showing the custom preset creation dialog
                    onDismiss()
                    // TODO: Show custom preset creation dialog
                }
            )
            
            QuickActionItem(
                icon = Icons.Filled.Refresh,
                text = "Reset Layout",
                onClick = onResetLayout
            )
            
            Divider(
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            QuickActionItem(
                icon = Icons.Filled.Close,
                text = "Close",
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun EmptyDashboardState(
    onCustomize: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Dashboard,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your Dashboard is Empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add some widgets to personalize your dashboard and get quick access to your notes.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onCustomize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A82FB),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Widgets")
            }
        }
    }
}
