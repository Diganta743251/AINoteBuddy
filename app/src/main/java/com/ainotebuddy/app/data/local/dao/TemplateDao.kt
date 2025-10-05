package com.ainotebuddy.app.data.local.dao

import androidx.room.*
import com.ainotebuddy.app.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TemplateEntity?>

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TemplateEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(entity: TemplateEntity)

    @Delete
    suspend fun delete(entity: TemplateEntity)

    @Query("DELETE FROM templates WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
