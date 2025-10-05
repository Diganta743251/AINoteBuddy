package com.ainotebuddy.app.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ainotebuddy.app.R
import com.ainotebuddy.app.ui.theme.*
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.compose.foundation.border
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    themeManager: ThemeManager,
    onBackClick: () -> Unit
) {
    val themeState by themeManager.themeState.observeAsState(ThemeState())

    val coroutineScope = rememberCoroutineScope()
    val dynamicColorService = rememberDynamicColorService()
    val isDynamicColorAvailable = dynamicColorService.isDynamicColorAvailable()
    var showMessage by remember { mutableStateOf<String?>(null) }
    
    // Collect export/import status flows
    LaunchedEffect(themeManager) {
        themeManager.exportStatus
            .onEach { status ->
                when (status) {
                    is ThemeManager.ExportStatus.Success -> showMessage = status.message
                    is ThemeManager.ExportStatus.Error -> showMessage = status.message
                    else -> {}
                }
            }
            .launchIn(this)

        themeManager.importStatus
            .onEach { status ->
                when (status) {
                    is ThemeManager.ImportStatus.Success -> showMessage = status.message
                    is ThemeManager.ImportStatus.Error -> showMessage = status.message
                    else -> {}
                }
            }
            .launchIn(this)
    }
    
    // Show snackbar for messages
    if (showMessage != null) {
        LaunchedEffect(showMessage) {
            delay(3000) // Auto-dismiss after 3 seconds
            showMessage = null
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text(
                text = showMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val context = LocalContext.current
        var showExportDialog by remember { mutableStateOf(false) }
        var exportName by remember { mutableStateOf("") }
        var exportDescription by remember { mutableStateOf("") }
        var showImportDialog by remember { mutableStateOf(false) }
        
        // File picker for import
        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { 
                coroutineScope.launch {
                    val result = themeManager.importThemePreset(uri)
                    // Show result to user
                }
            }
        }
        
        // File saver for export
        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let {
                coroutineScope.launch {
                    val result = themeManager.exportThemePreset(
                        name = exportName,
                        description = exportDescription,
                        uri = uri
                    )
                    // Show result to user
                    showExportDialog = false
                }
            }
        }
        
        // Show export dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Theme") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = exportName,
                            onValueChange = { exportName = it },
                            label = { Text("Theme Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = exportDescription,
                            onValueChange = { exportDescription = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            if (exportName.isNotBlank()) {
                                exportLauncher.launch("theme_${exportName.lowercase().replace(" ", "_")}.json")
                            }
                        },
                        enabled = exportName.isNotBlank()
                    ) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Theme Type Selection
            Text(
                "Theme Style",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Base Themes
            ThemeOptionGrid(
                themes = AppTheme.baseThemes.filter {
                    it != AppTheme.DYNAMIC || isDynamicColorAvailable
                },
                selectedTheme = themeState.currentTheme,
                onThemeSelected = { theme ->
                    coroutineScope.launch {
                        if (theme == AppTheme.DYNAMIC && isDynamicColorAvailable) {
                            // Extract colors from wallpaper for dynamic theme
                            val colors = dynamicColorService.extractWallpaperColors()
                            if (colors != null) {
                                themeManager.updateCustomColors(mapOf(
                                    "primary" to colors.primary,
                                    "onPrimary" to colors.onPrimary,
                                    "primaryContainer" to colors.primaryContainer,
                                    "onPrimaryContainer" to colors.onPrimaryContainer,
                                    "secondary" to colors.secondary,
                                    "onSecondary" to colors.onSecondary,
                                    "secondaryContainer" to colors.secondaryContainer,
                                    "onSecondaryContainer" to colors.onSecondaryContainer,
                                    "tertiary" to colors.tertiary,
                                    "onTertiary" to colors.onTertiary,
                                    "background" to colors.background,
                                    "onBackground" to colors.onBackground,
                                    "surface" to colors.surface,
                                    "onSurface" to colors.onSurface
                                ))
                            }
                        }
                        themeManager.setTheme(theme)
                    }
                },
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Preset Themes
            Text(
                "Preset Themes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            ThemeOptionGrid(
                themes = AppTheme.presets,
                selectedTheme = themeState.currentTheme,
                onThemeSelected = { theme -> themeManager.setTheme(theme) },
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Font Size
            Text(
                "Font Size",
                style = MaterialTheme.typography.titleMedium,
            )
            
            Slider(
                value = themeState.fontScale,
                onValueChange = { themeManager.setFontScale(it) },
                valueRange = 0.8f..2f,
                steps = 6,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Smaller", style = MaterialTheme.typography.bodySmall)
                Text("Larger", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ThemeOptionGrid(
    themes: List<AppTheme>,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        items(themes) { theme ->
            ThemeOption(
                theme = theme,
                isSelected = theme == selectedTheme,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = when (theme) {
        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> DarkColors
        AppTheme.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
        AppTheme.MATERIAL_YOU -> if (isSystemInDarkTheme()) MaterialYouDarkColors else MaterialYouLightColors
        AppTheme.DYNAMIC -> if (isSystemInDarkTheme()) DarkColors else LightColors
        AppTheme.FUTURISTIC -> if (isSystemInDarkTheme()) DarkColors else LightColors
        AppTheme.OCEAN -> if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = oceanDarkColors["primary"] ?: DarkColors.primary,
                onPrimary = oceanDarkColors["onPrimary"] ?: DarkColors.onPrimary,
                primaryContainer = oceanDarkColors["primaryContainer"] ?: DarkColors.primaryContainer,
                onPrimaryContainer = oceanDarkColors["onPrimaryContainer"] ?: DarkColors.onPrimaryContainer,
                secondary = oceanDarkColors["secondary"] ?: DarkColors.secondary,
                onSecondary = oceanDarkColors["onSecondary"] ?: DarkColors.onSecondary,
                secondaryContainer = oceanDarkColors["secondaryContainer"] ?: DarkColors.secondaryContainer,
                onSecondaryContainer = oceanDarkColors["onSecondaryContainer"] ?: DarkColors.onSecondaryContainer,
                tertiary = DarkColors.tertiary,
                onTertiary = DarkColors.onTertiary,
                background = DarkColors.background,
                onBackground = DarkColors.onBackground,
                surface = DarkColors.surface,
                onSurface = DarkColors.onSurface,
                error = DarkColors.error,
                onError = DarkColors.onError
            )
        } else {
            lightColorScheme(
                primary = oceanLightColors["primary"] ?: LightColors.primary,
                onPrimary = oceanLightColors["onPrimary"] ?: LightColors.onPrimary,
                primaryContainer = oceanLightColors["primaryContainer"] ?: LightColors.primaryContainer,
                onPrimaryContainer = oceanLightColors["onPrimaryContainer"] ?: LightColors.onPrimaryContainer,
                secondary = oceanLightColors["secondary"] ?: LightColors.secondary,
                onSecondary = oceanLightColors["onSecondary"] ?: LightColors.onSecondary,
                secondaryContainer = oceanLightColors["secondaryContainer"] ?: LightColors.secondaryContainer,
                onSecondaryContainer = oceanLightColors["onSecondaryContainer"] ?: LightColors.onSecondaryContainer,
                tertiary = LightColors.tertiary,
                onTertiary = LightColors.onTertiary,
                background = LightColors.background,
                onBackground = LightColors.onBackground,
                surface = LightColors.surface,
                onSurface = LightColors.onSurface,
                error = LightColors.error,
                onError = LightColors.onError
            )
        }
        // Add other presets similarly...
        else -> if (isSystemInDarkTheme()) DarkColors else LightColors
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Theme preview
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            // Preview of the theme
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.primary,
                                colors.secondary
                            )
                        )
                    )
            )
            
            // Checkmark if selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Theme name
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
