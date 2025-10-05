package com.ainotebuddy.app.ui.components.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ainotebuddy.app.R
import com.ainotebuddy.app.viewmodel.SecurityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Load security settings when the screen is first displayed
    LaunchedEffect(noteId) {
        viewModel.loadSecuritySettings(noteId)
    }
    
    // Show error dialog if there's an error
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(uiState.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // Main content
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Biometric Lock Section
            SecuritySettingItem(
                title = stringResource(R.string.biometric_lock),
                subtitle = stringResource(R.string.biometric_lock_subtitle),
                icon = Icons.Default.Fingerprint,
                checked = uiState.requiresBiometric,
                enabled = uiState.isBiometricAvailable,
                onCheckedChange = { viewModel.toggleBiometricLock(it) }
            )
            
            Divider()
            
            // Encryption Section
            SecuritySettingItem(
                title = stringResource(R.string.encryption),
                subtitle = stringResource(R.string.encryption_subtitle),
                icon = Icons.Default.Lock,
                checked = uiState.isEncrypted,
                onCheckedChange = { viewModel.toggleEncryption(it) }
            )
            
            Divider()
            
            // Self-Destruct Section
            var showDatePicker by remember { mutableStateOf(false) }
            
            SecuritySettingItem(
                title = stringResource(R.string.self_destruct),
                subtitle = uiState.selfDestructTime?.let { date ->
                    val formatter = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                    stringResource(R.string.self_destruct_set_to, formatter.format(date))
                } ?: stringResource(R.string.self_destruct_subtitle),
                icon = Icons.Default.SelfImprovement,
                checked = uiState.selfDestructTime != null,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        showDatePicker = true
                    } else {
                        viewModel.setSelfDestructTime(null)
                    }
                }
            )
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDateSelected = { date ->
                        viewModel.setSelfDestructTime(date)
                        showDatePicker = false
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
            
            Divider()
            
            // Secure Sharing Section
            var showPasswordDialog by remember { mutableStateOf(false) }
            
            SecuritySettingItem(
                title = stringResource(R.string.secure_sharing),
                subtitle = if (uiState.hasSharePassword) {
                    stringResource(R.string.secure_sharing_enabled)
                } else {
                    stringResource(R.string.secure_sharing_subtitle)
                },
                icon = Icons.Default.Share,
                checked = uiState.hasSharePassword,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        showPasswordDialog = true
                    } else {
                        viewModel.setSharePassword(null)
                    }
                }
            )
            
            if (showPasswordDialog) {
                var password by remember { mutableStateOf("") }
                
                ConfirmationDialog(
                    title = stringResource(R.string.set_share_password),
                    onConfirm = {
                        viewModel.setSharePassword(password)
                        showPasswordDialog = false
                    },
                    onDismiss = { showPasswordDialog = false },
                    confirmText = stringResource(R.string.set_password),
                    dismissText = stringResource(R.string.cancel)
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState()
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) onDateSelected(Date(millis)) else onDismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    dismissText: String,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { content() },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } }
    )
}

@Composable
private fun SecuritySettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = null, // Handled by the row click
                enabled = enabled
            )
        }
    }
}
