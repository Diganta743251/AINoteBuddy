package com.ainotebuddy.app.ads

sealed class AdState {
    object Loading : AdState()
    object Loaded : AdState()
    data class Failed(val error: String) : AdState()
    object Shown : AdState()
    object Dismissed : AdState()
    data class Rewarded(val rewardAmount: Int, val rewardType: String) : AdState()
}

data class AdMetrics(
    val loadTime: Long = 0L,
    val showTime: Long = 0L,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val revenue: Double = 0.0
)

enum class AdType {
    BANNER,
    INTERSTITIAL,
    REWARDED,
    REWARDED_INTERSTITIAL,
    NATIVE_ADVANCED,
    APP_OPEN
}