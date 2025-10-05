package com.ainotebuddy.app.features
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerScreen(
    folders: List<com.ainotebuddy.app.data.FolderEntity>,
    notes: List<com.ainotebuddy.app.data.NoteEntity>,
    onBack: () -> Unit,
    onCreateFolder: (String, Long?) -> Unit,
    onDeleteFolder: (com.ainotebuddy.app.data.FolderEntity) -> Unit,
    onRenameFolder: (com.ainotebuddy.app.data.FolderEntity, String) -> Unit,
    onMoveFolder: (com.ainotebuddy.app.data.FolderEntity, Long?) -> Unit,
    onToggleExpanded: (com.ainotebuddy.app.data.FolderEntity) -> Unit,
    onFolderClick: (com.ainotebuddy.app.data.FolderEntity) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf<Long?>(null) }
    var showRenameDialog by remember { mutableStateOf<com.ainotebuddy.app.data.FolderEntity?>(null) }
    var selectedFolder by remember { mutableStateOf<com.ainotebuddy.app.data.FolderEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Folder Manager") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showCreateDialog = null }) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder")
                }
            }
        )

        if (folders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(64.dp))
                    Text("No folders yet", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { showCreateDialog = null }) { Text("Create First Folder") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { folder ->
                    val noteCount = notes.count { it.folderId == folder.id }
                    Card(
                        onClick = {
                            selectedFolder = folder
                            onFolderClick(folder)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                getFolderIcon(folder.icon ?: "folder"),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                if (noteCount > 0) {
                                    Text("${noteCount} notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { showRenameDialog = folder }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Rename")
                            }
                            IconButton(onClick = { onDeleteFolder(folder) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Create folder dialog
    showCreateDialog?.let { parentId ->
        CreateFolderDialog(
            parentFolder = folders.find { it.id == parentId },
            onDismiss = { showCreateDialog = null },
            onCreate = { name ->
                onCreateFolder(name, parentId)
                showCreateDialog = null
            }
        )
    }

    // Rename folder dialog
    showRenameDialog?.let { folder ->
        RenameFolderDialog(
            folder = folder,
            onDismiss = { showRenameDialog = null },
            onRename = { newName ->
                onRenameFolder(folder, newName)
                showRenameDialog = null
            }
        )
    }
}

@Composable
fun CreateFolderDialog(
    parentFolder: com.ainotebuddy.app.data.FolderEntity?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (parentFolder != null) "Create Subfolder in ${parentFolder.name}"
                else "Create New Folder"
            )
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(folderName) },
                enabled = folderName.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RenameFolderDialog(
    folder: com.ainotebuddy.app.data.FolderEntity,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var folderName by remember { mutableStateOf(folder.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(folderName) },
                enabled = folderName.isNotBlank() && folderName != folder.name
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun getFolderIcon(iconName: String): ImageVector {
    return when (iconName) {
        "work" -> Icons.Filled.Work
        "home" -> Icons.Filled.Home
        "school" -> Icons.Filled.School
        "favorite" -> Icons.Filled.Favorite
        "star" -> Icons.Filled.Star
        else -> Icons.Filled.Folder
    }
}