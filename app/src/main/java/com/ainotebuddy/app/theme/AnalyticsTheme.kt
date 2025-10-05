package com.ainotebuddy.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow

/**
 * Analytics-specific theme configuration
 */
object AnalyticsTheme {
    // Primary colors
    val PrimaryLight = Color(0xFF4A6572)
    val PrimaryDark = Color(0xFF344955)
    val Secondary = Color(0xFFF9AA33)
    val BackgroundLight = Color(0xFFF5F5F5)
    val BackgroundDark = Color(0xFF1A1A1A)
    val SurfaceLight = Color.White
    val SurfaceDark = Color(0xFF2D2D2D)
    val Error = Color(0xFFB00020)
    val OnPrimary = Color.White
    val OnSecondary = Color.Black
    val OnBackground = Color.Black
    val OnSurface = Color.Black
    val OnError = Color.White

    // Chart colors
    val ChartColors = listOf(
        Color(0xFF4E79A7), // Blue
        Color(0xFFF28E2B), // Orange
        Color(0xFFE15759), // Red
        Color(0xFF76B7B2), // Teal
        Color(0xFF59A14F), // Green
        Color(0xFFEDC948), // Yellow
        Color(0xFFB07AA1), // Purple
        Color(0xFFFF9DA7), // Pink
        Color(0xFF9C755F), // Brown
        Color(0xFFBAB0AC)  // Gray
    )

    // Time range selector colors
    val TimeRangeSelectedLight = PrimaryLight
    val TimeRangeUnselectedLight = Color.LightGray
    val TimeRangeTextSelected = OnPrimary
    val TimeRangeTextUnselected = OnSurface

    // Card colors
    val CardBackgroundLight = SurfaceLight
    val CardBackgroundDark = SurfaceDark
    val CardElevation = 4.dp
    val CardShape = RoundedCornerShape(12.dp)

    // Spacing
    val PaddingSmall = 8.dp
    val PaddingMedium = 16.dp
    val PaddingLarge = 24.dp
    val ChartBarSpacing = 4.dp
    val ChartBarWidth = 24.dp

    // Typography
    val ChartAxisTextStyle = TextStyle(
        color = Color.Gray,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal
    )

    val ChartLabelTextStyle = TextStyle(
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )

    val StatValueTextStyle: TextStyle
        @Composable get() = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

    val StatLabelTextStyle: TextStyle
        @Composable get() = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
}

/**
 * Custom theme for analytics screens
 */
@Composable
fun AnalyticsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AnalyticsTheme.PrimaryDark,
            secondary = AnalyticsTheme.Secondary,
            background = AnalyticsTheme.BackgroundDark,
            surface = AnalyticsTheme.SurfaceDark,
            onPrimary = AnalyticsTheme.OnPrimary,
            onSecondary = AnalyticsTheme.OnSecondary,
            onBackground = AnalyticsTheme.OnBackground,
            onSurface = AnalyticsTheme.OnSurface,
            error = AnalyticsTheme.Error,
            onError = AnalyticsTheme.OnError
        )
    } else {
        lightColorScheme(
            primary = AnalyticsTheme.PrimaryLight,
            secondary = AnalyticsTheme.Secondary,
            background = AnalyticsTheme.BackgroundLight,
            surface = AnalyticsTheme.SurfaceLight,
            onPrimary = AnalyticsTheme.OnPrimary,
            onSecondary = AnalyticsTheme.OnSecondary,
            onBackground = AnalyticsTheme.OnBackground,
            onSurface = AnalyticsTheme.OnSurface,
            error = AnalyticsTheme.Error,
            onError = AnalyticsTheme.OnError
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes.copy(
            small = AnalyticsTheme.CardShape,
            medium = AnalyticsTheme.CardShape,
            large = AnalyticsTheme.CardShape
        ),
        content = content
    )
}

/**
 * Preview composable for light theme
 */
@Preview(showBackground = true)
@Composable
fun AnalyticsLightThemePreview() {
    AnalyticsTheme(darkTheme = false) {
        // Add preview content here
    }
}

/**
 * Preview composable for dark theme
 */
@Preview(showBackground = true)
@Composable
fun AnalyticsDarkThemePreview() {
    AnalyticsTheme(darkTheme = true) {
        // Add preview content here
    }
}

/**
 * Extension function to get chart color by index with cycling
 */
fun getChartColor(index: Int): Color {
    return AnalyticsTheme.ChartColors[index % AnalyticsTheme.ChartColors.size]
}

/**
 * Extension function to get contrast color for text on colored backgrounds
 */
fun Color.contrastText(): Color {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return if (luminance > 0.5) Color.Black else Color.White
}

/**
 * Extension function to apply analytics card styling
 */
@Composable
fun Modifier.analyticsCard(): Modifier = this
    .background(
        color = MaterialTheme.colorScheme.surface,
        shape = AnalyticsTheme.CardShape
    )
    .padding(AnalyticsTheme.PaddingMedium)
    .shadow(
        elevation = AnalyticsTheme.CardElevation,
        shape = AnalyticsTheme.CardShape,
        clip = true
    )

/**
 * Extension function to apply section header styling
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = AnalyticsTheme.PaddingSmall)
    )
}

/**
 * Extension function to apply stat value styling
 */
@Composable
fun StatValue(
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = value,
        style = AnalyticsTheme.StatValueTextStyle,
        modifier = modifier
    )
}

/**
 * Extension function to apply stat label styling
 */
@Composable
fun StatLabel(
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = AnalyticsTheme.StatLabelTextStyle,
        modifier = modifier
    )
}
