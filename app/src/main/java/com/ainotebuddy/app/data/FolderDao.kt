package com.ainotebuddy.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isActive = 1")
    fun getAllFolders(): Flow<List<FolderEntity>>
    
    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?
    
    @Query("SELECT * FROM folders WHERE parentId = :parentId")
    fun getFoldersByParent(parentId: Long?): Flow<List<FolderEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long
    
    @Update
    suspend fun updateFolder(folder: FolderEntity)
    
    @Query("UPDATE folders SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteFolder(id: Long)
    
    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}