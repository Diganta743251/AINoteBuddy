package com.ainotebuddy.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "templates",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String?,
    val icon: String,
    val category: String
)
