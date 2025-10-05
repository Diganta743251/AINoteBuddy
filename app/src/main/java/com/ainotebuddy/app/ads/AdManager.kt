package com.ainotebuddy.app.ads

import android.app.Activity
import android.content.Context
import com.ainotebuddy.app.BuildConfig
import com.ainotebuddy.app.ads.AdConstants
import com.ainotebuddy.app.billing.BillingManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdManager private constructor(private val context: Context) {
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _adMetrics = MutableStateFlow(mapOf<AdType, AdMetrics>())
    val adMetrics: StateFlow<Map<AdType, AdMetrics>> = _adMetrics.asStateFlow()
    
    // Ad managers
    lateinit var bannerAdManager: BannerAdManager
    lateinit var interstitialAdManager: InterstitialAdManager
    lateinit var rewardedAdManager: RewardedAdManager
    lateinit var appOpenAdManager: AppOpenAdManager
    
    // Session tracking
    private var interstitialCount = 0
    private var appOpenCount = 0
    private var sessionStartTime = System.currentTimeMillis()
    
    companion object {
        @Volatile
        private var INSTANCE: AdManager? = null
        
        fun getInstance(context: Context): AdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    fun initialize(onComplete: (Boolean) -> Unit = {}) {
        if (_isInitialized.value) {
            onComplete(true)
            return
        }
        
        // Configure test devices for development
        if (BuildConfig.DEBUG) {
            val testDeviceIds = listOf("YOUR_TEST_DEVICE_ID") // Add your test device ID
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }
        
        MobileAds.initialize(context) { initializationStatus ->
            val isSuccess = initializationStatus.adapterStatusMap.values.all { 
                it.initializationState == com.google.android.gms.ads.initialization.AdapterStatus.State.READY 
            }
            
            if (isSuccess) {
                initializeAdManagers()
                _isInitialized.value = true
            }
            
            onComplete(isSuccess)
        }
    }
    
    private fun initializeAdManagers() {
        bannerAdManager = BannerAdManager(context)
        interstitialAdManager = InterstitialAdManager(context) { 
            interstitialCount++
        }
        rewardedAdManager = RewardedAdManager(context)
        appOpenAdManager = AppOpenAdManager(context) { 
            appOpenCount++
        }
    }
    
    fun getAdUnitId(adType: AdType): String {
        return if (BuildConfig.DEBUG) {
            when (adType) {
                AdType.BANNER -> AdConstants.TEST_BANNER_AD_UNIT_ID
                AdType.INTERSTITIAL -> AdConstants.TEST_INTERSTITIAL_AD_UNIT_ID
                AdType.REWARDED -> AdConstants.TEST_REWARDED_AD_UNIT_ID
                AdType.APP_OPEN -> AdConstants.TEST_APP_OPEN_AD_UNIT_ID
                else -> AdConstants.TEST_BANNER_AD_UNIT_ID
            }
        } else {
            when (adType) {
                AdType.BANNER -> AdConstants.BANNER_AD_UNIT_ID
                AdType.INTERSTITIAL -> AdConstants.INTERSTITIAL_AD_UNIT_ID
                AdType.REWARDED -> AdConstants.REWARDED_AD_UNIT_ID
                AdType.REWARDED_INTERSTITIAL -> AdConstants.REWARDED_INTERSTITIAL_AD_UNIT_ID
                AdType.NATIVE_ADVANCED -> AdConstants.NATIVE_ADVANCED_AD_UNIT_ID
                AdType.APP_OPEN -> AdConstants.APP_OPEN_AD_UNIT_ID
            }
        }
    }
    
    fun canShowInterstitial(): Boolean {
        val timeSinceLastInterstitial = System.currentTimeMillis() - 
            (interstitialAdManager.lastShownTime ?: 0)
        return interstitialCount < AdConstants.MAX_INTERSTITIAL_PER_SESSION &&
                timeSinceLastInterstitial >= AdConstants.INTERSTITIAL_MIN_INTERVAL_MS
    }
    
    fun canShowAppOpen(): Boolean {
        return appOpenCount < AdConstants.MAX_APP_OPEN_PER_SESSION
    }
    
    fun updateMetrics(adType: AdType, metrics: AdMetrics) {
        val currentMetrics = _adMetrics.value.toMutableMap()
        currentMetrics[adType] = metrics
        _adMetrics.value = currentMetrics
    }
    
    fun resetSessionCounters() {
        interstitialCount = 0
        appOpenCount = 0
        sessionStartTime = System.currentTimeMillis()
    }
}