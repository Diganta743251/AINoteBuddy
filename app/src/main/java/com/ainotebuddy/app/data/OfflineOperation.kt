package com.ainotebuddy.app.data

@Deprecated("Legacy model retained for reference; replaced by offline.OfflineOperationEntity and offline.OfflineOperation models.")
data class OfflineOperation(
    val id: String,
    val type: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int = 0,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val status: String = "PENDING",
    val errorMessage: String? = null,
    val networkRequirement: String = "ANY",
    val dependencies: String? = null,
    val estimatedSize: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val scheduledAt: Long? = null,
    val entityId: String,
    val entityType: String
)