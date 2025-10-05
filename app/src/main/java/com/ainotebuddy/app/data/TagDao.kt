package com.ainotebuddy.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE isActive = 1 ORDER BY usageCount DESC")
    fun getAllTags(): Flow<List<TagEntity>>
    
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?
    
    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): TagEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long
    
    @Update
    suspend fun updateTag(tag: TagEntity)
    
    @Query("UPDATE tags SET usageCount = usageCount + 1 WHERE name = :name")
    suspend fun incrementUsageCount(name: String)
    
    @Query("UPDATE tags SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteTag(id: Long)
    
    @Delete
    suspend fun deleteTag(tag: TagEntity)

    // Bridging helpers for legacy usages
    suspend fun insert(tag: TagEntity): Long = insertTag(tag)
    suspend fun update(tag: TagEntity) = updateTag(tag)
    suspend fun delete(tag: TagEntity) = deleteTag(tag)
    suspend fun incrementUsage(name: String) = incrementUsageCount(name)
    suspend fun searchTags(query: String): List<TagEntity> =
        getAllTags().first().filter { it.name.contains(query, ignoreCase = true) }
}