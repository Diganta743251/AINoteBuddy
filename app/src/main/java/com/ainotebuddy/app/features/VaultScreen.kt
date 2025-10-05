package com.ainotebuddy.app.features

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.ui.dashboard.PremiumNoteCard
import com.ainotebuddy.app.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val vaultNotes = remember(notes) { notes.filter { it.isInVault } }
    var isUnlocked by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        authenticateUser(
            context = context,
            onSuccess = { isUnlocked = true },
            onError = { showError = true }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Secure Vault")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isUnlocked) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vaultNotes) { note ->
                    PremiumNoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onFavorite = { viewModel.toggleFavorite(note.id) },
                        onPin = { viewModel.togglePin(note.id) },
                        onMoveToVault = { /* TODO: implement vault move */ }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Use biometric authentication to access your secure notes",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(32.dp))

                if (showError) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "Authentication failed. Please try again.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        showError = false
                        authenticateUser(
                            context = context,
                            onSuccess = { isUnlocked = true },
                            onError = { showError = true }
                        )
                    }
                ) {
                    Text("Unlock Vault")
                }
            }
        }
    }
}

private fun authenticateUser(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val biometricManager = BiometricManager.from(context)
    
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val activity = context as? FragmentActivity ?: return
            
            val biometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError()
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError()
                    }
                }
            )
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Secure Vault")
                .setSubtitle("Use your biometric credential to access secure notes")
                .setNegativeButtonText("Cancel")
                .build()
                
            biometricPrompt.authenticate(promptInfo)
        }
        else -> {
            // Biometric not available, allow access (or implement PIN fallback)
            onSuccess()
        }
    }
}