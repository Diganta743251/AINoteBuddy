package com.ainotebuddy.app.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlin.math.*

data class CanvasNode(
    val id: String = java.util.UUID.randomUUID().toString(),
    var position: Offset,
    var text: String,
    var color: Color = Color.Blue,
    var size: Size = Size(120f, 80f),
    var isSelected: Boolean = false
)

data class CanvasConnection(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fromNodeId: String,
    val toNodeId: String,
    val color: Color = Color.Gray,
    val strokeWidth: Float = 2f
)

data class CanvasState(
    val nodes: List<CanvasNode> = emptyList(),
    val connections: List<CanvasConnection> = emptyList(),
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfiniteCanvasScreen(
    onBack: () -> Unit,
    onSave: (CanvasState) -> Unit
) {
    var canvasState by remember { mutableStateOf(CanvasState()) }
    var selectedTool by remember { mutableStateOf(CanvasTool.SELECT) }
    var showNodeDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<CanvasNode?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStartNode by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Mind Map Canvas") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = { onSave(canvasState) }) {
                    Icon(Icons.Filled.Save, "Save")
                }
            }
        )
        
        // Tool Bar
        CanvasToolBar(
            selectedTool = selectedTool,
            onToolSelected = { tool ->
                selectedTool = tool
                if (tool != CanvasTool.CONNECT) {
                    isConnecting = false
                    connectionStartNode = null
                }
            },
            onAddNode = {
                val newNode = CanvasNode(
                    position = Offset(200f, 200f),
                    text = "New Node"
                )
                canvasState = canvasState.copy(
                    nodes = canvasState.nodes + newNode
                )
            }
        )
        
        // Canvas
        Box(modifier = Modifier.weight(1f)) {
            InfiniteCanvas(
                state = canvasState,
                selectedTool = selectedTool,
                isConnecting = isConnecting,
                connectionStartNode = connectionStartNode,
                onStateChange = { canvasState = it },
                onNodeDoubleClick = { node ->
                    editingNode = node
                    showNodeDialog = true
                },
                onNodeClick = { node ->
                    when (selectedTool) {
                        CanvasTool.CONNECT -> {
                            if (connectionStartNode == null) {
                                connectionStartNode = node.id
                                isConnecting = true
                            } else if (connectionStartNode != node.id) {
                                val newConnection = CanvasConnection(
                                    fromNodeId = connectionStartNode!!,
                                    toNodeId = node.id
                                )
                                canvasState = canvasState.copy(
                                    connections = canvasState.connections + newConnection
                                )
                                connectionStartNode = null
                                isConnecting = false
                            }
                        }
                        CanvasTool.DELETE -> {
                            canvasState = canvasState.copy(
                                nodes = canvasState.nodes.filter { it.id != node.id },
                                connections = canvasState.connections.filter { 
                                    it.fromNodeId != node.id && it.toNodeId != node.id 
                                }
                            )
                        }
                        else -> {
                            // Select node
                            canvasState = canvasState.copy(
                                nodes = canvasState.nodes.map { 
                                    it.copy(isSelected = it.id == node.id) 
                                }
                            )
                        }
                    }
                }
            )
        }
    }
    
    // Node Edit Dialog
    if (showNodeDialog && editingNode != null) {
        NodeEditDialog(
            node = editingNode!!,
            onDismiss = { 
                showNodeDialog = false
                editingNode = null
            },
            onSave = { updatedNode ->
                canvasState = canvasState.copy(
                    nodes = canvasState.nodes.map { 
                        if (it.id == updatedNode.id) updatedNode else it 
                    }
                )
                showNodeDialog = false
                editingNode = null
            }
        )
    }
}

enum class CanvasTool {
    SELECT, ADD_NODE, CONNECT, DELETE, PAN
}

@Composable
fun CanvasToolBar(
    selectedTool: CanvasTool,
    onToolSelected: (CanvasTool) -> Unit,
    onAddNode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolButton(
            icon = Icons.Filled.NearMe,
            label = "Select",
            isSelected = selectedTool == CanvasTool.SELECT,
            onClick = { onToolSelected(CanvasTool.SELECT) }
        )
        
        ToolButton(
            icon = Icons.Filled.Add,
            label = "Add Node",
            isSelected = selectedTool == CanvasTool.ADD_NODE,
            onClick = { 
                onToolSelected(CanvasTool.ADD_NODE)
                onAddNode()
            }
        )
        
        ToolButton(
            icon = Icons.Filled.Link,
            label = "Connect",
            isSelected = selectedTool == CanvasTool.CONNECT,
            onClick = { onToolSelected(CanvasTool.CONNECT) }
        )
        
        ToolButton(
            icon = Icons.Filled.Delete,
            label = "Delete",
            isSelected = selectedTool == CanvasTool.DELETE,
            onClick = { onToolSelected(CanvasTool.DELETE) }
        )
        
        ToolButton(
            icon = Icons.Filled.PanTool,
            label = "Pan",
            isSelected = selectedTool == CanvasTool.PAN,
            onClick = { onToolSelected(CanvasTool.PAN) }
        )
    }
}

@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Icon(
            icon, 
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun InfiniteCanvas(
    state: CanvasState,
    selectedTool: CanvasTool,
    isConnecting: Boolean,
    connectionStartNode: String?,
    onStateChange: (CanvasState) -> Unit,
    onNodeClick: (CanvasNode) -> Unit,
    onNodeDoubleClick: (CanvasNode) -> Unit
) {
    var draggedNode by remember { mutableStateOf<String?>(null) }
    var lastClickTime by remember { mutableStateOf(0L) }
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectedTool) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (selectedTool == CanvasTool.PAN) {
                        onStateChange(
                            state.copy(
                                offset = state.offset + pan,
                                scale = (state.scale * zoom).coerceIn(0.1f, 5f)
                            )
                        )
                    }
                }
            }
            .pointerInput(selectedTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val adjustedOffset = (offset - state.offset) / state.scale
                        val clickedNode = state.nodes.find { node ->
                            adjustedOffset.x >= node.position.x &&
                            adjustedOffset.x <= node.position.x + node.size.width &&
                            adjustedOffset.y >= node.position.y &&
                            adjustedOffset.y <= node.position.y + node.size.height
                        }
                        
                        if (clickedNode != null && selectedTool == CanvasTool.SELECT) {
                            draggedNode = clickedNode.id
                        }
                    },
                    onDrag = { _, dragAmount ->
                        draggedNode?.let { nodeId ->
                            onStateChange(
                                state.copy(
                                    nodes = state.nodes.map { node ->
                                        if (node.id == nodeId) {
                                            node.copy(position = node.position + dragAmount / state.scale)
                                        } else node
                                    }
                                )
                            )
                        }
                    },
                    onDragEnd = {
                        draggedNode = null
                    }
                )
            }
            .pointerInput(selectedTool) {
                detectTapGestures(
                    onTap = { offset ->
                        val adjustedOffset = (offset - state.offset) / state.scale
                        val clickedNode = state.nodes.find { node ->
                            adjustedOffset.x >= node.position.x &&
                            adjustedOffset.x <= node.position.x + node.size.width &&
                            adjustedOffset.y >= node.position.y &&
                            adjustedOffset.y <= node.position.y + node.size.height
                        }
                        
                        if (clickedNode != null) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime < 300) {
                                // Double click
                                onNodeDoubleClick(clickedNode)
                            } else {
                                // Single click
                                onNodeClick(clickedNode)
                            }
                            lastClickTime = currentTime
                        }
                    }
                )
            }
    ) {
        // Apply transformations
        scale(state.scale) {
            translate(state.offset.x / state.scale, state.offset.y / state.scale) {
                // Draw connections
                state.connections.forEach { connection ->
                    val fromNode = state.nodes.find { it.id == connection.fromNodeId }
                    val toNode = state.nodes.find { it.id == connection.toNodeId }
                    
                    if (fromNode != null && toNode != null) {
                        drawConnection(fromNode, toNode, connection)
                    }
                }
                
                // Draw nodes
                state.nodes.forEach { node ->
                    drawNode(node)
                }
                
                // Draw connection preview
                if (isConnecting && connectionStartNode != null) {
                    val startNode = state.nodes.find { it.id == connectionStartNode }
                    if (startNode != null) {
                        // Draw preview line (this would need mouse position)
                        // For now, just highlight the start node
                        drawNode(startNode.copy(color = Color.Green))
                    }
                }
            }
        }
    }
}

fun DrawScope.drawNode(node: CanvasNode) {
    val rect = androidx.compose.ui.geometry.Rect(
        offset = node.position,
        size = node.size
    )
    
    // Draw node background
    drawRoundRect(
        color = node.color.copy(alpha = 0.3f),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
    )
    
    // Draw node border
    drawRoundRect(
        color = if (node.isSelected) Color.Red else node.color,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
        style = Stroke(width = if (node.isSelected) 3.dp.toPx() else 2.dp.toPx())
    )
    
    // Draw text (simplified - in real implementation you'd use TextMeasurer)
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        canvas.nativeCanvas.drawText(
            node.text,
            rect.center.x,
            rect.center.y,
            paint
        )
    }
}

fun DrawScope.drawConnection(fromNode: CanvasNode, toNode: CanvasNode, connection: CanvasConnection) {
    val fromCenter = fromNode.position + Offset(fromNode.size.width / 2, fromNode.size.height / 2)
    val toCenter = toNode.position + Offset(toNode.size.width / 2, toNode.size.height / 2)
    
    // Draw line
    drawLine(
        color = connection.color,
        start = fromCenter,
        end = toCenter,
        strokeWidth = connection.strokeWidth
    )
    
    // Draw arrow head
    val angle = atan2(toCenter.y - fromCenter.y, toCenter.x - fromCenter.x)
    val arrowLength = 15f
    val arrowAngle = PI / 6
    
    val arrowPoint1 = Offset(
        toCenter.x - arrowLength * cos(angle - arrowAngle).toFloat(),
        toCenter.y - arrowLength * sin(angle - arrowAngle).toFloat()
    )
    
    val arrowPoint2 = Offset(
        toCenter.x - arrowLength * cos(angle + arrowAngle).toFloat(),
        toCenter.y - arrowLength * sin(angle + arrowAngle).toFloat()
    )
    
    val arrowPath = Path().apply {
        moveTo(toCenter.x, toCenter.y)
        lineTo(arrowPoint1.x, arrowPoint1.y)
        lineTo(arrowPoint2.x, arrowPoint2.y)
        close()
    }
    
    drawPath(arrowPath, connection.color)
}

@Composable
fun NodeEditDialog(
    node: CanvasNode,
    onDismiss: () -> Unit,
    onSave: (CanvasNode) -> Unit
) {
    var text by remember { mutableStateOf(node.text) }
    var selectedColor by remember { mutableStateOf(node.color) }
    
    val colors = listOf(
        Color.Blue, Color.Red, Color.Green, Color.Yellow,
        Color(0xFF9C27B0), Color(0xFFFF9800), Color.Cyan, Color.Magenta
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Node",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Node Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(node.copy(text = text, color = selectedColor))
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}