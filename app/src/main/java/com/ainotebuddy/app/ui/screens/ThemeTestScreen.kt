package com.ainotebuddy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.ui.components.PreferenceItem
import com.ainotebuddy.app.ui.theme.AppTheme
import com.ainotebuddy.app.ui.theme.ThemeManager
import com.ainotebuddy.app.ui.theme.displayName

@Composable
fun ThemeTestScreen(
    themeManager: ThemeManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeState by themeManager.themeState.observeAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme & Display Test") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
        ) {
            // Theme Selection
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Theme Options
            AppTheme.entries.forEach { theme ->
                PreferenceItem(
                    title = when (theme) {
                        AppTheme.LIGHT -> "Light"
                        AppTheme.DARK -> "Dark"
                        AppTheme.SYSTEM -> "System Default"
                        AppTheme.MATERIAL_YOU -> "Material You"
                        AppTheme.FUTURISTIC -> "Futuristic"
                        else -> theme.displayName
                    },
                    onClick = { themeManager.setTheme(theme) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Font Size Slider
            Text(
                text = "Font Size",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Slider(
                value = themeState?.fontScale ?: 1f,
                onValueChange = { themeManager.setFontScale(it) },
                valueRange = 0.8f..1.5f,
                steps = 6,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Preview Section
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Heading 1",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Text(
                        text = "Subtitle",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This is a sample text to demonstrate the current theme and font size settings. " +
                                "You can adjust the appearance using the controls above.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Sample Button")
                    }
                }
            }
            
            // Display current settings
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Current Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    InfoRow("Theme", themeState?.currentTheme?.name ?: "System")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Dark Mode", if (themeState?.isDarkMode == true) "Yes" else "No")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Font Scale", "${String.format("%.1f", themeState?.fontScale ?: 1f)}x")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
