package com.ainotebuddy.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Screen size breakpoints for responsive design
enum class ScreenSize {
    COMPACT,    // < 600dp width (phones)
    MEDIUM,     // 600-840dp width (tablets, foldables)
    EXPANDED    // > 840dp width (large tablets, desktop)
}

// Window size classes following Material Design guidelines
data class WindowSizeClass(
    val widthSizeClass: ScreenSize,
    val heightSizeClass: ScreenSize
)

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val widthDp = with(density) { configuration.screenWidthDp.dp }
    val heightDp = with(density) { configuration.screenHeightDp.dp }
    
    val widthSizeClass = when {
        widthDp < 600.dp -> ScreenSize.COMPACT
        widthDp < 840.dp -> ScreenSize.MEDIUM
        else -> ScreenSize.EXPANDED
    }
    
    val heightSizeClass = when {
        heightDp < 480.dp -> ScreenSize.COMPACT
        heightDp < 900.dp -> ScreenSize.MEDIUM
        else -> ScreenSize.EXPANDED
    }
    
    return WindowSizeClass(widthSizeClass, heightSizeClass)
}

@Composable
fun ResponsiveLayout(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    compactContent: @Composable () -> Unit,
    mediumContent: @Composable () -> Unit = compactContent,
    expandedContent: @Composable () -> Unit = mediumContent
) {
    when (windowSizeClass.widthSizeClass) {
        ScreenSize.COMPACT -> compactContent()
        ScreenSize.MEDIUM -> mediumContent()
        ScreenSize.EXPANDED -> expandedContent()
    }
}

@Composable
fun AdaptiveGrid(
    items: List<Any>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Any) -> Unit
) {
    val columns = when (windowSizeClass.widthSizeClass) {
        ScreenSize.COMPACT -> 1
        ScreenSize.MEDIUM -> 2
        ScreenSize.EXPANDED -> 3
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            itemContent(item)
        }
    }
}

@Composable
fun AdaptiveRow(
    items: List<Any>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Any) -> Unit
) {
    when (windowSizeClass.widthSizeClass) {
        ScreenSize.COMPACT -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    itemContent(item)
                }
            }
        }
        ScreenSize.MEDIUM, ScreenSize.EXPANDED -> {
            LazyRow(
                modifier = modifier,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    itemContent(item)
                }
            }
        }
    }
}

@Composable
fun ResponsivePadding(
    windowSizeClass: WindowSizeClass
): PaddingValues {
    return when (windowSizeClass.widthSizeClass) {
        ScreenSize.COMPACT -> PaddingValues(16.dp)
        ScreenSize.MEDIUM -> PaddingValues(24.dp)
        ScreenSize.EXPANDED -> PaddingValues(32.dp)
    }
}

@Composable
fun ResponsiveSpacing(
    windowSizeClass: WindowSizeClass
): Dp {
    return when (windowSizeClass.widthSizeClass) {
        ScreenSize.COMPACT -> 8.dp
        ScreenSize.MEDIUM -> 12.dp
        ScreenSize.EXPANDED -> 16.dp
    }
}

// Adaptive navigation based on screen size
@Composable
fun AdaptiveNavigation(
    windowSizeClass: WindowSizeClass,
    currentDestination: String,
    onNavigate: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    when (windowSizeClass.widthSizeClass) {
        ScreenSize.COMPACT -> {
            // Bottom navigation for phones
            ModernBottomNavigation(
                currentDestination = currentDestination,
                onNavigate = onNavigate,
                modifier = modifier
            )
        }
        ScreenSize.MEDIUM -> {
            // Navigation rail for tablets
            NavigationRail(
                modifier = modifier
            ) {
                val destinations = listOf(
                    NavigationDestination.Dashboard,
                    NavigationDestination.Notes,
                    NavigationDestination.Search,
                    NavigationDestination.AI,
                    NavigationDestination.Settings
                )
                
                destinations.forEach { destination ->
                    NavigationRailItem(
                        selected = currentDestination == destination.route,
                        onClick = { onNavigate(destination) },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == destination.route) {
                                    destination.selectedIcon
                                } else {
                                    destination.icon
                                },
                                contentDescription = destination.title
                            )
                        },
                        label = { Text(destination.title) }
                    )
                }
            }
        }
        ScreenSize.EXPANDED -> {
            // Navigation drawer for large screens
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(240.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "AI NoteBuddy",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            val destinations = listOf(
                                NavigationDestination.Dashboard,
                                NavigationDestination.Notes,
                                NavigationDestination.Search,
                                NavigationDestination.AI,
                                NavigationDestination.Settings
                            )
                            
                            destinations.forEach { destination ->
                                NavigationDrawerItem(
                                    selected = currentDestination == destination.route,
                                    onClick = { onNavigate(destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentDestination == destination.route) {
                                                destination.selectedIcon
                                            } else {
                                                destination.icon
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(destination.title) }
                                )
                            }
                        }
                    }
                },
                modifier = modifier
            ) {
                // Main content area
            }
        }
    }
}

// Content-aware spacing that adapts to content density
@Composable
fun ContentAwareSpacing(
    contentDensity: ContentDensity,
    windowSizeClass: WindowSizeClass
): Dp {
    val baseSpacing = ResponsiveSpacing(windowSizeClass)
    
    return when (contentDensity) {
        ContentDensity.LOW -> baseSpacing * 1.5f
        ContentDensity.MEDIUM -> baseSpacing
        ContentDensity.HIGH -> baseSpacing * 0.75f
    }
}

enum class ContentDensity {
    LOW, MEDIUM, HIGH
}

// Edge-to-edge design with proper insets
@Composable
fun EdgeToEdgeLayout(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        content(paddingValues)
    }
}
