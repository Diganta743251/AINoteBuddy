package com.ainotebuddy.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A data class that holds spacing values used throughout the app.
 * These values follow the 8dp grid system for consistent spacing.
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val extraExtraLarge: Dp = 48.dp,
    
    // Specific spacing values for common use cases
    val default: Dp = medium,
    val divider: Dp = 1.dp,
    val buttonHeight: Dp = 48.dp,
    val appBarElevation: Dp = 4.dp,
    val cardElevation: Dp = 2.dp,
    val fabElevation: Dp = 6.dp,
    val dialogElevation: Dp = 24.dp,
    
    // Content padding
    val contentPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val buttonPadding: Dp = 16.dp,
    val iconPadding: Dp = 8.dp,
    
    // Border widths
    val borderWidth: Dp = 1.dp,
    val dividerWidth: Dp = 1.dp,
    
    // Corner radius
    val smallCornerRadius: Dp = 4.dp,
    val mediumCornerRadius: Dp = 8.dp,
    val largeCornerRadius: Dp = 12.dp,
    val extraLargeCornerRadius: Dp = 16.dp,
    
    // Icon sizes
    val iconSizeSmall: Dp = 16.dp,
    val iconSizeMedium: Dp = 24.dp,
    val iconSizeLarge: Dp = 32.dp,
    
    // Avatar sizes
    val avatarSizeSmall: Dp = 32.dp,
    val avatarSizeMedium: Dp = 48.dp,
    val avatarSizeLarge: Dp = 64.dp,
    
    // List item heights
    val listItemHeightSmall: Dp = 48.dp,
    val listItemHeightMedium: Dp = 56.dp,
    val listItemHeightLarge: Dp = 72.dp
)

/**
 * Default spacing values that follow the 8dp grid system.
 */
val defaultSpacing = Spacing()

/**
 * CompositionLocal that provides the current [Spacing] values.
 */
val LocalSpacing = staticCompositionLocalOf { defaultSpacing }

/**
 * Returns the current [Spacing] at the call site's position in the hierarchy.
 */
@Composable
fun spacing(): Spacing = LocalSpacing.current
