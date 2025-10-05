package com.ainotebuddy.app.offline.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.offline.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI components for displaying sync status and offline-first functionality
 * Provides real-time feedback on network state, sync progress, and operation status
 */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SyncStatusIndicator(
    networkState: NetworkState,
    queueStatistics: QueueStatistics,
    modifier: Modifier = Modifier
) {
    val syncStatus = when {
        !networkState.isConnected -> SyncIndicatorStatus.OFFLINE
        queueStatistics.pendingOperations > 0 -> SyncIndicatorStatus.SYNCING
        queueStatistics.failedOperations > 0 -> SyncIndicatorStatus.ERROR
        queueStatistics.conflictsToResolve > 0 -> SyncIndicatorStatus.CONFLICTS
        else -> SyncIndicatorStatus.SYNCED
    }
    
    Row(
        modifier = modifier
            .background(
                color = syncStatus.backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Status icon with animation
        AnimatedContent(
            targetState = syncStatus,
            transitionSpec = {
                fadeIn() with fadeOut()
            }
        ) { status ->
            Icon(
                imageVector = status.icon,
                contentDescription = status.description,
                tint = status.iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Status text
        Text(
            text = syncStatus.getStatusText(queueStatistics),
            color = syncStatus.textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        
        // Sync progress indicator
        if (syncStatus == SyncIndicatorStatus.SYNCING) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = syncStatus.iconColor
            )
        }
    }
}

@Composable
fun NetworkStatusBadge(
    networkState: NetworkState,
    modifier: Modifier = Modifier
) {
    val connectionInfo = when {
        !networkState.isConnected -> ConnectionInfo("Offline", Icons.Default.WifiOff, Color.Red)
        networkState.connectionType == ConnectionType.WIFI -> ConnectionInfo("WiFi", Icons.Default.Wifi, Color.Green)
        networkState.connectionType == ConnectionType.MOBILE_DATA -> ConnectionInfo("Mobile", Icons.Default.SignalCellularAlt, Color(0xFFFF9800))
        networkState.connectionType == ConnectionType.ETHERNET -> ConnectionInfo("Ethernet", Icons.Default.Cable, Color.Blue)
        else -> ConnectionInfo("Connected", Icons.Default.NetworkCheck, Color.Gray)
    }
    
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = connectionInfo.color.copy(alpha = 0.1f),
        contentColor = connectionInfo.color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = connectionInfo.icon,
                contentDescription = connectionInfo.label,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = connectionInfo.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SyncProgressCard(
    queueStatistics: QueueStatistics,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onViewDetails) {
                    Text("View Details")
                }
            }
            
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SyncStatItem(
                    label = "Pending",
                    value = queueStatistics.pendingOperations,
                    color = MaterialTheme.colorScheme.primary
                )
                SyncStatItem(
                    label = "Failed",
                    value = queueStatistics.failedOperations,
                    color = MaterialTheme.colorScheme.error
                )
                SyncStatItem(
                    label = "Conflicts",
                    value = queueStatistics.conflictsToResolve,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            // Data size indicator
            if (queueStatistics.pendingDataSize > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Data size",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pending data: ${formatDataSize(queueStatistics.pendingDataSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun OperationStatusList(
    operations: List<OfflineOperationEntity>,
    onRetryOperation: (String) -> Unit,
    onCancelOperation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(operations) { operation ->
            OperationStatusItem(
                operation = operation,
                onRetry = { onRetryOperation(operation.id) },
                onCancel = { onCancelOperation(operation.id) }
            )
        }
    }
}

@Composable
fun OperationStatusItem(
    operation: OfflineOperationEntity,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val operationInfo = getOperationInfo(operation)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Operation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = operationInfo.icon,
                        contentDescription = operationInfo.type,
                        modifier = Modifier.size(20.dp),
                        tint = operationInfo.color
                    )
                    Column {
                        Text(
                            text = operationInfo.type,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = operationInfo.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getStatusColor(operation.status).copy(alpha = 0.1f),
                    contentColor = getStatusColor(operation.status)
                ) {
                    Text(
                        text = operation.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Operation details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Priority: ${getPriorityText(operation.priority)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (operation.retryCount > 0) {
                        Text(
                            text = "Retries: ${operation.retryCount}/${operation.maxRetries}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Action buttons
                if (operation.status == "FAILED" && operation.retryCount < operation.maxRetries) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Error message
            if (operation.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = operation.errorMessage,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStatItem(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions and data classes
private enum class SyncIndicatorStatus(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val textColor: Color,
    val description: String
) {
    SYNCED(Icons.Default.CloudDone, Color.Green, Color.Green.copy(alpha = 0.1f), Color.Green, "Synced"),
    SYNCING(Icons.Default.CloudSync, Color.Blue, Color.Blue.copy(alpha = 0.1f), Color.Blue, "Syncing"),
    OFFLINE(Icons.Default.CloudOff, Color.Gray, Color.Gray.copy(alpha = 0.1f), Color.Gray, "Offline"),
    ERROR(Icons.Default.ErrorOutline, Color.Red, Color.Red.copy(alpha = 0.1f), Color.Red, "Error"),
    CONFLICTS(Icons.Default.Warning, Color(0xFFFF9800), Color(0xFFFF9800).copy(alpha = 0.1f), Color(0xFFFF9800), "Conflicts");
    
    fun getStatusText(stats: QueueStatistics): String = when (this) {
        SYNCED -> "All synced"
        SYNCING -> "${stats.pendingOperations} pending"
        OFFLINE -> "Offline mode"
        ERROR -> "${stats.failedOperations} failed"
        CONFLICTS -> "${stats.conflictsToResolve} conflicts"
    }
}

private data class ConnectionInfo(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

private data class OperationInfo(
    val type: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

private fun getOperationInfo(operation: OfflineOperationEntity): OperationInfo {
    return when (operation.type) {
        "CREATE_NOTE" -> OperationInfo("Create Note", "Creating new note", Icons.Default.Add, Color.Green)
        "UPDATE_NOTE" -> OperationInfo("Update Note", "Updating note content", Icons.Default.Edit, Color.Blue)
        "DELETE_NOTE" -> OperationInfo("Delete Note", "Deleting note", Icons.Default.Delete, Color.Red)
        "CREATE_CATEGORY" -> OperationInfo("Create Category", "Creating new category", Icons.Default.Folder, Color(0xFF9C27B0))
        "AI_ANALYSIS" -> OperationInfo("AI Analysis", "Processing with AI", Icons.Default.Psychology, Color(0xFFFF9800))
        "SYNC_COLLABORATIVE_SESSION" -> OperationInfo("Sync Session", "Syncing collaboration", Icons.Default.Group, Color.Cyan)
        else -> OperationInfo("Unknown", "Unknown operation", Icons.Default.HelpOutline, Color.Gray)
    }
}

private fun getStatusColor(status: String): Color = when (status) {
    "PENDING" -> Color.Blue
    "PROCESSING" -> Color(0xFFFF9800)
    "SUCCESS" -> Color.Green
    "FAILED" -> Color.Red
    "CANCELLED" -> Color.Gray
    "RETRYING" -> Color.Yellow
    else -> Color.Gray
}

private fun getPriorityText(priority: Int): String = when (priority) {
    1 -> "High"
    2 -> "Medium"
    3 -> "Low"
    else -> "Unknown"
}

private fun formatDataSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
