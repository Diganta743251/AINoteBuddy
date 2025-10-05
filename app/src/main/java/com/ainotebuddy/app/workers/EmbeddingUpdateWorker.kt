package com.ainotebuddy.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ainotebuddy.app.ai.embeddings.AIEmbeddings
import com.ainotebuddy.app.ai.embeddings.AISemanticSearch
import com.ainotebuddy.app.ai.embeddings.InMemorySemanticSearch
import com.ainotebuddy.app.repository.NoteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically updates embeddings for notes and refreshes the local semantic index.
 * Runs only when device is charging and idle (battery-friendly).
 */
@HiltWorker
class EmbeddingUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val noteRepository: NoteRepository,
    private val embeddings: AIEmbeddings
) : CoroutineWorker(appContext, params) {

    private val index: AISemanticSearch = InMemorySemanticSearch()

    override suspend fun doWork(): Result = try {
        val notes = noteRepository.getAllNotes().first()
        // Batch update
        for (note in notes) {
            val vec = embeddings.embed(note.title + "\n" + note.content)
            index.index("note", note.id, vec)
        }
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "embedding_update"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<EmbeddingUpdateWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}