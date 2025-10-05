package com.ainotebuddy.app.ui.dashboard

import android.annotation.TargetApi
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.ainotebuddy.app.ui.components.BannerAdView

// Stub functions for missing functionality
fun toggleFavorite(noteId: Long, viewModel: NoteViewModel) {
    viewModel.toggleFavorite(noteId)
}

fun togglePin(noteId: Long, viewModel: NoteViewModel) {
    viewModel.togglePin(noteId)
}

@Composable
fun DashboardScreen(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    onNewNote: () -> Unit,
    onSearch: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTemplates: () -> Unit = {},
    onCategories: () -> Unit = {},
    onShowAllNotes: () -> Unit = {},
    onShowFavorites: () -> Unit = {},
    onShowPinned: () -> Unit = {},
    onShowVault: () -> Unit = {}
) {
    val notes by viewModel.notes.collectAsState()
    val favoriteNotes = remember(notes) { notes.filter { it.isFavorite } }
    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val vaultNotes = remember(notes) { notes.filter { it.isInVault } }
    val context = LocalContext.current
    var vaultUnlocked by remember { mutableStateOf(false) }

    val themeState = remember { mutableStateOf("light") }
    val isDarkTheme = themeState.value == "dark"

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedGradientBackdrop()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            item {
                DashboardHeader(
                    onSearch = onSearch,
                    onNewNote = onNewNote
                )
            }

            // Quick Stats Cards (clickable)
            item {
                QuickStatsSection(
                    notes = notes,
                    favoriteNotes = favoriteNotes,
                    pinnedNotes = pinnedNotes,
                    vaultNotes = vaultNotes,
                    onTotalClick = onShowAllNotes,
                    onFavoritesClick = onShowFavorites,
                    onPinnedClick = onShowPinned,
                    onVaultClick = onShowVault
                )
            }

            // Quick Actions
            item {
                QuickActionsSection(
                    onTemplates = onTemplates,
                    onCategories = onCategories
                )
            }

            // Recent Notes (Max 10)
            item {
                SectionHeader(
                    title = "Recent Notes",
                    subtitle = "Your latest thoughts",
                    icon = Icons.Filled.Note,
                    color = Color(0xFF6A82FB)
                )
            }
            items(notes.take(10).ifEmpty { listOf() }) { note ->
                CompactNoteCard(
                    note = note,
                    onClick = { onNoteClick(note) },
                    onFavorite = { viewModel.toggleFavorite(note.id) },
                    onPin = { viewModel.togglePin(note.id) }
                )
            }
            
            // Banner Ad
            item {
                BannerAdView(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedGradientBackdrop() {
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

@Composable
fun DashboardHeader(
    onSearch: () -> Unit,
    onNewNote: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Welcome back! 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ready to capture your thoughts?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                
                IconButton(
                    onClick = onSearch,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onNewNote,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF667eea)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Note", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickStatsSection(
    notes: List<NoteEntity>,
    favoriteNotes: List<NoteEntity>,
    pinnedNotes: List<NoteEntity>,
    vaultNotes: List<NoteEntity>,
    onTotalClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onPinnedClick: () -> Unit,
    onVaultClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                title = "Total Notes",
                value = notes.size.toString(),
                icon = Icons.Filled.Note,
                color = Color(0xFF6A82FB),
                onClick = onTotalClick
            )
        }
        item {
            StatCard(
                title = "Favorites",
                value = favoriteNotes.size.toString(),
                icon = Icons.Filled.Favorite,
                color = Color(0xFFFC5C7D),
                onClick = onFavoritesClick
            )
        }
        item {
            StatCard(
                title = "Pinned",
                value = pinnedNotes.size.toString(),
                icon = Icons.Filled.Star,
                color = Color(0xFFFFD700),
                onClick = onPinnedClick
            )
        }
        item {
            StatCard(
                title = "In Vault",
                value = vaultNotes.size.toString(),
                icon = Icons.Filled.Lock,
                color = Color(0xFF00FFC6),
                onClick = onVaultClick
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onTemplates: () -> Unit,
    onCategories: () -> Unit
) {
    GlassCard {
        Column {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "Templates",
                    icon = Icons.Filled.Description,
                    color = Color(0xFF00FFC6),
                    onClick = onTemplates,
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    title = "Categories",
                    icon = Icons.Filled.Folder,
                    color = Color(0xFFFFD700),
                    onClick = onCategories,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PinnedNotesGrid(
    notes: List<NoteEntity>,
    onNoteClick: (NoteEntity) -> Unit,
    viewModel: NoteViewModel
) {
    // Use regular Column and Row instead of LazyVerticalGrid to avoid nesting scrollable components
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        notes.chunked(2).forEach { rowNotes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowNotes.forEach { note ->
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumNoteCard(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onFavorite = { toggleFavorite(note.id, viewModel) },
                            onPin = { togglePin(note.id, viewModel) },
                            onMoveToVault = { /* TODO: implement vault move */ },
                            isCompact = true
                        )
                    }
                }
                // Fill remaining space if odd number of notes in row
                if (rowNotes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CompactNoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Note content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isFavorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFC5C7D),
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (note.isPinned) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (note.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (note.isFavorite) Color(0xFFFC5C7D) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onPin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (note.isPinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumNoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit,
    onMoveToVault: () -> Unit,
    isCompact: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    if (note.isFavorite) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFC5C7D),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (note.isPinned) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            if (!isCompact) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (note.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) Color(0xFFFC5C7D) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onPin,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (note.isPinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@TargetApi(Build.VERSION_CODES.N)
@Composable
fun VaultSection(
    vaultNotes: List<NoteEntity>,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    viewModel: NoteViewModel
) {
    GlassCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00FFC6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Secure Vault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${vaultNotes.size} secure notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                if (!isUnlocked) {
                    Button(
                        onClick = onUnlock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFC6),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (isUnlocked && vaultNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                vaultNotes.forEach { note ->
                    PremiumNoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onFavorite = { toggleFavorite(note.id, viewModel) },
                        onPin = { togglePin(note.id, viewModel) },
                        onMoveToVault = { /* TODO: implement vault move */ },
                        isCompact = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CategoriesCardSection(
    notes: List<NoteEntity>,
    onCategoryClick: (String) -> Unit
) {
    val categories = notes.map { it.category }.distinct().filter { it.isNotBlank() }
    
    if (categories.isNotEmpty()) {
        GlassCard {
            Column {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categories.chunked(2).forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowCategories.forEach { category ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val noteCount = notes.count { it.category == category }
                                    CategoryCard(
                                        name = category,
                                        count = noteCount,
                                        onClick = { onCategoryClick(category) }
                                    )
                                }
                            }
                            if (rowCategories.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    name: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = Color(0xFF6A82FB),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$count notes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            content()
        }
    }
}

private fun authenticateVault(context: android.content.Context, onSuccess: () -> Unit) {
    val activity = context as? androidx.fragment.app.FragmentActivity ?: return
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Vault")
        .setSubtitle("Authenticate to access your secure notes")
        .setNegativeButtonText("Cancel")
        .build()
    
    val biometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
    )
    biometricPrompt.authenticate(promptInfo)
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
} 