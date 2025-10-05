package com.ainotebuddy.app.ui.dashboard.presets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.ui.components.GlassCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPresetScreen(
    onBackClick: () -> Unit,
    onPresetApplied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presetManager = remember { DashboardPresetManager(context) }
    val scope = rememberCoroutineScope()
    
    var selectedCategory by remember { mutableStateOf<PresetCategory?>(null) }
    var selectedPreset by remember { mutableStateOf<DashboardPresetType?>(null) }
    var currentPreset by remember { mutableStateOf<DashboardPresetType?>(null) }
    var usageStats by remember { mutableStateOf(emptyMap<String, PresetUsageStats>()) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var appliedPreset by remember { mutableStateOf<DashboardPreset?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    // Load current preset and usage stats
    LaunchedEffect(Unit) {
        presetManager.currentPresetFlow.collect { preset ->
            currentPreset = preset
        }
    }
    
    LaunchedEffect(Unit) {
        presetManager.usageStatsFlow.collect { stats ->
            usageStats = stats
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search presets...", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6A82FB),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Dashboard Presets",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { 
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }
                ) {
                    Icon(
                        if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (isSearchActive) "Close search" else "Search",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Preset Status
            if (currentPreset != null && !isSearchActive) {
                item {
                    CurrentPresetCard(
                        preset = currentPreset!!,
                        onCustomize = { /* Navigate to customization */ }
                    )
                }
            }
            
            // Category Filter (only show if not searching)
            if (!isSearchActive) {
                item {
                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }
                
                // Recommended Presets
                item {
                    RecommendedPresetsSection(
                        presetManager = presetManager,
                        onPresetClick = { preset ->
                            selectedPreset = preset
                            showPreviewDialog = true
                        }
                    )
                }
            }
            
            // Preset List
            val presetsToShow = when {
                searchQuery.isNotBlank() -> presetManager.searchPresets(searchQuery)
                selectedCategory != null -> presetManager.getPresetsByCategory(selectedCategory!!)
                else -> presetManager.getAllPresets()
            }
            
            items(presetsToShow) { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = preset.type == currentPreset,
                    usageStats = usageStats[preset.type.id],
                    onClick = {
                        selectedPreset = preset.type
                        showPreviewDialog = true
                    },
                    onQuickApply = {
                        scope.launch {
                            presetManager.applyPreset(preset.type)
                            appliedPreset = preset
                            showSuccessDialog = true
                        }
                    }
                )
            }
            
            // Empty state for search
            if (searchQuery.isNotBlank() && presetsToShow.isEmpty()) {
                item {
                    EmptySearchState(query = searchQuery)
                }
            }
        }
    }
    
    // Preview Dialog
    if (showPreviewDialog && selectedPreset != null) {
        PresetPreviewDialog(
            preset = presetManager.getPresetByType(selectedPreset!!),
            preview = presetManager.getPresetPreview(selectedPreset!!),
            onDismiss = { showPreviewDialog = false },
            onApply = {
                showPreviewDialog = false
                showApplyDialog = true
            },
            onCustomize = {
                showPreviewDialog = false
                // Navigate to customization with preset as base
            }
        )
    }
    
    // Apply Confirmation Dialog
    if (showApplyDialog && selectedPreset != null) {
        ApplyPresetDialog(
            preset = presetManager.getPresetByType(selectedPreset!!),
            onDismiss = { showApplyDialog = false },
            onConfirm = {
                scope.launch {
                    presetManager.applyPreset(selectedPreset!!)
                    appliedPreset = presetManager.getPresetByType(selectedPreset!!)
                    showApplyDialog = false
                    showSuccessDialog = true
                }
            }
        )
    }
    
    // Success Dialog
    if (showSuccessDialog && appliedPreset != null) {
        PresetSuccessDialog(
            preset = appliedPreset!!,
            onDismiss = { 
                showSuccessDialog = false
                onPresetApplied()
            },
            onCustomize = {
                showSuccessDialog = false
                // Navigate to customization with applied preset as base
                onPresetApplied()
            },
            onRatePreset = { rating ->
                scope.launch {
                    presetManager.ratePreset(appliedPreset!!.type, rating)
                }
            }
        )
    }
}

@Composable
fun CurrentPresetCard(
    preset: DashboardPresetType,
    onCustomize: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
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
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Layout",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            OutlinedButton(
                onClick = onCustomize,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("Customize", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedCategory: PresetCategory?,
    onCategorySelected: (PresetCategory?) -> Unit
) {
    Column {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                CategoryChip(
                    category = null,
                    displayName = "All",
                    isSelected = selectedCategory == null,
                    onClick = { onCategorySelected(null) }
                )
            }
            
            items(PresetCategory.values()) { category ->
                CategoryChip(
                    category = category,
                    displayName = category.displayName,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: PresetCategory?,
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        category?.color ?: Color(0xFF6A82FB)
    } else {
        Color.White.copy(alpha = 0.1f)
    }
    
    val contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
    
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = displayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
fun RecommendedPresetsSection(
    presetManager: DashboardPresetManager,
    onPresetClick: (DashboardPresetType) -> Unit
) {
    val recommendedPresets = presetManager.getRecommendedPresets()
    
    if (recommendedPresets.isNotEmpty()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recommended for You",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(recommendedPresets) { preset ->
                    RecommendedPresetCard(
                        preset = preset,
                        onClick = { onPresetClick(preset.type) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedPresetCard(
    preset: DashboardPreset,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                preset.type.icon,
                contentDescription = null,
                tint = preset.type.color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = preset.type.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun PresetCard(
    preset: DashboardPreset,
    isSelected: Boolean,
    usageStats: PresetUsageStats?,
    onClick: () -> Unit,
    onQuickApply: () -> Unit
) {
    val borderColor = if (isSelected) preset.type.color else Color.White.copy(alpha = 0.2f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    preset.type.icon,
                    contentDescription = null,
                    tint = preset.type.color,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Current",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = preset.type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                    
                    // Usage stats
                    if (usageStats != null && usageStats.timesApplied > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Used ${usageStats.timesApplied} times",
                                style = MaterialTheme.typography.labelSmall,
                                color = preset.type.color
                            )
                            
                            if (usageStats.userRating > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f", usageStats.userRating),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFD700)
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (!isSelected) {
                    Button(
                        onClick = onQuickApply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = preset.type.color,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = preset.type.category.color.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = preset.type.category.displayName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = preset.type.category.color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptySearchState(query: String) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.SearchOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No presets found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "No presets match \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PresetPreviewDialog(
    preset: DashboardPreset,
    preview: PresetPreview,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onCustomize: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    preset.type.icon,
                    contentDescription = null,
                    tint = preset.type.color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = preset.type.displayName,
                    color = Color.White
                )
            }
        },
        text = {
            Column {
                Text(
                    text = preset.type.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Preview details
                PreviewDetailsCard(preview = preview)
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = preset.type.color,
                    contentColor = Color.White
                )
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onCustomize
                ) {
                    Text("Customize", color = Color.White.copy(alpha = 0.7f))
                }
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        },
        containerColor = Color(0xFF1a1a2e),
        tonalElevation = 8.dp
    )
}

@Composable
fun PreviewDetailsCard(preview: PresetPreview) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Setup time estimate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Setup Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = preview.estimatedSetupTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Widgets section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Widgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "${preview.enabledWidgets} enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (preview.widgetTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(preview.widgetTypes.take(3)) { widgetType ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF6A82FB).copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = widgetType,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6A82FB),
                                maxLines = 1
                            )
                        }
                    }
                    if (preview.widgetTypes.size > 3) {
                        item {
                            Text(
                                text = "+${preview.widgetTypes.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // FAB section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FAB Style",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = preview.fabStyle.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "${preview.enabledActions} enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (preview.actionTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(preview.actionTypes.take(4)) { actionType ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE91E63).copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = actionType,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE91E63),
                                maxLines = 1
                            )
                        }
                    }
                    if (preview.actionTypes.size > 4) {
                        item {
                            Text(
                                text = "+${preview.actionTypes.size - 4}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplyPresetDialog(
    preset: DashboardPreset,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Apply Preset",
                color = Color.White
            )
        },
        text = {
            Text(
                text = "This will replace your current dashboard configuration with the \"${preset.type.displayName}\" preset. Your current settings will be lost.",
                color = Color.White.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = preset.type.color,
                    contentColor = Color.White
                )
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF1a1a2e),
        tonalElevation = 8.dp
    )
}