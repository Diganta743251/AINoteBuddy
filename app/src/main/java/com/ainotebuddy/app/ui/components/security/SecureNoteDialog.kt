package com.ainotebuddy.app.ui.components.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.ainotebuddy.app.R
import com.ainotebuddy.app.security.BiometricManager

@Composable
fun SecureNoteDialog(
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit,
    requiresBiometric: Boolean,
    requiresPassword: Boolean,
    onPasswordEntered: (String) -> Unit,
    biometricManager: BiometricManager = BiometricManager(LocalContext.current)
) {
    var showPassword by remember { mutableStateOf(requiresPassword) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
    // Show biometric prompt if required
    LaunchedEffect(Unit) {
        if (requiresBiometric && !showPassword) {
            if (context is FragmentActivity) {
                biometricManager.showBiometricPrompt(
                    activity = context,
                    onSuccess = { onAuthenticated() },
                    onError = { _, err -> 
                        error = err.toString()
                        showPassword = true
                    }
                )
            } else {
                error = context.getString(R.string.biometric_authentication_failed)
                showPassword = true
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.authentication_required),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Subtitle
                Text(
                    text = stringResource(R.string.verify_identity_to_continue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Password field (shown if password is required or biometric failed)
                if (showPassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None 
                                           else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility 
                                               else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" 
                                                      else "Show password"
                                )
                            }
                        },
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Error message
                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Submit button
                    Button(
                        onClick = {
                            if (password.isNotBlank()) {
                                onPasswordEntered(password)
                            } else {
                                error = context.getString(R.string.password_cannot_be_empty)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.unlock))
                    }
                    
                    // Use biometric instead (if available)
                    if (requiresBiometric) {
                        TextButton(
                            onClick = {
                                showPassword = false
                                error = null
                                password = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.use_biometric_instead))
                        }
                    }
                } else {
                    // Show loading indicator while waiting for biometric prompt
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
