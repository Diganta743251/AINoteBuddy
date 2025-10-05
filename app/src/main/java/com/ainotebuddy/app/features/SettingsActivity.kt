package com.ainotebuddy.app.features

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.ainotebuddy.app.viewmodel.SimpleSettingsViewModel

class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AINoteBuddyTheme {
                SettingsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen() {
        val viewModel: SimpleSettingsViewModel = viewModel { SimpleSettingsViewModel(this@SettingsActivity) }
        val uiState by viewModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Card
            SettingsHeader()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Theme & Appearance
                item {
                    ModernSettingsCard(
                        title = "🎨 Theme & Appearance",
                        description = "Customize your experience"
                    ) {
                        ThemeSelectionSection()
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        ModernSwitchItem(
                            icon = Icons.Filled.DarkMode,
                            title = "Dark Mode",
                            subtitle = "AMOLED black for battery saving",
                            checked = uiState.darkModeEnabled,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                }
                
                // AI Settings
                item {
                    ModernSettingsCard(
                        title = "🤖 AI Settings",
                        description = "Configure intelligent features"
                    ) {
                        ModernSwitchItem(
                            icon = Icons.Filled.AutoAwesome,
                            title = "AI Assistant",
                            subtitle = "Smart suggestions and insights",
                            checked = uiState.aiSuggestionsEnabled,
                            onCheckedChange = { viewModel.toggleAISuggestions() }
                        )
                        
                        ModernSwitchItem(
                            icon = Icons.Filled.Save,
                            title = "Auto-save",
                            subtitle = "Save notes automatically while typing",
                            checked = uiState.autoSaveEnabled,
                            onCheckedChange = { viewModel.toggleAutoSave() }
                        )
                    }
                }
                
                // Security
                item {
                    ModernSettingsCard(
                        title = "🔒 Security",
                        description = "Keep your notes private"
                    ) {
                        ModernSwitchItem(
                            icon = Icons.Filled.Lock,
                            title = "App Lock",
                            subtitle = "Biometric or PIN protection",
                            checked = uiState.appLockEnabled,
                            onCheckedChange = { viewModel.toggleAppLock() }
                        )
                    }
                }
                
                // Data Management
                item {
                    ModernSettingsCard(
                        title = "💾 Data Management",
                        description = "Backup and restore your notes"
                    ) {
                        ModernActionItem(
                            icon = Icons.Filled.CloudUpload,
                            title = "Export Notes",
                            subtitle = "Save all notes to device storage",
                            onClick = { viewModel.exportNotes() }
                        )
                        
                        ModernActionItem(
                            icon = Icons.Filled.CloudDownload,
                            title = "Import Notes",
                            subtitle = "Restore notes from backup file",
                            onClick = { viewModel.importNotes() }
                        )
                        
                        ModernActionItem(
                            icon = Icons.Filled.Delete,
                            title = "Clear All Data",
                            subtitle = "Delete all notes and settings",
                            isDestructive = true,
                            onClick = { viewModel.clearAllData() }
                        )
                    }
                }
                
                // About
                item {
                    ModernSettingsCard(
                        title = "ℹ️ About",
                        description = "App information and support"
                    ) {
                        ModernInfoItem(
                            icon = Icons.Filled.Info,
                            title = "Version",
                            value = "2.1.0"
                        )
                        
                        ModernActionItem(
                            icon = Icons.Filled.Help,
                            title = "Help & Support",
                            subtitle = "Get assistance and report issues",
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun SettingsHeader() {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { finish() },
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
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Customize your experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    @Composable
    fun ModernSettingsCard(
        title: String,
        description: String,
        content: @Composable ColumnScope.() -> Unit
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
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                content()
            }
        }
    }
    
    @Composable
    fun ThemeSelectionSection() {
        val themes = listOf(
            "☀️ Light Theme" to "Clean and bright interface",
            "🌙 Dark Theme" to "AMOLED black for battery saving",
            "🎨 Material You" to "Dynamic colors based on wallpaper",
            "🔮 Futuristic" to "Cyberpunk-inspired design"
        )
        val selectedTheme = remember { mutableStateOf(3) } // Futuristic selected
        
        Column {
            themes.forEachIndexed { index, (title, subtitle) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTheme.value = index }
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTheme.value == index) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (selectedTheme.value == index) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (selectedTheme.value == index) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun ModernSwitchItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f)
                )
            )
        }
    }
    
    @Composable
    fun ModernActionItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        isDestructive: Boolean = false,
        onClick: () -> Unit
    ) {
        val iconColor = if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
        
        val titleColor = if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    
    @Composable
    fun ModernInfoItem(
        icon: ImageVector,
        title: String,
        value: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}