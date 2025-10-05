package com.ainotebuddy.app.data

import androidx.room.*
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import com.ainotebuddy.app.offline.DataIntegrityEntity

/**
 * DAO for data integrity checks and validation
 */
@Dao
interface DataIntegrityDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(integrity: DataIntegrityEntity)

    @Query("SELECT * FROM data_integrity_checks WHERE entityType = :entityType AND entityId = :entityId ORDER BY validatedAt DESC LIMIT 1")
    suspend fun getLatestIntegrityCheck(entityType: String, entityId: String): DataIntegrityEntity?
    
    @Query("SELECT * FROM data_integrity_checks WHERE entityType = :entityType ORDER BY validatedAt DESC")
    suspend fun getIntegrityChecks(entityType: String): List<DataIntegrityEntity>
    
    @Query("SELECT * FROM data_integrity_checks WHERE validatedAt >= :since ORDER BY validatedAt DESC")
    suspend fun getIntegrityChecksSince(since: Long): List<DataIntegrityEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegrityCheck(check: DataIntegrityEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegrityChecks(checks: List<DataIntegrityEntity>)
    
    @Query("DELETE FROM data_integrity_checks WHERE validatedAt < :before")
    suspend fun deleteOldIntegrityChecks(before: Long): Int
    
    @Query("DELETE FROM data_integrity_checks WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteIntegrityChecksForEntity(entityType: String, entityId: String): Int
    
    @Query("SELECT COUNT(*) FROM data_integrity_checks WHERE entityType = :entityType AND isValid = false")
    suspend fun getInvalidCheckCount(entityType: String): Int
    
    @Query("SELECT COUNT(*) FROM data_integrity_checks WHERE entityType = :entityType")
    suspend fun getTotalCheckCount(entityType: String): Int
    
    @Query("SELECT * FROM data_integrity_checks WHERE isValid = false ORDER BY validatedAt DESC")
    suspend fun getFailedIntegrityChecks(): List<DataIntegrityEntity>
    
    // Methods used by DataIntegrityManager
    @Query("SELECT * FROM data_integrity_checks WHERE isValid = false")
    fun getInvalidEntities(): Flow<List<DataIntegrityEntity>>
    
    @Query("SELECT COUNT(*) FROM data_integrity_checks WHERE isValid = false")
    suspend fun getInvalidEntityCount(): Int
    
    @Query("UPDATE data_integrity_checks SET correctionApplied = true, correctionDetails = :details WHERE id = :id")
    suspend fun markCorrectionApplied(id: String, details: String)
    
    @Query("DELETE FROM data_integrity_checks WHERE validatedAt < :olderThan")
    suspend fun cleanupOldChecks(olderThan: Long): Int
    
    @Query("SELECT entityType, COUNT(*) as count FROM data_integrity_checks GROUP BY entityType")
    suspend fun getIntegrityCheckStatistics(): List<IntegrityStatistic>
}

/**
 * Data class for integrity statistics
 */
data class IntegrityStatistic(
    val entityType: String,
    val count: Int
)
