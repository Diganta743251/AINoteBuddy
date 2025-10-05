package com.ainotebuddy.app.data.model.organization

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

/**
 * Represents a note that should be created on a recurring schedule
 */
@Entity(
    tableName = "recurring_notes",
    foreignKeys = [
        ForeignKey(
            entity = NoteTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("templateId")]
)
data class RecurringNote(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val templateId: String? = null,
    val templateVariables: Map<String, String> = emptyMap(),
    val recurrenceRule: RecurrenceRule,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val lastTriggered: Long? = null,
    val nextTrigger: Long = calculateNextTrigger(System.currentTimeMillis(), RecurrenceRule.DAILY),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Defines the recurrence rule for the note
     */
    enum class RecurrenceRule(val displayName: String, val interval: Int) {
        DAILY("Daily", 1),
        WEEKLY("Weekly", 7),
        BIWEEKLY("Bi-weekly", 14),
        MONTHLY("Monthly", 30),
        QUARTERLY("Quarterly", 90),
        YEARLY("Yearly", 365);

        companion object {
            fun fromDisplayName(displayName: String): RecurrenceRule? {
                return values().find { it.displayName.equals(displayName, ignoreCase = true) }
            }
        }
    }

    companion object {
        /**
         * Calculate the next trigger time based on the current time and recurrence rule
         */
        fun calculateNextTrigger(currentTime: Long, rule: RecurrenceRule): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentTime
                add(Calendar.DAY_OF_YEAR, rule.interval)
            }
            return calendar.timeInMillis
        }
    }
}
