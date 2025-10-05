package com.ainotebuddy.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ainotebuddy.app.MainActivity
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationSettings(
    val syncNotifications: Boolean = true,
    val reminderNotifications: Boolean = true,
    val aiSuggestionNotifications: Boolean = true,
    val collaborationNotifications: Boolean = true,
    val backupNotifications: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)

@Singleton
class SmartNotificationManager @Inject constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    
    companion object {
        // Notification Channels
        const val CHANNEL_SYNC = "sync_notifications"
        const val CHANNEL_REMINDERS = "reminder_notifications"
        const val CHANNEL_AI_SUGGESTIONS = "ai_suggestion_notifications"
        const val CHANNEL_COLLABORATION = "collaboration_notifications"
        const val CHANNEL_BACKUP = "backup_notifications"
        const val CHANNEL_GENERAL = "general_notifications"
        
        // Notification IDs
        const val NOTIFICATION_SYNC_PROGRESS = 1001
        const val NOTIFICATION_SYNC_COMPLETE = 1002
        const val NOTIFICATION_SYNC_ERROR = 1003
        const val NOTIFICATION_REMINDER = 2001
        const val NOTIFICATION_AI_SUGGESTION = 3001
        const val NOTIFICATION_COLLABORATION = 4001
        const val NOTIFICATION_BACKUP_COMPLETE = 5001
        const val NOTIFICATION_BACKUP_FAILED = 5002
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_SYNC,
                    "Sync Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications about sync progress and status"
                },
                
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Note Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for your notes"
                    enableVibration(true)
                },
                
                NotificationChannel(
                    CHANNEL_AI_SUGGESTIONS,
                    "AI Suggestions",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "AI-powered suggestions for your notes"
                },
                
                NotificationChannel(
                    CHANNEL_COLLABORATION,
                    "Collaboration",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Collaboration and sharing notifications"
                },
                
                NotificationChannel(
                    CHANNEL_BACKUP,
                    "Backup & Restore",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Backup and restore notifications"
                },
                
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General app notifications"
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { systemNotificationManager.createNotificationChannel(it) }
        }
    }
    
    // Sync Notifications
    fun showSyncStartedNotification() {
        if (!shouldShowNotification()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setContentTitle("Syncing Notes")
            .setContentText("Synchronizing your notes with the cloud...")
            .setSmallIcon(R.drawable.ic_sync)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_SYNC_PROGRESS, notification)
    }
    
    fun updateSyncProgress(progress: Float, message: String = "") {
        if (!shouldShowNotification()) return
        
        val progressInt = (progress * 100).toInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setContentTitle("Syncing Notes")
            .setContentText(message.ifEmpty { "Synchronizing your notes... ($progressInt%)" })
            .setSmallIcon(R.drawable.ic_sync)
            .setProgress(100, progressInt, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_SYNC_PROGRESS, notification)
    }
    
    fun showSyncCompleteNotification(syncedCount: Int) {
        if (!shouldShowNotification()) return
        
        // Cancel progress notification
        notificationManager.cancel(NOTIFICATION_SYNC_PROGRESS)
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setContentTitle("Sync Complete")
            .setContentText("Successfully synced $syncedCount notes")
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_SYNC_COMPLETE, notification)
    }
    
    fun showSyncErrorNotification(error: String) {
        if (!shouldShowNotification()) return
        
        // Cancel progress notification
        notificationManager.cancel(NOTIFICATION_SYNC_PROGRESS)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_sync_settings", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setContentTitle("Sync Failed")
            .setContentText("Failed to sync notes: $error")
            .setSmallIcon(R.drawable.ic_error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(NOTIFICATION_SYNC_ERROR, notification)
    }
    
    // Note Reminders
    fun showNoteReminderNotification(note: NoteEntity, reminderText: String) {
        if (!shouldShowNotification()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("note_id", note.id)
            putExtra("open_note", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, note.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setContentTitle("Note Reminder")
            .setContentText(reminderText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${note.title}\n\n$reminderText")
            )
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()
        
        notificationManager.notify(NOTIFICATION_REMINDER + note.id.toInt(), notification)
    }
    
    // AI Suggestions
    fun showAISuggestionNotification(note: NoteEntity, suggestions: List<String>) {
        if (!shouldShowNotification() || suggestions.isEmpty()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("note_id", note.id)
            putExtra("show_ai_suggestions", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, note.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val suggestionText = suggestions.take(3).joinToString("\n• ", "• ")
        
        val notification = NotificationCompat.Builder(context, CHANNEL_AI_SUGGESTIONS)
            .setContentTitle("AI Suggestions Available")
            .setContentText("New suggestions for \"${note.title.take(30)}${if(note.title.length > 30) "..." else ""}\"")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("AI has suggestions for your note:\n\n$suggestionText")
            )
            .setSmallIcon(R.drawable.ic_lightbulb)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_AI_SUGGESTION + note.id.toInt(), notification)
    }
    
    // Collaboration Notifications
    fun showCollaborationInviteNotification(noteTitle: String, inviterName: String) {
        if (!shouldShowNotification()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("show_collaboration", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLABORATION)
            .setContentTitle("Collaboration Invite")
            .setContentText("$inviterName invited you to collaborate on \"$noteTitle\"")
            .setSmallIcon(R.drawable.ic_people)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(NOTIFICATION_COLLABORATION, notification)
    }
    
    fun showNoteSharedNotification(noteTitle: String, recipientName: String) {
        if (!shouldShowNotification()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLABORATION)
            .setContentTitle("Note Shared")
            .setContentText("Successfully shared \"$noteTitle\" with $recipientName")
            .setSmallIcon(R.drawable.ic_share)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    // Backup Notifications
    fun showBackupCompleteNotification(backupSize: String, noteCount: Int) {
        if (!shouldShowNotification()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_backup_settings", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setContentTitle("Backup Complete")
            .setContentText("Successfully backed up $noteCount notes ($backupSize)")
            .setSmallIcon(R.drawable.ic_backup)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_BACKUP_COMPLETE, notification)
    }
    
    fun showBackupFailedNotification(error: String) {
        if (!shouldShowNotification()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_backup_settings", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setContentTitle("Backup Failed")
            .setContentText("Failed to backup notes: $error")
            .setSmallIcon(R.drawable.ic_error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(NOTIFICATION_BACKUP_FAILED, notification)
    }
    
    // Utility functions
    private fun shouldShowNotification(): Boolean {
        // Check if notifications are enabled in preferences
        // Check quiet hours
        // Check do not disturb mode
        return true // Simplified for now
    }

    fun cancelAllSyncNotifications() {
        notificationManager.cancel(NOTIFICATION_SYNC_PROGRESS)
        notificationManager.cancel(NOTIFICATION_SYNC_COMPLETE)
        notificationManager.cancel(NOTIFICATION_SYNC_ERROR)
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    fun hasNotificationPermission(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}