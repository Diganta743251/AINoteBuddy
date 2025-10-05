package com.ainotebuddy.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for checklist items. Uses ChecklistItemEntity defined in ChecklistItemEntity.kt
 */
@Dao
interface ChecklistItemDao {

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY `order` ASC, id ASC")
    fun getItemsForNote(noteId: Long): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItemEntity): Long

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Update
    suspend fun updateItems(items: List<ChecklistItemEntity>)

    @Delete
    suspend fun delete(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteAllForNote(noteId: Long)

    @Query("UPDATE checklist_items SET completed = :completed WHERE id = :id")
    suspend fun updateCompletion(id: Long, completed: Boolean)
}