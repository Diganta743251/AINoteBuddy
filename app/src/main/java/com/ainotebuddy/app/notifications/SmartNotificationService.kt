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
import com.ainotebuddy.app.ai.EnhancedAIService
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.repository.NoteRepository
import com.ainotebuddy.app.data.toEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartNotificationService @Inject constructor(
    private val application: Application,
    private val noteRepository: NoteRepository,
    private val enhancedAIService: EnhancedAIService
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val notificationManager = NotificationManagerCompat.from(application)

    companion object {
        private const val CHANNEL_ID_REMINDERS = "note_reminders"
        private const val CHANNEL_ID_AI_INSIGHTS = "ai_insights"
        private const val CHANNEL_ID_PRODUCTIVITY = "productivity_tips"

        private const val NOTIFICATION_ID_DAILY_REVIEW = 1001
        private const val NOTIFICATION_ID_AI_INSIGHT = 1002
        private const val NOTIFICATION_ID_PRODUCTIVITY = 1003
        private const val NOTIFICATION_ID_ACTION_ITEMS = 1004
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Note Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for your notes and tasks"
            }

            val insightsChannel = NotificationChannel(
                CHANNEL_ID_AI_INSIGHTS,
                "AI Insights",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI-powered insights about your notes"
            }

            val productivityChannel = NotificationChannel(
                CHANNEL_ID_PRODUCTIVITY,
                "Productivity Tips",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Productivity insights and suggestions"
            }

            val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(insightsChannel)
            manager.createNotificationChannel(productivityChannel)
        }
    }

    /**
     * Show daily review notification with AI insights
     */
    fun showDailyReviewNotification() {
        serviceScope.launch {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            val domainNotes = noteRepository.getAllNotes().first()
            val recentNotes = domainNotes.filter { it.dateModified >= cutoff }
            if (recentNotes.isEmpty()) return@launch
            
            val aiInsights = generateDailyInsights(recentNotes.map { it.toEntity() })
            
            val intent = Intent(application, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(

                application, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(application, CHANNEL_ID_AI_INSIGHTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Daily Note Review")
                .setContentText(aiInsights.summary)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(aiInsights.detailedInsight))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID_DAILY_REVIEW, notification)
        }
    }
    
    /**
     * Show AI insight notification for a specific note
     */
    fun showAIInsightNotification(note: NoteEntity) {
        serviceScope.launch {
            val analysis = enhancedAIService.performComprehensiveAnalysis(note)
            
            if (analysis.improvementSuggestions.isNotEmpty()) {
                val intent = Intent(application, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("noteId", note.id)
                }
                val pendingIntent = PendingIntent.getActivity(
                    application, note.id.toInt(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = NotificationCompat.Builder(application, CHANNEL_ID_AI_INSIGHTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("AI Insight for \"${note.title}\"")
                    .setContentText(analysis.improvementSuggestions.first())
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("AI suggests: ${analysis.improvementSuggestions.joinToString(", ")}"))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                
                notificationManager.notify(NOTIFICATION_ID_AI_INSIGHT + note.id.toInt(), notification)
            }
        }
    }
    
    /**
     * Show productivity tip notification
     */
    fun showProductivityTipNotification() {
        serviceScope.launch {
            val tip = generateProductivityTip()
            
            val intent = Intent(application, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                application, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(application, CHANNEL_ID_PRODUCTIVITY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Productivity Tip")
                .setContentText(tip.title)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(tip.description))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID_PRODUCTIVITY, notification)
        }
    }
    
    /**
     * Show action items reminder notification
     */
    fun showActionItemsReminder(actionItems: List<String>) {
        if (actionItems.isEmpty()) return
        
        val intent = Intent(application, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            application, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(application, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Action Items Reminder")
            .setContentText("You have ${actionItems.size} pending action items")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Pending items:\n${actionItems.take(3).joinToString("\n• ", "• ")}"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_ACTION_ITEMS, notification)
    }
    
    /**
     * Generate daily insights from recent notes
     */
    private suspend fun generateDailyInsights(notes: List<NoteEntity>): DailyInsights {
        val analyses = notes.map { enhancedAIService.performComprehensiveAnalysis(it) }
        
        val totalNotes = notes.size
        val averageSentiment = analyses.map { 
            when (it.sentiment.sentiment) {
                com.ainotebuddy.app.ai.Sentiment.POSITIVE -> 1f
                com.ainotebuddy.app.ai.Sentiment.NEUTRAL -> 0f
                com.ainotebuddy.app.ai.Sentiment.NEGATIVE -> -1f
            }
        }.average().toFloat()
        
        val totalActionItems = analyses.sumOf { it.actionItems.size }
        val commonTags = analyses.flatMap { it.recommendedTags }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }?.key
        
        val summary = when {
            totalNotes == 0 -> "No notes created recently"
            averageSentiment > 0.3f -> "You've been productive with a positive outlook!"
            averageSentiment < -0.3f -> "Consider focusing on positive action items"
            else -> "You've been maintaining steady note-taking habits"
        }
        
        val detailedInsight = buildString {
            append("In the past week:\n")
            append("• Created $totalNotes notes\n")
            append("• Identified $totalActionItems action items\n")
            if (commonTags != null) {
                append("• Most common topic: $commonTags\n")
            }
            append("• Overall sentiment: ${getSentimentDescription(averageSentiment)}")
        }
        
        return DailyInsights(summary, detailedInsight)
    }
    
    /**
     * Generate a random productivity tip
     */
    private fun generateProductivityTip(): ProductivityTip {
        val tips = listOf(
            ProductivityTip(
                "Use Tags Effectively",
                "Add relevant tags to your notes for better organization. Try using tags like #important, #idea, or #todo."
            ),
            ProductivityTip(
                "Regular Reviews",
                "Review your notes weekly to identify action items and track progress on your goals."
            ),
            ProductivityTip(
                "Voice Notes",
                "Try creating voice notes when you're on the go. It's faster than typing and captures your natural thought process."
            ),
            ProductivityTip(
                "Note Templates",
                "Create templates for common note types like meeting notes, project plans, or daily journals."
            ),
            ProductivityTip(
                "Quick Capture",
                "Write down ideas immediately when they come to you. The AI can help organize them later."
            ),
            ProductivityTip(
                "Connect Ideas",
                "Look for connections between your notes. The AI can help identify related concepts and themes."
            )
        )
        
        return tips.random()
    }
    
    /**
     * Get human-readable sentiment description
     */
    private fun getSentimentDescription(sentiment: Float): String {
        return when {
            sentiment > 0.5f -> "Very Positive"
            sentiment > 0.2f -> "Positive"
            sentiment > -0.2f -> "Neutral"
            sentiment > -0.5f -> "Negative"
            else -> "Very Negative"
        }
    }
}

/**
 * Daily insights data class
 */
data class DailyInsights(
    val summary: String,
    val detailedInsight: String
)

/**
 * Productivity tip data class
 */
data class ProductivityTip(
    val title: String,
    val description: String
)