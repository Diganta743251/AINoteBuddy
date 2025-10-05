package com.ainotebuddy.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickActionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AINoteBuddy Quick Actions")
        .setContentText("Tap an action to create a note, checklist, or reminder.")
        .setSmallIcon(R.drawable.ic_add)
        .addAction(
            R.drawable.ic_add, "New Note",
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).setData("ainotebuddy://new_note".toUri()),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            R.drawable.ic_add, "New Checklist",
            PendingIntent.getActivity(
                this, 1,
                Intent(this, MainActivity::class.java).setData("ainotebuddy://new_checklist".toUri()),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            R.drawable.ic_add, "New Reminder",
            PendingIntent.getActivity(
                this, 2,
                Intent(this, MainActivity::class.java).setData("ainotebuddy://new_reminder".toUri()),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AINoteBuddy Quick Actions",
                NotificationManager.IMPORTANCE_LOW
            )
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    // Scaffold: Function to schedule a notification for a note reminder
    fun scheduleNoteReminder(noteId: Long, reminderTime: Long, context: Context) {
        // Delegate to ReminderScheduler for single scheduling path
        com.ainotebuddy.app.service.ReminderScheduler.schedule(
            context,
            noteId,
            reminderTime,
            title = "Note Reminder",
            content = ""
        )
    }

    companion object {
        private const val CHANNEL_ID = "ainotebuddy_quick_actions"
    }
}

class NoteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra("noteId", -1)
        val noteTitle = intent.getStringExtra("noteTitle") ?: "Note Reminder"
        val noteContent = intent.getStringExtra("noteContent") ?: "You have a note reminder."
        
        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(noteTitle)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
    }
} 