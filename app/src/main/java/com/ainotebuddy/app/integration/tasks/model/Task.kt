package com.ainotebuddy.app.integration.tasks.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

/**
 * Represents a task that can be created from a note.
 *
 * @property id The unique ID of the task
 * @property noteId The ID of the note this task is created from
 * @property title The title of the task
 * @property description The description of the task
 * @property dueDate The due date of the task (optional)
 * @property priority The priority of the task (1-5, with 5 being highest)
 * @property isCompleted Whether the task is completed
 * @property createdAt When the task was created
 * @property updatedAt When the task was last updated
 * @property completedAt When the task was completed (if completed)
 */
@Parcelize
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = com.ainotebuddy.app.data.NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val title: String,
    val description: String = "",
    val dueDate: LocalDateTime? = null,
    val priority: Int = 3, // Default to medium priority
    val isCompleted: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
) : Parcelable {
    /**
     * Returns a copy of the task with the completed status toggled.
     */
    fun toggleCompleted(): Task {
        return copy(
            isCompleted = !isCompleted,
            completedAt = if (!isCompleted) LocalDateTime.now() else null,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Returns a copy of the task with the priority updated.
     */
    fun withPriority(newPriority: Int): Task {
        return copy(
            priority = newPriority.coerceIn(1, 5), // Ensure priority is between 1-5
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Returns a copy of the task with the due date updated.
     */
    fun withDueDate(newDueDate: LocalDateTime?): Task {
        return copy(
            dueDate = newDueDate,
            updatedAt = LocalDateTime.now()
        )
    }
}
