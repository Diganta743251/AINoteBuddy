package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.model.analytics.TagUsage
import kotlin.math.min
import java.time.LocalDateTime

/**
 * A composable that displays a section for tag usage analytics.
 *
 * @param tags List of tags with their usage counts
 * @param onViewAllTags Callback when "View All" is clicked
 * @param modifier Modifier to be applied to the layout
 * @param maxTags Maximum number of tags to show before showing "View All"
 * @param onTagClick Callback when a tag is clicked
 */
@Composable
fun TagUsageSection(
    tags: List<TagUsage>,
    onViewAllTags: () -> Unit,
    modifier: Modifier = Modifier,
    maxTags: Int = 5,
    onTagClick: (String) -> Unit = {}
) {
    val visibleTags = if (tags.size > maxTags) tags.take(maxTags - 1) else tags
    val remainingTagsCount = tags.size - visibleTags.size
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tag Usage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (tags.size > maxTags) {
                    TextButton(
                        onClick = onViewAllTags,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("View All")
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View All Tags"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (tags.isEmpty()) {
                Text(
                    text = "No tags found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                // Tags list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(visibleTags) { tag ->
                        TagChip(
                            tag = tag,
                            onClick = { onTagClick(tag.tag) }
                        )
                    }
                    
                    if (remainingTagsCount > 0) {
                        item {
                            TagChip(
                                tag = TagUsage("+$remainingTagsCount", 0, lastUsed = LocalDateTime.now()),
                                onClick = onViewAllTags
                            )
                        }
                    }
                }
                
                // Usage distribution (optional: show a simple bar chart for top tags)
                if (visibleTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TagUsageChart(
                        tags = visibleTags,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: TagUsage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "#${tag.tag}",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (tag.count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tag.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun TagUsageChart(
    tags: List<TagUsage>,
    modifier: Modifier = Modifier
) {
    val maxCount = tags.maxOfOrNull { it.count }?.toFloat() ?: 1f
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )
    
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            tags.forEachIndexed { index, tag ->
                val heightRatio = if (maxCount > 0) tag.count.toFloat() / maxCount else 0f
                val animatedHeightRatio by animateFloatAsState(
                    targetValue = heightRatio,
                    label = "tagBarAnimation_${tag.tag}"
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .weight(animatedHeightRatio, fill = false)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(colors[index % colors.size].copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tag.count > 0) {
                            Text(
                                text = tag.count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                    
                    // Label
                    Text(
                        text = "#${tag.tag.take(4)}${if (tag.tag.length > 4) "…" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
