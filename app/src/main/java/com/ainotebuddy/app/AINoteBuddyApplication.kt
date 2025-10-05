package com.ainotebuddy.app

import android.app.Application
import com.ainotebuddy.app.workers.EmbeddingUpdateWorker
// TODO: Re-enable Hilt when compatibility issues are resolved
// import com.ainotebuddy.app.data.PreferencesManager
// import com.ainotebuddy.app.data.preferences.SettingsRepository
// import dagger.hilt.android.HiltAndroidApp
// import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// @HiltAndroidApp
class AINoteBuddyApplication : Application() {

    // TODO: Re-enable when Hilt is restored
    // @Inject lateinit var preferencesManager: PreferencesManager
    // @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        setupTheme()

        // TODO: Re-enable when Hilt is restored
        // Respect "Pause AI Processing" setting before scheduling embeddings
        /*
        CoroutineScope(Dispatchers.Default).launch {
            val paused = try { settingsRepository.pauseAIProcessing.first() } catch (_: Throwable) { false }
            if (!paused) {
                EmbeddingUpdateWorker.schedule(this@AINoteBuddyApplication)
            }
        }
        */
        
        // Temporary: Schedule embeddings without settings check
        CoroutineScope(Dispatchers.Default).launch {
            try {
                EmbeddingUpdateWorker.schedule(this@AINoteBuddyApplication)
            } catch (_: Throwable) {
                // Ignore worker scheduling errors for now
            }
        }
    }

    private fun setupTheme() {
        // Apply saved theme when app starts
    }
}

