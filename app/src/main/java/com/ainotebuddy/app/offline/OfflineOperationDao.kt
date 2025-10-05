package com.ainotebuddy.app.offline

import androidx.room.*
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for offline operations
 * Provides comprehensive database operations for the Enhanced Offline-First Architecture
 */
@Dao
interface OfflineOperationDao {
    
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: OfflineOperationEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<OfflineOperationEntity>)
    
    @Update
    suspend fun update(operation: OfflineOperationEntity)
    
    @Delete
    suspend fun delete(operation: OfflineOperationEntity)
    
    @Query("DELETE FROM offline_operations WHERE id = :operationId")
    suspend fun deleteById(operationId: String)
    
    @Query("DELETE FROM offline_operations WHERE status = :status")
    suspend fun deleteByStatus(status: String)
    
    // Query operations
    @Query("SELECT * FROM offline_operations WHERE id = :operationId")
    suspend fun getById(operationId: String): OfflineOperationEntity?
    
    @Query("SELECT * FROM offline_operations WHERE status = 'FAILED' ORDER BY timestamp DESC")
    suspend fun getFailedOperations(): List<OfflineOperationEntity>
    
    @Query("SELECT * FROM offline_operations WHERE entityId = :entityId AND status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingOperationsByEntityId(entityId: String): List<OfflineOperationEntity>
    
    @Query("SELECT * FROM offline_operations WHERE status = :status ORDER BY priority ASC, timestamp ASC")
    fun getOperationsByStatus(status: String): Flow<List<OfflineOperationEntity>>
    
    @Query("SELECT * FROM offline_operations WHERE status = 'PENDING' ORDER BY priority ASC, scheduledAt ASC")
    fun getPendingOperationsFlow(): Flow<List<OfflineOperationEntity>>
    
    @Query("SELECT * FROM offline_operations WHERE status = 'FAILED' AND retryCount < maxRetries ORDER BY priority ASC, lastAttemptAt ASC")
    fun getRetryableOperations(): Flow<List<OfflineOperationEntity>>
    
    @Query("SELECT * FROM offline_operations WHERE priority = :priority ORDER BY timestamp ASC")
    fun getOperationsByPriority(priority: Int): Flow<List<OfflineOperationEntity>>
    
    @Query("SELECT * FROM offline_operations WHERE type = :type ORDER BY timestamp ASC")
    fun getOperationsByType(type: String): Flow<List<OfflineOperationEntity>>
    
    @Query("SELECT * FROM offline_operations WHERE networkRequirement = :requirement ORDER BY priority ASC, timestamp ASC")
    fun getOperationsByNetworkRequirement(requirement: String): Flow<List<OfflineOperationEntity>>
    
    // Advanced queries
    @Query("""
        SELECT * FROM offline_operations 
        WHERE status = 'PENDING' 
        AND (networkRequirement = 'ANY' OR networkRequirement = :currentNetwork)
        AND scheduledAt <= :currentTime
        ORDER BY priority ASC, timestamp ASC 
        LIMIT :limit
    """)
    suspend fun getExecutableOperations(
        currentNetwork: String, 
        currentTime: Long, 
        limit: Int = 10
    ): List<OfflineOperationEntity>
    
    @Query("""
        SELECT * FROM offline_operations 
        WHERE status = 'FAILED' 
        AND retryCount < maxRetries
        AND (lastAttemptAt IS NULL OR lastAttemptAt < :retryAfter)
        ORDER BY priority ASC, lastAttemptAt ASC
        LIMIT :limit
    """)
    suspend fun getOperationsForRetry(retryAfter: Long, limit: Int = 5): List<OfflineOperationEntity>
    
    @Query("SELECT COUNT(*) FROM offline_operations WHERE status = :status")
    suspend fun getOperationCountByStatus(status: String): Int
    
    @Query("SELECT COUNT(*) FROM offline_operations WHERE status = 'PENDING'")
    suspend fun getPendingOperationCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_operations WHERE status = 'FAILED'")
    suspend fun getFailedOperationCount(): Int
    
    @Query("SELECT SUM(estimatedSize) FROM offline_operations WHERE status = 'PENDING'")
    suspend fun getPendingDataSize(): Long?
    
    @Query("""
        SELECT * FROM offline_operations 
        WHERE dependencies LIKE '%' || :dependencyId || '%'
        ORDER BY priority ASC, timestamp ASC
    """)
    suspend fun getOperationsDependingOn(dependencyId: String): List<OfflineOperationEntity>
    
    // Batch operations
    @Query("UPDATE offline_operations SET status = :newStatus WHERE status = :oldStatus")
    suspend fun updateStatusBatch(oldStatus: String, newStatus: String): Int
    
    @Query("UPDATE offline_operations SET retryCount = retryCount + 1, lastAttemptAt = :attemptTime WHERE id = :operationId")
    suspend fun incrementRetryCount(operationId: String, attemptTime: Long)
    
    @Query("UPDATE offline_operations SET status = :status, errorMessage = :errorMessage WHERE id = :operationId")
    suspend fun updateOperationResult(operationId: String, status: String, errorMessage: String?)
    
    // Cleanup operations
    @Query("DELETE FROM offline_operations WHERE status = 'SUCCESS' AND timestamp < :olderThan")
    suspend fun cleanupSuccessfulOperations(olderThan: Long): Int
    
    @Query("DELETE FROM offline_operations WHERE status = 'FAILED' AND retryCount >= maxRetries AND timestamp < :olderThan")
    suspend fun cleanupFailedOperations(olderThan: Long): Int
    
    @Query("DELETE FROM offline_operations WHERE status = 'CANCELLED'")
    suspend fun cleanupCancelledOperations(): Int
    
    // Statistics and monitoring
    @Query("""
        SELECT 
            status,
            COUNT(*) as count,
            AVG(retryCount) as avgRetries,
            SUM(estimatedSize) as totalSize
        FROM offline_operations 
        GROUP BY status
    """)
    suspend fun getOperationStatistics(): List<OperationStatistics>
    
    @Query("""
        SELECT 
            type,
            COUNT(*) as count,
            AVG(CASE WHEN status = 'SUCCESS' THEN 1.0 ELSE 0.0 END) as successRate
        FROM offline_operations 
        GROUP BY type
    """)
    suspend fun getOperationSuccessRates(): List<OperationTypeStatistics>
}


// Data classes for statistics queries
data class OperationStatistics(
    val status: String,
    val count: Int,
    val avgRetries: Double,
    val totalSize: Long
)

data class OperationTypeStatistics(
    val type: String,
    val count: Int,
    val successRate: Double
)
