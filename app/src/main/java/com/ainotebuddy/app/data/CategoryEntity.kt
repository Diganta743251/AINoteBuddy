package com.ainotebuddy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val icon: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val parentId: Long? = null,
    val isLocked: Boolean = false
) 