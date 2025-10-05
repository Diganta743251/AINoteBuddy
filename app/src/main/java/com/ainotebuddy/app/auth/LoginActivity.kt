package com.ainotebuddy.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.MainActivity
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class LoginActivity : ComponentActivity() {
    
    private lateinit var googleAuthService: GoogleAuthService
    
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            onSignInSuccess(account)
        } catch (e: ApiException) {
            onSignInFailure(e)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check Google Play Services availability
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Toast.makeText(this, "Google Play Services not available", Toast.LENGTH_LONG).show()
            }
            return
        }
        
        googleAuthService = GoogleAuthService(this)
        
        // Check if user is already signed in
        if (googleAuthService.isSignedIn()) {
            startMainActivity()
            return
        }
        
        setContent {
            AINoteBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onSignInClick = { startGoogleSignIn() }
                    )
                }
            }
        }
    }
    
    private fun startGoogleSignIn() {
        val signInIntent = googleAuthService.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }
    
    private fun onSignInSuccess(account: GoogleSignInAccount) {
        Toast.makeText(this, "Welcome, ${account.displayName}!", Toast.LENGTH_SHORT).show()
        startMainActivity()
    }
    
    private fun onSignInFailure(exception: ApiException) {
        val message = when (exception.statusCode) {
            12501 -> "Sign in was cancelled"
            12500 -> "Sign in failed. Please try again."
            10 -> "Developer error: Check SHA1 fingerprint and OAuth configuration"
            7 -> "Network error: Check your internet connection"
            8 -> "Internal error: Please try again later"
            else -> "Sign in failed (Code: ${exception.statusCode}): ${exception.message}"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Log the error for debugging
        android.util.Log.e("LoginActivity", "Sign in failed with code: ${exception.statusCode}", exception)
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun LoginScreen(onSignInClick: () -> Unit, onSkip: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AINoteBuddy",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your AI-powered note-taking companion",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Sign in with Google",
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Sign in to sync your notes with Google Drive",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Show skip button only when onSkip is provided (for MainActivity)
        onSkip?.let { skipAction ->
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = skipAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for now",
                    fontSize = 14.sp
                )
            }
        }
    }
} 