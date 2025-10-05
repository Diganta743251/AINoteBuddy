package com.ainotebuddy.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onPrivacy: (() -> Unit)? = null) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome!",
            desc = "Meet your AI-powered note buddy.",
            icon = Icons.Filled.Star
        ),
        OnboardingPage(
            title = "Voice Notes",
            desc = "Record and transcribe notes hands-free.",
            icon = Icons.Filled.Mic
        ),
        OnboardingPage(
            title = "Backup & Sync",
            desc = "Keep your notes safe and accessible.",
            icon = Icons.Filled.Cloud
        ),
        OnboardingPage(
            title = "Personalize",
            desc = "Choose your theme and sign in for more.",
            icon = Icons.Filled.Palette
        ),
        OnboardingPage(
            title = "Privacy First",
            desc = "Your notes are secure and private.",
            icon = Icons.Filled.Security
        )
    )
    var page by remember { mutableStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            // Icon and Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(pages[page].icon, contentDescription = null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                Text(pages[page].title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(pages[page].desc, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            // Page indicators
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                pages.forEachIndexed { i, _ ->
                    val color = if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (i == page) 12.dp else 8.dp)
                            .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onFinish) {
                    Text("Skip")
                }
                if (page == pages.lastIndex && onPrivacy != null) {
                    TextButton(onClick = onPrivacy) {
                        Text("Privacy Policy")
                    }
                }
                Button(onClick = {
                    if (page < pages.lastIndex) page++ else onFinish()
                }) {
                    Text(if (page < pages.lastIndex) "Next" else "Get Started")
                }
            }
        }
    }
}

data class OnboardingPage(val title: String, val desc: String, val icon: ImageVector) 