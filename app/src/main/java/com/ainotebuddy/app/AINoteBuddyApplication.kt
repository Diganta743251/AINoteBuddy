package com.ainotebuddy.app

import android.app.Application
import android.util.Log
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.data.preferences.SettingsRepository
import com.ainotebuddy.app.workers.EmbeddingUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class AINoteBuddyApplication : Application() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AINoteBuddy Application starting...")
        
        setupTheme()
        scheduleBackgroundTasks()
    }

    private fun setupTheme() {
        // Theme will be applied in MainActivity based on user preferences
        Log.d(TAG, "Theme setup complete")
    }
    
    private fun scheduleBackgroundTasks() {
        // Respect "Pause AI Processing" setting before scheduling embeddings
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val paused = settingsRepository.pauseAIProcessing.first()
                if (!paused) {
                    EmbeddingUpdateWorker.schedule(this@AINoteBuddyApplication)
                    Log.d(TAG, "Background embedding worker scheduled")
                } else {
                    Log.d(TAG, "AI processing paused, skipping worker schedule")
                }
            } catch (e: Exception) {
                // Fallback: Schedule worker if settings unavailable
                Log.w(TAG, "Error reading AI settings, scheduling worker anyway: ${e.message}")
                EmbeddingUpdateWorker.schedule(this@AINoteBuddyApplication)
            }
        }
    }
    
    companion object {
        private const val TAG = "AINoteBuddyApp"
    }
}

