package com.ainotebuddy.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ainotebuddy.app.data.NoteDao
import com.ainotebuddy.app.data.AppDatabase
import com.ainotebuddy.app.MainActivity
import com.ainotebuddy.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class StickyNoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_sticky_note)

            // Load the most recent note (blocking for simplicity)
            val db = AppDatabase.getInstance(context)
            val noteDao: NoteDao = db.noteDao()
            val note = runBlocking(Dispatchers.IO) {
                noteDao.getMostRecentNote()
            }
            if (note != null) {
                views.setTextViewText(R.id.widget_note_title, note.title)
                views.setTextViewText(R.id.widget_note_content, note.content)
            } else {
                views.setTextViewText(R.id.widget_note_title, "No notes yet")
                views.setTextViewText(R.id.widget_note_content, "Tap to add a note")
            }

            // Open app on widget tap
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_note_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_note_content, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
} 