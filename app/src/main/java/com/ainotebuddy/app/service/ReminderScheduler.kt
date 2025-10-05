package com.ainotebuddy.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.ainotebuddy.app.NoteReminderReceiver

/**
 * Thin wrapper for scheduling precise reminders via AlarmManager.
 */
object ReminderScheduler {
    fun schedule(context: Context, noteId: Long, timeMillis: Long, title: String, content: String = "") {
        val intent = Intent(context, NoteReminderReceiver::class.java).apply {
            putExtra("noteId", noteId)
            putExtra("noteTitle", title)
            putExtra("noteContent", content)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi)
    }

    fun cancel(context: Context, noteId: Long) {
        val intent = Intent(context, NoteReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pi)
            pi.cancel()
        }
    }
}