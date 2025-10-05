package com.ainotebuddy.app.data.local.analytics

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TagUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagUsageEntity)

    @Query("SELECT * FROM tag_usages ORDER BY count DESC LIMIT :limit")
    fun getMostUsedTags(limit: Int): Flow<List<TagUsageEntity>>

    @Query("SELECT * FROM tag_usages WHERE tag LIKE '%' || :query || '%' ORDER BY count DESC")
    fun searchTags(query: String): Flow<List<TagUsageEntity>>

    @Query("UPDATE tag_usages SET count = count + 1, lastUsed = :timestamp WHERE tag = :tag")
    suspend fun incrementTagUsage(tag: String, timestamp: LocalDateTime)

    @Query("SELECT EXISTS(SELECT 1 FROM tag_usages WHERE tag = :tag)")
    suspend fun tagExists(tag: String): Boolean

    @Query("DELETE FROM tag_usages WHERE tag = :tag")
    suspend fun deleteTag(tag: String)

    @Query("SELECT COUNT(*) FROM tag_usages")
    suspend fun getTagCount(): Int

    @Query("SELECT SUM(count) FROM tag_usages")
    suspend fun getTotalTagUsage(): Int
}