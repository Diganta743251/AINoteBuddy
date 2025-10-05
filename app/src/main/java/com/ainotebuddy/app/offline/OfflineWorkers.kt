package com.ainotebuddy.app.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import androidx.lifecycle.asFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background workers for Enhanced Offline-First Architecture
 * Handles periodic sync, cleanup, and maintenance tasks
 */

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineOperationManager: OfflineOperationManager,
    private val networkStateManager: NetworkStateManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check network state
            val networkState = networkStateManager.getCurrentNetworkState()
            if (!networkState.isConnected) {
                return@withContext Result.retry()
            }
            
            // Get sync recommendation
            val syncRecommendation = networkStateManager.getSyncRecommendation()
            if (syncRecommendation == SyncRecommendation.WAIT) {
                return@withContext Result.retry()
            }
            
            // Force sync all pending operations
            offlineOperationManager.forceSyncAll()
            
            // Set progress
            setProgress(workDataOf("status" to "Syncing operations"))
            
            // Wait for sync to complete (with timeout)
            var attempts = 0
            val maxAttempts = 30 // 30 seconds timeout
            
            while (attempts < maxAttempts) {
                val stats = offlineOperationManager.queueStatistics.value
                if (stats.pendingOperations == 0) {
                    break
                }
                kotlinx.coroutines.delay(1000)
                attempts++
            }
            
            Result.success(workDataOf("syncCompleted" to true))
            
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }
    
    companion object {
        const val WORK_NAME = "offline_sync_work"
        
        fun createOneTimeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
        
        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
    }
}

@HiltWorker
class OfflineCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineOperationDao: OfflineOperationDao,
    private val conflictHistoryDao: ConflictHistoryDao,
    private val dataIntegrityManager: DataIntegrityManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7) // 7 days ago
            
            // Clean up successful operations older than 7 days
            val cleanedOperations = offlineOperationDao.cleanupSuccessfulOperations(cutoffTime)
            
            // Clean up failed operations that have exceeded max retries
            val cleanedFailedOps = offlineOperationDao.cleanupFailedOperations(cutoffTime)
            
            // Clean up cancelled operations
            val cleanedCancelled = offlineOperationDao.cleanupCancelledOperations()
            
            // Clean up old conflict history (older than 30 days)
            val conflictCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val cleanedConflicts = conflictHistoryDao.cleanupOldConflicts(conflictCutoff)
            
            // Clean up old integrity checks
            val cleanedIntegrity = dataIntegrityManager.cleanupOldIntegrityChecks(30)
            
            val totalCleaned = cleanedOperations + cleanedFailedOps + cleanedCancelled + 
                             cleanedConflicts + cleanedIntegrity
            
            Result.success(workDataOf(
                "cleanedOperations" to cleanedOperations,
                "cleanedFailedOps" to cleanedFailedOps,
                "cleanedCancelled" to cleanedCancelled,
                "cleanedConflicts" to cleanedConflicts,
                "cleanedIntegrity" to cleanedIntegrity,
                "totalCleaned" to totalCleaned
            ))
            
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }
    
    companion object {
        const val WORK_NAME = "offline_cleanup_work"
        
        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<OfflineCleanupWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .build()
        }
    }
}

@HiltWorker
class DataIntegrityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataIntegrityManager: DataIntegrityManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Perform comprehensive integrity scan
            val scanResult = dataIntegrityManager.performIntegrityScan("note")
            
            // Map DataIntegrityManager result to worker output fields
            return@withContext Result.success(workDataOf(
                "scannedCount" to scanResult.scannedCount,
                "invalidCount" to scanResult.invalidCount,
                "correctedCount" to scanResult.correctedCount,
                "scanDuration" to scanResult.scanDuration
            ))
            
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }
    
    companion object {
        const val WORK_NAME = "data_integrity_work"
        
        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<DataIntegrityWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .setInitialDelay(2, TimeUnit.HOURS) // Start 2 hours after app installation
                .build()
        }
    }
}

@HiltWorker
class ConflictResolutionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val conflictResolutionEngine: ConflictResolutionEngine,
    private val offlineOperationManager: OfflineOperationManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get operations that failed due to conflicts
            val failedOperations = offlineOperationManager.getOperationsByStatus(OperationStatus.FAILED)
                .first()
                .filter { it.errorMessage?.contains("conflict", ignoreCase = true) == true }
            
            var resolvedCount = 0
            var failedCount = 0
            
            for (operation in failedOperations.take(10)) { // Process up to 10 conflicts
                try {
                    // Attempt to retry the operation
                    val retrySuccess = offlineOperationManager.retryOperation(operation.id)
                    if (retrySuccess) {
                        resolvedCount++
                    } else {
                        failedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                }
            }
            
            Result.success(workDataOf(
                "resolvedCount" to resolvedCount,
                "failedCount" to failedCount,
                "processedCount" to (resolvedCount + failedCount)
            ))
            
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }
    
    companion object {
        const val WORK_NAME = "conflict_resolution_work"
        
        fun createOneTimeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<ConflictResolutionWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
    }
}

/**
 * Work scheduler for managing all offline-related background tasks
 */
class OfflineWorkScheduler @javax.inject.Inject constructor(
    private val workManager: WorkManager
) {
    
    fun scheduleAllPeriodicWork() {
        // Schedule periodic sync
        workManager.enqueueUniquePeriodicWork(
            OfflineSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            OfflineSyncWorker.createPeriodicRequest()
        )
        
        // Schedule periodic cleanup
        workManager.enqueueUniquePeriodicWork(
            OfflineCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            OfflineCleanupWorker.createPeriodicRequest()
        )
        
        // Schedule periodic integrity checks
        workManager.enqueueUniquePeriodicWork(
            DataIntegrityWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            DataIntegrityWorker.createPeriodicRequest()
        )
    }
    
    fun triggerImmediateSync() {
        workManager.enqueueUniqueWork(
            "${OfflineSyncWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            OfflineSyncWorker.createOneTimeRequest()
        )
    }
    
    fun triggerConflictResolution() {
        workManager.enqueueUniqueWork(
            ConflictResolutionWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND,
            ConflictResolutionWorker.createOneTimeRequest()
        )
    }
    
    fun cancelAllWork() {
        workManager.cancelUniqueWork(OfflineSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork(OfflineCleanupWorker.WORK_NAME)
        workManager.cancelUniqueWork(DataIntegrityWorker.WORK_NAME)
        workManager.cancelUniqueWork(ConflictResolutionWorker.WORK_NAME)
    }
    
    fun getWorkStatus(workName: String): kotlinx.coroutines.flow.Flow<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkLiveData(workName).asFlow()
    }
}
