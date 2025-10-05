package com.ainotebuddy.app.sync

import android.content.Context
import com.ainotebuddy.app.data.NoteEntity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GoogleDriveSyncService(private val context: Context) {
    
    private val gson = Gson()
    private val notesFileName = "ainotebuddy_notes.json"
    private val notesFolderName = "AINoteBuddy"
    
    suspend fun uploadNotes(notes: List<NoteEntity>, driveService: Drive?) {
        if (driveService == null) return
        return withContext(Dispatchers.IO) {
            try {
                // Create or get the AINoteBuddy folder
                val folderId = getOrCreateFolder(driveService, notesFolderName)
                
                // Convert notes to JSON
                val notesJson = gson.toJson(notes)
                val notesBytes = notesJson.toByteArray()
                
                // Check if file already exists
                val existingFileId = findFile(driveService, notesFileName, folderId)
                
                val fileMetadata = File().apply {
                    name = notesFileName
                    parents = listOf(folderId)
                }
                
                val mediaContent = ByteArrayContent("application/json", notesBytes)
                
                val uploadedFile = if (existingFileId != null) {
                    // Update existing file
                    driveService.files().update(existingFileId, fileMetadata, mediaContent)
                        .execute()
                } else {
                    // Create new file
                    driveService.files().create(fileMetadata, mediaContent)
                        .execute()
                }
                
                // Success
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun downloadNotes(driveService: Drive?): List<NoteEntity> {
        if (driveService == null) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                // Get the AINoteBuddy folder
                val folderId = findFolder(driveService, notesFolderName)
                if (folderId == null) {
                    return@withContext emptyList()
                }
                
                // Find the notes file
                val fileId = findFile(driveService, notesFileName, folderId)
                if (fileId == null) {
                    return@withContext emptyList()
                }
                
                // Download the file content
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
                
                val notesJson = outputStream.toString()
                val notesType = object : TypeToken<List<NoteEntity>>() {}.type
                val notes = gson.fromJson<List<NoteEntity>>(notesJson, notesType)
                
                notes ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun getOrCreateFolder(driveService: Drive, folderName: String): String {
        val existingFolderId = findFolder(driveService, folderName)
        if (existingFolderId != null) {
            return existingFolderId
        }
        
        val folderMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        
        val folder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()
        
        return folder.id
    }
    
    private fun findFolder(driveService: Drive, folderName: String): String? {
        val result = driveService.files().list()
            .setQ("name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
        
        return result.files.firstOrNull()?.id
    }
    
    private fun findFile(driveService: Drive, fileName: String, parentFolderId: String): String? {
        val result = driveService.files().list()
            .setQ("name='$fileName' and '$parentFolderId' in parents and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
        
        return result.files.firstOrNull()?.id
    }
} 