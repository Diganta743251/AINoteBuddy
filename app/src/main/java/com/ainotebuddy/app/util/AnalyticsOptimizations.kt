package com.ainotebuddy.app.util

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced analytics optimization and performance monitoring
 */

@Singleton
class AnalyticsOptimizations @Inject constructor(
    private val context: Context
) {
    
    private val eventQueue = ConcurrentHashMap<String, MutableList<AnalyticsEvent>>()
    private val performanceMetrics = ConcurrentHashMap<String, PerfMetric>()
    private val batchSize = 50
    private val flushInterval = 30000L // 30 seconds
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        startBatchProcessor()
        startPerformanceMonitoring()
    }
    
    fun trackEvent(event: AnalyticsEvent) {
        val category = event.category
        eventQueue.getOrPut(category) { mutableListOf() }.add(event)
        
        if (eventQueue[category]?.size ?: 0 >= batchSize) {
            scope.launch {
                flushEvents(category)
            }
        }
    }
    
    fun trackPerformance(operation: String, duration: Long, success: Boolean = true) {
        val metric = performanceMetrics.getOrPut(operation) {
            PerfMetric(operation)
        }
        
        metric.addMeasurement(duration, success)
    }
    
    suspend fun trackAsyncOperation(
        operation: String,
        block: suspend () -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        var success = true
        
        try {
            block()
        } catch (e: Exception) {
            success = false
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            trackPerformance(operation, duration, success)
        }
    }
    
    private fun startBatchProcessor() {
        scope.launch {
            while (true) {
                delay(flushInterval)
                flushAllEvents()
            }
        }
    }
    
    private fun startPerformanceMonitoring() {
        scope.launch {
            while (true) {
                delay(60000L) // Check every minute
                analyzePerformanceMetrics()
            }
        }
    }
    
    private suspend fun flushEvents(category: String) {
        val events = eventQueue[category]?.toList() ?: return
        eventQueue[category]?.clear()
        
        try {
            // Process events batch
            processEventBatch(category, events)
        } catch (e: Exception) {
            // Re-queue events if processing fails
            eventQueue.getOrPut(category) { mutableListOf() }.addAll(events)
        }
    }
    
    private suspend fun flushAllEvents() {
        eventQueue.keys.forEach { category ->
            flushEvents(category)
        }
    }
    
    private suspend fun processEventBatch(category: String, events: List<AnalyticsEvent>) {
        withContext(Dispatchers.IO) {
            // Optimize events before sending
            val optimizedEvents = optimizeEventBatch(events)
            
            // Send to analytics service
            sendToAnalyticsService(category, optimizedEvents)
            
            // Update local metrics
            updateLocalMetrics(optimizedEvents)
        }
    }
    
    private fun optimizeEventBatch(events: List<AnalyticsEvent>): List<AnalyticsEvent> {
        // Remove duplicate events
        val deduplicatedEvents = removeDuplicateEvents(events)
        
        // Aggregate similar events
        val aggregatedEvents = aggregateSimilarEvents(deduplicatedEvents)
        
        // Compress event data
        return compressEventData(aggregatedEvents)
    }
    
    private fun removeDuplicateEvents(events: List<AnalyticsEvent>): List<AnalyticsEvent> {
        return events.distinctBy { "${it.name}_${it.timestamp}_${it.properties}" }
    }
    
    private fun aggregateSimilarEvents(events: List<AnalyticsEvent>): List<AnalyticsEvent> {
        return events.groupBy { it.name }
            .flatMap { (name, eventGroup) ->
                if (eventGroup.size > 1 && shouldAggregate(name)) {
                    listOf(createAggregatedEvent(name, eventGroup))
                } else {
                    eventGroup
                }
            }
    }
    
    private fun shouldAggregate(eventName: String): Boolean {
        return eventName in listOf(
            "page_view",
            "button_click",
            "scroll_event",
            "focus_event"
        )
    }
    
    private fun createAggregatedEvent(name: String, events: List<AnalyticsEvent>): AnalyticsEvent {
        val aggregatedProperties = mutableMapOf<String, Any>()
        aggregatedProperties["count"] = events.size
        aggregatedProperties["first_timestamp"] = events.minOf { it.timestamp }
        aggregatedProperties["last_timestamp"] = events.maxOf { it.timestamp }
        
        // Aggregate numeric properties
        events.forEach { event ->
            event.properties.forEach { (key, value) ->
                when (value) {
                    is Number -> {
                        val currentSum = (aggregatedProperties[key] as? Number)?.toDouble() ?: 0.0
                        aggregatedProperties[key] = currentSum + value.toDouble()
                    }
                    is String -> {
                        if (!aggregatedProperties.containsKey(key)) {
                            aggregatedProperties[key] = value
                        }
                    }
                }
            }
        }
        
        return AnalyticsEvent(
            name = "${name}_aggregated",
            category = events.first().category,
            timestamp = events.last().timestamp,
            properties = aggregatedProperties
        )
    }
    
    private fun compressEventData(events: List<AnalyticsEvent>): List<AnalyticsEvent> {
        return events.map { event ->
            val compressedProperties = event.properties.mapValues { (_, value) ->
                when (value) {
                    is String -> if (value.length > 100) value.take(100) + "..." else value
                    else -> value
                }
            }
            
            event.copy(properties = compressedProperties)
        }
    }
    
    private suspend fun sendToAnalyticsService(category: String, events: List<AnalyticsEvent>) {
        // Implementation would send to actual analytics service
        // For now, just log the batch
        println("Sending ${events.size} events in category $category")
    }
    
    private fun updateLocalMetrics(events: List<AnalyticsEvent>) {
        events.forEach { event ->
            val metric = performanceMetrics.getOrPut("analytics_events") {
                PerfMetric("analytics_events")
            }
            metric.incrementCounter()
        }
    }
    
    private fun analyzePerformanceMetrics() {
        performanceMetrics.values.forEach { metric ->
            if (metric.isPerformanceDegraded()) {
                handlePerformanceDegradation(metric)
            }
        }
    }
    
    private fun handlePerformanceDegradation(metric: PerfMetric) {
        when (metric.operation) {
            "analytics_events" -> reduceBatchSize()
            "database_operations" -> optimizeDatabaseQueries()
            "ui_rendering" -> reduceAnimationComplexity()
        }
    }
    
    private fun reduceBatchSize() {
        // Temporarily reduce batch size to improve performance
        println("Reducing analytics batch size due to performance issues")
    }
    
    private fun optimizeDatabaseQueries() {
        println("Optimizing database queries due to performance issues")
    }
    
    private fun reduceAnimationComplexity() {
        println("Reducing animation complexity due to performance issues")
    }
    
    fun getPerformanceReport(): PerformanceReport {
        val metrics = performanceMetrics.values.map { it.toReport() }
        return PerformanceReport(
            timestamp = System.currentTimeMillis(),
            metrics = metrics,
            overallScore = calculateOverallPerformanceScore(metrics)
        )
    }

    // Avoid enum name collision with AnalyticsEvents.PerformanceMetric by using AnalyticsPerfMetric alias
    @Deprecated("Use AnalyticsEvents.PerformanceMetric for public analytics enums; this class is internal.")
    private class AnalyticsPerfMetric(val operationName: String)
    
    private fun calculateOverallPerformanceScore(metrics: List<MetricReport>): Float {
        if (metrics.isEmpty()) return 1.0f
        
        return metrics.map { it.performanceScore }.average().toFloat()
    }
}

data class AnalyticsEvent(
    val name: String,
    val category: String,
    val timestamp: Long,
    val properties: Map<String, Any>
)

class PerfMetric(val operation: String) {
    private val measurements = mutableListOf<Long>()
    private val successCount = AtomicLong(0)
    private val failureCount = AtomicLong(0)
    private val counter = AtomicLong(0)
    
    fun addMeasurement(duration: Long, success: Boolean) {
        synchronized(measurements) {
            measurements.add(duration)
            if (measurements.size > 100) {
                measurements.removeFirst()
            }
        }
        
        if (success) {
            successCount.incrementAndGet()
        } else {
            failureCount.incrementAndGet()
        }
    }
    
    fun incrementCounter() {
        counter.incrementAndGet()
    }
    
    fun isPerformanceDegraded(): Boolean {
        synchronized(measurements) {
            if (measurements.size < 10) return false
            
            val recentAverage = measurements.takeLast(10).average()
            val overallAverage = measurements.average()
            
            return recentAverage > overallAverage * 1.5
        }
    }
    
    fun toReport(): MetricReport {
        synchronized(measurements) {
            val totalOperations = successCount.get() + failureCount.get()
            val successRate = if (totalOperations > 0) {
                successCount.get().toFloat() / totalOperations
            } else 1.0f
            
            return MetricReport(
                operation = operation,
                averageDuration = measurements.average(),
                successRate = successRate,
                totalOperations = totalOperations,
                performanceScore = calculatePerformanceScore(measurements.average(), successRate)
            )
        }
    }
    
    private fun calculatePerformanceScore(avgDuration: Double, successRate: Float): Float {
        val durationScore = when {
            avgDuration < 100 -> 1.0f
            avgDuration < 500 -> 0.8f
            avgDuration < 1000 -> 0.6f
            avgDuration < 2000 -> 0.4f
            else -> 0.2f
        }
        
        return (durationScore * 0.7f + successRate * 0.3f).coerceIn(0.0f, 1.0f)
    }
}

data class PerformanceReport(
    val timestamp: Long,
    val metrics: List<MetricReport>,
    val overallScore: Float
)

data class MetricReport(
    val operation: String,
    val averageDuration: Double,
    val successRate: Float,
    val totalOperations: Long,
    val performanceScore: Float
)