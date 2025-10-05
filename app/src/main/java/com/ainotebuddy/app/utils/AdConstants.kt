package com.ainotebuddy.app.utils

object AdConstants {
    const val APP_ID = "ca-app-pub-4084721334097026~2124246597"
    
    // Production Ad Unit IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-4084721334097026/9400101389"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4084721334097026/8550054140"
    const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4084721334097026/7153211990"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-4084721334097026/7895448027"
    const val NATIVE_ADVANCED_AD_UNIT_ID = "ca-app-pub-4084721334097026/8370090746"
    const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-4084721334097026/5269284686"
    
    // Test Ad Unit IDs (for development)
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"
    
    // Ad refresh intervals
    const val BANNER_REFRESH_INTERVAL_MS = 30000L // 30 seconds
    const val INTERSTITIAL_MIN_INTERVAL_MS = 180000L // 3 minutes
    
    // Frequency capping
    const val MAX_INTERSTITIAL_PER_SESSION = 5
    const val MAX_APP_OPEN_PER_SESSION = 3
}