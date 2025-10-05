package com.ainotebuddy.app.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Network state manager for Enhanced Offline-First Architecture
 * Monitors network connectivity, bandwidth, and provides intelligent sync scheduling
 */
@Singleton
class NetworkStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _networkState = MutableStateFlow(NetworkState(false, ConnectionType.NONE))
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _bandwidthEstimate = MutableStateFlow(0L)
    val bandwidthEstimate: StateFlow<Long> = _bandwidthEstimate.asStateFlow()
    
    private val _syncRecommendation = MutableStateFlow(SyncRecommendation.WAIT)
    val syncRecommendation: StateFlow<SyncRecommendation> = _syncRecommendation.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastBandwidthTest = 0L
    private val bandwidthTestInterval = 300000L // 5 minutes
    
    init {
        startNetworkMonitoring()
        startBandwidthMonitoring()
        startSyncRecommendationEngine()
    }
    
    /**
     * Get current network state
     */
    suspend fun getCurrentNetworkState(): NetworkState {
        return withContext(Dispatchers.IO) {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            
            if (activeNetwork == null || networkCapabilities == null) {
                NetworkState(false, ConnectionType.NONE)
            } else {
                val connectionType = determineConnectionType(networkCapabilities)
                val isMetered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                val signalStrength = getSignalStrength(connectionType)
                val latency = measureLatency()
                val bandwidth = _bandwidthEstimate.value
                
                NetworkState(
                    isConnected = true,
                    connectionType = connectionType,
                    bandwidth = bandwidth,
                    isMetered = isMetered,
                    signalStrength = signalStrength,
                    latency = latency
                )
            }
        }
    }
    
    /**
     * Test network connectivity with a specific host
     */
    suspend fun testConnectivity(host: String = "8.8.8.8", port: Int = 53, timeoutMs: Int = 3000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Measure network latency
     */
    suspend fun measureLatency(host: String = "8.8.8.8", port: Int = 53): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 5000)
                }
                System.currentTimeMillis() - startTime
            } catch (e: Exception) {
                -1L // Indicates failure
            }
        }
    }
    
    /**
     * Estimate bandwidth by downloading a small test file
     */
    suspend fun estimateBandwidth(): Long {
        return withContext(Dispatchers.IO) {
            try {
                val testSize = 1024 * 10 // 10KB test
                val startTime = System.currentTimeMillis()
                
                // Simulate bandwidth test (in real implementation, download actual test data)
                delay(100) // Simulate network delay
                
                val duration = System.currentTimeMillis() - startTime
                if (duration > 0) {
                    (testSize * 1000) / duration // bytes per second
                } else {
                    0L
                }
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    /**
     * Get sync recommendation based on current network conditions
     */
    fun getSyncRecommendation(): SyncRecommendation {
        val currentState = _networkState.value
        
        return when {
            !currentState.isConnected -> SyncRecommendation.WAIT
            currentState.connectionType == ConnectionType.WIFI -> {
                when {
                    currentState.bandwidth > 1_000_000 -> SyncRecommendation.SYNC_ALL // > 1MB/s
                    currentState.bandwidth > 100_000 -> SyncRecommendation.SYNC_PRIORITY // > 100KB/s
                    else -> SyncRecommendation.SYNC_ESSENTIAL
                }
            }
            currentState.connectionType == ConnectionType.MOBILE_DATA -> {
                when {
                    currentState.isMetered -> SyncRecommendation.SYNC_ESSENTIAL
                    currentState.bandwidth > 500_000 -> SyncRecommendation.SYNC_PRIORITY // > 500KB/s
                    else -> SyncRecommendation.SYNC_ESSENTIAL
                }
            }
            else -> SyncRecommendation.SYNC_ESSENTIAL
        }
    }
    
    /**
     * Check if operation should be executed based on network requirements
     */
    fun shouldExecuteOperation(networkRequirement: NetworkRequirement): Boolean {
        val currentState = _networkState.value
        
        return when (networkRequirement) {
            NetworkRequirement.ANY -> true
            NetworkRequirement.WIFI_ONLY -> currentState.isConnected && currentState.connectionType == ConnectionType.WIFI
            NetworkRequirement.MOBILE_DATA_OK -> currentState.isConnected
        }
    }
    
    /**
     * Get optimal batch size for sync operations based on network conditions
     */
    fun getOptimalBatchSize(): Int {
        val currentState = _networkState.value
        val recommendation = getSyncRecommendation()
        
        return when (recommendation) {
            SyncRecommendation.SYNC_ALL -> 50
            SyncRecommendation.SYNC_PRIORITY -> 20
            SyncRecommendation.SYNC_ESSENTIAL -> 5
            SyncRecommendation.WAIT -> 0
        }
    }
    
    /**
     * Get recommended sync interval based on network conditions
     */
    fun getRecommendedSyncInterval(): Long {
        val currentState = _networkState.value
        
        return when {
            !currentState.isConnected -> 300000L // 5 minutes when offline
            currentState.connectionType == ConnectionType.WIFI -> 30000L // 30 seconds on WiFi
            currentState.connectionType == ConnectionType.MOBILE_DATA && !currentState.isMetered -> 60000L // 1 minute on unlimited mobile
            currentState.connectionType == ConnectionType.MOBILE_DATA && currentState.isMetered -> 300000L // 5 minutes on metered mobile
            else -> 120000L // 2 minutes default
        }
    }
    
    /**
     * Start network monitoring
     */
    private fun startNetworkMonitoring() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    updateNetworkState()
                }
            }
            
            override fun onLost(network: Network) {
                scope.launch {
                    updateNetworkState()
                }
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                scope.launch {
                    updateNetworkState()
                }
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback?.let { callback ->
            connectivityManager.registerNetworkCallback(networkRequest, callback)
        }
        
        // Initial state update
        scope.launch {
            updateNetworkState()
        }
    }
    
    /**
     * Start bandwidth monitoring
     */
    private fun startBandwidthMonitoring() {
        scope.launch {
            while (true) {
                try {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBandwidthTest > bandwidthTestInterval) {
                        val networkState = getCurrentNetworkState()
                        if (networkState.isConnected) {
                            val bandwidth = estimateBandwidth()
                            _bandwidthEstimate.value = bandwidth
                            lastBandwidthTest = currentTime
                        }
                    }
                    delay(60000) // Check every minute
                } catch (e: Exception) {
                    delay(120000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Start sync recommendation engine
     */
    private fun startSyncRecommendationEngine() {
        scope.launch {
            networkState.collect { state ->
                val recommendation = getSyncRecommendation()
                _syncRecommendation.value = recommendation
            }
        }
    }
    
    /**
     * Update network state
     */
    private suspend fun updateNetworkState() {
        val newState = getCurrentNetworkState()
        _networkState.value = newState
    }
    
    /**
     * Determine connection type from network capabilities
     */
    private fun determineConnectionType(networkCapabilities: NetworkCapabilities): ConnectionType {
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE_DATA
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }
    
    /**
     * Get signal strength based on connection type
     */
    private fun getSignalStrength(connectionType: ConnectionType): Int {
        return when (connectionType) {
            ConnectionType.MOBILE_DATA -> {
                try {
                    // This is a simplified approach - real implementation would use SignalStrength
                    val signalStrength = telephonyManager.signalStrength
                    signalStrength?.level?.let { level ->
                        // Convert level (0-4) to percentage (0-100)
                        (level * 25).coerceIn(0, 100)
                    } ?: 50 // Default to 50% if unknown
                } catch (e: Exception) {
                    50 // Default value
                }
            }
            ConnectionType.WIFI -> {
                // For WiFi, we'd need WifiManager to get RSSI
                // For now, return a default value
                75
            }
            ConnectionType.ETHERNET -> 100 // Assume full strength for wired
            else -> 0
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        networkCallback?.let { callback ->
            connectivityManager.unregisterNetworkCallback(callback)
        }
        scope.cancel()
    }
}

/**
 * Sync recommendation based on network conditions
 */
enum class SyncRecommendation {
    SYNC_ALL,       // Excellent network - sync everything
    SYNC_PRIORITY,  // Good network - sync priority operations
    SYNC_ESSENTIAL, // Poor network - sync only essential operations
    WAIT           // No network - wait for better conditions
}

/**
 * Network quality assessment
 */
data class NetworkQuality(
    val score: Int, // 0-100 quality score
    val category: NetworkQualityCategory,
    val factors: List<String> // Factors affecting quality
)

enum class NetworkQualityCategory {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNAVAILABLE
}

/**
 * Bandwidth monitor for tracking data usage
 */
class BandwidthMonitor {
    private var bytesTransferred = 0L
    private var startTime = System.currentTimeMillis()
    
    fun recordDataTransfer(bytes: Long) {
        bytesTransferred += bytes
    }
    
    fun getCurrentBandwidth(): Long {
        val duration = System.currentTimeMillis() - startTime
        return if (duration > 0) {
            (bytesTransferred * 1000) / duration // bytes per second
        } else {
            0L
        }
    }
    
    fun reset() {
        bytesTransferred = 0L
        startTime = System.currentTimeMillis()
    }
}

/**
 * Network state predictor for anticipating connectivity changes
 */
class NetworkStatePredictor {
    private val stateHistory = mutableListOf<NetworkState>()
    private val maxHistorySize = 100
    
    fun recordState(state: NetworkState) {
        stateHistory.add(state)
        if (stateHistory.size > maxHistorySize) {
            stateHistory.removeAt(0)
        }
    }
    
    fun predictNextState(): NetworkState? {
        if (stateHistory.size < 3) return null
        
        // Simple prediction based on recent patterns
        val recentStates = stateHistory.takeLast(5)
        val mostCommonType = recentStates.groupBy { it.connectionType }
            .maxByOrNull { it.value.size }?.key
        
        return mostCommonType?.let { type ->
            val avgBandwidth = recentStates.filter { it.connectionType == type }
                .map { it.bandwidth }.average().toLong()
            
            NetworkState(
                isConnected = type != ConnectionType.NONE,
                connectionType = type,
                bandwidth = avgBandwidth
            )
        }
    }
    
    fun getConnectivityPattern(): ConnectivityPattern {
        if (stateHistory.size < 10) return ConnectivityPattern.UNKNOWN
        
        val connectivityChanges = stateHistory.zipWithNext { current, next ->
            current.isConnected != next.isConnected
        }.count { it }
        
        return when {
            connectivityChanges == 0 -> ConnectivityPattern.STABLE
            connectivityChanges < 3 -> ConnectivityPattern.OCCASIONAL_DROPS
            connectivityChanges < 6 -> ConnectivityPattern.UNSTABLE
            else -> ConnectivityPattern.VERY_UNSTABLE
        }
    }
}

enum class ConnectivityPattern {
    STABLE,
    OCCASIONAL_DROPS,
    UNSTABLE,
    VERY_UNSTABLE,
    UNKNOWN
}
