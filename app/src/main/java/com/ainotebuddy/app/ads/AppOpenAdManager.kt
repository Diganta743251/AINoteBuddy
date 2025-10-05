package com.ainotebuddy.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class AppOpenAdManager(
    private val context: Context,
    private val onAdShown: () -> Unit = {}
) {
    private val _adState = MutableStateFlow<AdState>(AdState.Loading)
    val adState: StateFlow<AdState> = _adState.asStateFlow()
    
    private var appOpenAd: AppOpenAd? = null
    private var loadTime: Long = 0
    
    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val AD_TIMEOUT = 4 * 3600 * 1000L // 4 hours in milliseconds
    }
    
    init {
        loadAd()
    }
    
    fun loadAd() {
        if (isAdAvailable()) return
        
        _adState.value = AdState.Loading
        
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdManager.getInstance(context).getAdUnitId(AdType.APP_OPEN)
        
        AppOpenAd.load(
            context,
            adUnitId,
            adRequest,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Failed to load app open ad: ${adError.message}")
                    appOpenAd = null
                    _adState.value = AdState.Failed(adError.message)
                }
                
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App open ad loaded successfully")
                    appOpenAd = ad
                    loadTime = Date().time
                    _adState.value = AdState.Loaded
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "App open ad clicked")
                        }
                        
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "App open ad dismissed")
                            appOpenAd = null
                            _adState.value = AdState.Dismissed
                            loadAd() // Preload next ad
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Failed to show app open ad: ${adError.message}")
                            appOpenAd = null
                            _adState.value = AdState.Failed(adError.message)
                        }
                        
                        override fun onAdImpression() {
                            Log.d(TAG, "App open ad impression recorded")
                        }
                        
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "App open ad showed full screen content")
                            _adState.value = AdState.Shown
                            onAdShown()
                        }
                    }
                }
            }
        )
    }
    
    fun showAdIfAvailable(activity: Activity, onAdClosed: () -> Unit = {}) {
        if (!isAdAvailable() || !AdManager.getInstance(context).canShowAppOpen()) {
            Log.w(TAG, "App open ad not available or frequency cap reached")
            onAdClosed()
            return
        }
        
        appOpenAd?.show(activity)
        
        // Override the callback to include custom logic
        val originalCallback = appOpenAd?.fullScreenContentCallback
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                originalCallback?.onAdClicked()
            }
            
            override fun onAdDismissedFullScreenContent() {
                originalCallback?.onAdDismissedFullScreenContent()
                onAdClosed()
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                originalCallback?.onAdFailedToShowFullScreenContent(adError)
                onAdClosed()
            }
            
            override fun onAdImpression() {
                originalCallback?.onAdImpression()
            }
            
            override fun onAdShowedFullScreenContent() {
                originalCallback?.onAdShowedFullScreenContent()
            }
        }
    }
    
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }
    
    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = Date().time - loadTime
        return dateDifference < AD_TIMEOUT
    }
    
    fun destroy() {
        appOpenAd = null
    }
}