package com.ainotebuddy.app.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ainotebuddy.app.repository.NoteRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.work.WorkManager
import com.ainotebuddy.app.workers.EmbeddingUpdateWorker
import com.ainotebuddy.app.ui.components.GlassCard
import com.ainotebuddy.app.ads.AdManager
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.ainotebuddy.app.ui.theme.ThemeManager
import com.ainotebuddy.app.ui.theme.AppTheme


@Composable
fun SettingsScreen(
    repository: NoteRepository,
    onSignOut: () -> Unit = {},
    onSyncToDrive: () -> Unit = {},
    onSyncFromDrive: () -> Unit = {},
    onAISettingsClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val themeState by ThemeManager.themeState.observeAsState(com.ainotebuddy.app.ui.theme.ThemeState())

    var backupStatus by remember { mutableStateOf("") }
    var showExportOptions by remember { mutableStateOf(false) }
    var showBiometricSettings by remember { mutableStateOf(false) }
    var showVaultSettings by remember { mutableStateOf(false) }
    var showEncryptionSettings by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { backupStatus = "Backup feature coming soon" }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { backupStatus = "Restore feature coming soon" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ThemeSection(themeState = themeState) }
        item {
            BackupRestoreSection(
                backupStatus = backupStatus,
                onBackup = { backupLauncher.launch("ainotebuddy_backup.json") },
                onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                onExportOptions = { showExportOptions = true }
            )
        }
        item {
            SecuritySection(
                onShowBiometricSettings = { showBiometricSettings = true },
                onShowVaultSettings = { showVaultSettings = true },
                onShowEncryptionSettings = { showEncryptionSettings = true }
            )
        }
        item { AccountSection(onSignOut = onSignOut) }
        item {
            SyncSection(
                onSyncToDrive = onSyncToDrive,
                onSyncFromDrive = onSyncFromDrive,
                onShowAutoSyncSettings = { /* TODO */ },
                onShowSyncSettings = { /* TODO */ }
            )
        }
        item { PrivacyLegalSection(onPrivacy = { /* TODO */ }, onAbout = { /* TODO */ }) }
        item { AppInfoSection() }
    }

    if (showExportOptions) {
        ExportOptionsDialog(
            onDismiss = { showExportOptions = false },
            onExportText = { /* TODO */ },
            onExportPdf = { /* TODO */ },
            onExportMarkdown = { /* TODO */ }
        )
    }

    if (showBiometricSettings) {
        BiometricSettingsDialog(onDismiss = { showBiometricSettings = false })
    }
    if (showVaultSettings) {
        VaultSettingsDialog(onDismiss = { showVaultSettings = false })
    }
    if (showEncryptionSettings) {
        EncryptionSettingsDialog(onDismiss = { showEncryptionSettings = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSection(themeState: com.ainotebuddy.app.ui.theme.ThemeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Section Header with Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Palette,
                    contentDescription = null,
                    tint = Color(0xFF6A82FB),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Theme & Appearance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Decorative divider
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6A82FB),
                                Color(0xFFFC5C7D),
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            // Theme Dropdown Selection
            var expanded by remember { mutableStateOf(false) }
            val themes = listOf(
                AppTheme.LIGHT to "☀️ Light Theme",
                AppTheme.DARK to "🌙 Dark Theme", 
                AppTheme.MATERIAL_YOU to "🎨 Material You",
                AppTheme.FUTURISTIC to "🧪 Futuristic"
            )
            val currentThemeText = themes.find { it.first == themeState.currentTheme }?.second ?: "☀️ Light Theme"
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentThemeText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selected Theme", color = Color.White.copy(alpha = 0.8f)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6A82FB),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6A82FB)
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    themes.forEach { (theme, title) ->
                        DropdownMenuItem(
                            text = { Text(title) },
                            onClick = {
                                ThemeManager.setTheme(theme)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Dynamic Colors Sub-option (nested under Material You)
            if (themeState.currentTheme == AppTheme.MATERIAL_YOU) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF6A82FB).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "└─ Dynamic Colors",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = themeState.useDynamicColors,
                            onCheckedChange = { ThemeManager.toggleDynamicColors() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6A82FB),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackupRestoreSection(
    backupStatus: String,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportOptions: () -> Unit
) {
    GlassCard {
        Column {
            SectionTitle(
                title = "Backup & Restore",
                icon = Icons.Filled.Backup,
                color = Color(0xFFFC5C7D)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsButton(
                    title = "Backup Notes",
                    subtitle = "Save all notes to file",
                    icon = Icons.Filled.CloudUpload,
                    onClick = onBackup
                )
                SettingsButton(
                    title = "Restore Notes",
                    subtitle = "Load notes from backup",
                    icon = Icons.Filled.CloudDownload,
                    onClick = onRestore
                )
                SettingsButton(
                    title = "Export Options",
                    subtitle = "Export in different formats",
                    icon = Icons.Filled.Download,
                    onClick = onExportOptions
                )
            }
            
            if (backupStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF00FFC6).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        backupStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF00FFC6),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SecuritySection(
    onShowBiometricSettings: () -> Unit,
    onShowVaultSettings: () -> Unit,
    onShowEncryptionSettings: () -> Unit
) {
    GlassCard {
        Column {
            SectionTitle(
                title = "Security",
                icon = Icons.Filled.Security,
                color = Color(0xFF00FFC6)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsButton(
                    title = "Biometric Lock",
                    subtitle = "Use fingerprint or face unlock",
                    icon = Icons.Filled.Fingerprint,
                    onClick = onShowBiometricSettings
                )
                SettingsButton(
                    title = "Vault Settings",
                    subtitle = "Configure secure note vault",
                    icon = Icons.Filled.Lock,
                    onClick = onShowVaultSettings
                )
                SettingsButton(
                    title = "Encryption",
                    subtitle = "Enable end-to-end encryption",
                    icon = Icons.Filled.Lock,
                    onClick = onShowEncryptionSettings
                )
            }
        }
    }
}

@Composable
fun AccountSection(onSignOut: () -> Unit = {}) {
    GlassCard {
        Column {
            SectionTitle(
                title = "Account",
                icon = Icons.Filled.AccountCircle,
                color = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsButton(
                    title = "Sign Out",
                    subtitle = "Sign out of your account",
                    icon = Icons.Filled.Logout,
                    onClick = onSignOut
                )
                SettingsButton(
                    title = "Account Settings",
                    subtitle = "Manage your account preferences",
                    icon = Icons.Filled.ManageAccounts,
                    onClick = { 
                        // Navigate to account settings or show account dialog
                        // For now, show a simple info dialog
                    }
                )
            }
        }
    }
}

@Composable
fun SyncSection(
    onSyncToDrive: () -> Unit = {},
    onSyncFromDrive: () -> Unit = {},
    onShowAutoSyncSettings: () -> Unit,
    onShowSyncSettings: () -> Unit
) {
    GlassCard {
        Column {
            SectionTitle(
                title = "Sync & Cloud",
                icon = Icons.Filled.Sync,
                color = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsButton(
                    title = "Upload to Drive",
                    subtitle = "Sync notes to Google Drive",
                    icon = Icons.Filled.CloudUpload,
                    onClick = onSyncToDrive
                )
                SettingsButton(
                    title = "Download from Drive",
                    subtitle = "Sync notes from Google Drive",
                    icon = Icons.Filled.CloudDownload,
                    onClick = onSyncFromDrive
                )
                SettingsButton(
                    title = "Auto Sync",
                    subtitle = "Automatically sync changes",
                    icon = Icons.Filled.Sync,
                    onClick = onShowAutoSyncSettings
                )
                SettingsButton(
                    title = "Sync Settings",
                    subtitle = "Configure sync preferences",
                    icon = Icons.Filled.Settings,
                    onClick = onShowSyncSettings
                )
            }
        }
    }
}

@Composable
fun PrivacyLegalSection(
    onPrivacy: () -> Unit,
    onAbout: () -> Unit
) {
    GlassCard {
        Column {
            SectionTitle(
                title = "Privacy & Legal",
                icon = Icons.Filled.Policy,
                color = Color(0xFF6A82FB)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsButton(
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    icon = Icons.Filled.Policy,
                    onClick = onPrivacy
                )
                SettingsButton(
                    title = "Terms of Service",
                    subtitle = "Read our terms of service",
                    icon = Icons.Filled.Description,
                    onClick = { 
                        // Open terms of service URL or show terms dialog
                        // Implementation would open browser or show in-app dialog
                    }
                )
                SettingsButton(
                    title = "About",
                    subtitle = "App information and credits",
                    icon = Icons.Filled.Info,
                    onClick = onAbout
                )
            }
        }
    }
}

@Composable
fun AppInfoSection() {
    GlassCard {
        Column {
            SectionTitle(
                title = "App Information",
                icon = Icons.Filled.Info,
                color = Color(0xFFFC5C7D)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(
                    label = "Version",
                    value = "2.0.0"
                )
                InfoRow(
                    label = "Build",
                    value = "2024.1"
                )
                InfoRow(
                    label = "Developer",
                    value = "AINoteBuddy Team"
                )
                InfoRow(
                    label = "Support",
                    value = "support@ainotebuddy.com"
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color.White.copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExportText: () -> Unit,
    onExportPdf: () -> Unit,
    onExportMarkdown: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Export Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SettingsButton(
                            title = "Export as Text",
                            subtitle = "Plain text format",
                            icon = Icons.Filled.TextFields,
                            onClick = {
                                onExportText()
                                onDismiss()
                            }
                        )
                    }
                    item {
                        SettingsButton(
                            title = "Export as PDF",
                            subtitle = "Portable document format",
                            icon = Icons.Filled.PictureAsPdf,
                            onClick = {
                                onExportPdf()
                                onDismiss()
                            }
                        )
                    }
                    item {
                        SettingsButton(
                            title = "Export as Markdown",
                            subtitle = "Markdown format",
                            icon = Icons.Filled.Code,
                            onClick = {
                                onExportMarkdown()
                                onDismiss()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun AiProcessingSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { com.ainotebuddy.app.data.preferences.SettingsRepository(context) }
    val paused by settingsRepo.pauseAIProcessing.collectAsState(initial = false)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pause AI Processing", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    "Stops background embeddings while charging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = paused,
                onCheckedChange = { checked ->
                    scope.launch {
                        settingsRepo.setPauseAIProcessing(checked)
                        if (checked) {
                            WorkManager.getInstance(context).cancelUniqueWork("embedding_update")
                        } else {
                            EmbeddingUpdateWorker.schedule(context)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AISettingsSection(onAISettingsClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Section Header with Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = Color(0xFF00FFC6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "AI Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Decorative divider
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00FFC6),
                                Color(0xFF6A82FB),
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            // AI Assistant Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAISettingsClick() },
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🤖",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure AI providers and settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // AI Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00FFC6).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📶",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "AI Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to configure AI settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun APIKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "OpenAI API Key Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Enter your OpenAI API key to enable advanced AI features.\nYour key is stored securely on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API Key", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("sk-...", color = Color.White.copy(alpha = 0.5f)) },
                    visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None 
                                         else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.7f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { onSave(keyInput.trim()) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Get your API key from: https://platform.openai.com/api-keys",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun BiometricSettingsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Biometric Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Biometric authentication will be available in a future update. This feature will allow you to secure your notes with fingerprint or face unlock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun VaultSettingsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Vault Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "The secure vault feature will be available in a future update. This will allow you to store sensitive notes in an encrypted vault with additional security measures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun EncryptionSettingsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Encryption Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "End-to-end encryption will be available in a future update. This feature will ensure that your notes are encrypted both locally and during sync, providing maximum security for your data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun PremiumFeaturesSection() {
    val context = LocalContext.current
    val adManager = AdManager.getInstance(context)
    var premiumUnlocked by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        adManager.initialize()
    }
    
    GlassCard {
        Column {
            SectionTitle(
                title = "Premium Features",
                icon = Icons.Filled.Star,
                color = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!premiumUnlocked) {
                // Watch ad to unlock premium features temporarily
                Button(
                    onClick = {
                        try {
                            val activity = context as? androidx.activity.ComponentActivity
                            if (activity != null && adManager.isInitialized.value && adManager.rewardedAdManager?.isAdReady() == true) {
                                adManager.rewardedAdManager?.showAd(
                                    activity = activity,
                                    onRewardEarned = { amount, type ->
                                        premiumUnlocked = true
                                        android.widget.Toast.makeText(
                                            context,
                                            "Premium features unlocked for 24 hours!",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    if (activity == null) "Unable to show ad from this context" 
                                    else "Ad not ready, please try again later",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Unable to load ad: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Watch Ad for Premium Access", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Watch a short ad to unlock premium features for 24 hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Premium features unlocked
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Premium features unlocked!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedThemeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    hasSubOption: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFF6A82FB).copy(alpha = 0.2f) 
            else 
                Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) 
                Color(0xFF6A82FB) 
            else 
                Color.White.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
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
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF6A82FB),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1a1a2e).copy(alpha = 0.9f),
                    Color(0xFF16213e).copy(alpha = 0.9f),
                    Color(0xFF0f3460).copy(alpha = 0.9f),
                    Color(0xFF533483).copy(alpha = 0.9f)
                ),
                start = Offset(0f, offset * 0.5f),
                end = Offset(offset * 0.5f, size.height)
            )
        )
    }
}