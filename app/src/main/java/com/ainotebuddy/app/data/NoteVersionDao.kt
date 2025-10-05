package com.ainotebuddy.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY versionNumber DESC")
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersionEntity>>
    
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestVersion(noteId: Long): NoteVersionEntity?
    
    @Query("SELECT * FROM note_versions WHERE id = :versionId")
    suspend fun getVersionById(versionId: Long): NoteVersionEntity?
    
    @Insert
    suspend fun insertVersion(version: NoteVersionEntity): Long
    
    @Delete
    suspend fun deleteVersion(version: NoteVersionEntity)
    
    @Query("DELETE FROM note_versions WHERE noteId = :noteId AND versionNumber < :keepVersionsAfter")
    suspend fun deleteOldVersions(noteId: Long, keepVersionsAfter: Int)
    
    @Query("SELECT COUNT(*) FROM note_versions WHERE noteId = :noteId")
    suspend fun getVersionCount(noteId: Long): Int
    
    @Query("SELECT MAX(versionNumber) FROM note_versions WHERE noteId = :noteId")
    suspend fun getLatestVersionNumber(noteId: Long): Int?
}