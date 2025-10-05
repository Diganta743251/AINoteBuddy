package com.ainotebuddy.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormatter {
    fun formatRelativeTime(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = timeMillis - now

        if (diff < 0) return "expired"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 60 -> "in ${minutes} min"
            hours < 24 -> "in ${hours} h"
            days == 0L -> "today, " + formatTime(timeMillis)
            days == 1L -> "tomorrow, " + formatTime(timeMillis)
            days in 2..6 -> SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(Date(timeMillis))
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timeMillis))
        }
    }

    fun formatClock(timeMillis: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timeMillis))

    private fun formatTime(timeMillis: Long): String {
        // Show special labels for common natural terms if exact times match
        val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
        return when (timeOnly) {
            "12:00" -> "noon"
            "00:00" -> "midnight"
            "21:00" -> "tonight 9:00 PM"
            else -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timeMillis))
        }
    }
}