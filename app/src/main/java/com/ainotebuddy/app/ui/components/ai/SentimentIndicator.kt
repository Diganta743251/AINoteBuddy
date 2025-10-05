package com.ainotebuddy.app.ui.components.ai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.ainotebuddy.app.ai.SentimentAnalysisResult
import com.ainotebuddy.app.ai.Sentiment
import com.ainotebuddy.app.ui.theme.LocalSpacing
import kotlin.math.roundToInt

/**
 * Displays a sentiment indicator with an icon and optional confidence level
 */
@Composable
fun SentimentIndicator(
    sentiment: Sentiment,
    confidence: Float = 0f,
    showConfidence: Boolean = true,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (sentiment) {
        Sentiment.POSITIVE -> Icons.Default.Mood to Color(0xFF4CAF50)
        Sentiment.NEUTRAL -> Icons.Default.SentimentNeutral to Color(0xFF9E9E9E)
        Sentiment.NEGATIVE -> Icons.Default.MoodBad to Color(0xFFF44336)
    }
    
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(durationMillis = 800)
    )
    
    val spacing = LocalSpacing.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Sentiment icon with pulse animation for high confidence
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "${sentiment.toString().lowercase()} sentiment",
                tint = tint,
                modifier = Modifier.size(size * 0.6f)
            )
            
            // Animated confidence ring
            if (showConfidence && confidence > 0) {
                CircularProgressIndicator(
                    progress = { animatedConfidence },
                    color = tint,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
        }
        
        // Confidence percentage
        if (showConfidence && confidence > 0) {
            Spacer(modifier = Modifier.width(spacing.extraSmall))
            Text(
                text = "${(confidence * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Displays a sentiment analysis result with icon and key phrases
 */
@Composable
fun SentimentAnalysisResultView(
    result: SentimentAnalysisResult,
    modifier: Modifier = Modifier,
    showPhrases: Boolean = true,
    showConfidence: Boolean = true
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(spacing.small)
    ) {
        // Header with sentiment and confidence
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sentiment: ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            SentimentIndicator(
                sentiment = result.sentiment,
                confidence = if (showConfidence) result.confidence else 0f,
                showConfidence = showConfidence,
                size = 20.dp
            )
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            Text(
                text = result.sentiment.toString().lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Key phrases
        if (showPhrases && result.keyPhrases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = "Key phrases: ${result.keyPhrases.take(3).joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(device = Devices.PIXEL_4)
@Composable
private fun SentimentIndicatorPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Individual indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SentimentIndicator(
                    sentiment = Sentiment.POSITIVE,
                    confidence = 0.85f
                )
                
                SentimentIndicator(
                    sentiment = Sentiment.NEUTRAL,
                    confidence = 0.6f
                )
                
                SentimentIndicator(
                    sentiment = Sentiment.NEGATIVE,
                    confidence = 0.75f
                )
                
                // Example fallback using NEUTRAL for mixed sentiment scenario
                SentimentIndicator(
                    sentiment = Sentiment.NEUTRAL,
                    confidence = 0.5f
                )
            }
            
            // Full result view
            SentimentAnalysisResultView(
                result = SentimentAnalysisResult(
                    sentiment = Sentiment.POSITIVE,
                    confidence = 0.85f,
                    keyPhrases = listOf("great progress", "well done", "excellent work"),
                    emotions = mapOf(
                        "joy" to 0.8f,
                        "trust" to 0.7f
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
