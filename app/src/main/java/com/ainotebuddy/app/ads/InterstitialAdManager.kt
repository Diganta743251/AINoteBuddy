package com.ainotebuddy.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InterstitialAdManager(
    private val context: Context,
    private val onAdShown: () -> Unit = {}
) {
    private val _adState = MutableStateFlow<AdState>(AdState.Loading)
    val adState: StateFlow<AdState> = _adState.asStateFlow()
    
    private var interstitialAd: InterstitialAd? = null
    var lastShownTime: Long? = null
        private set
    
    companion object {
        private const val TAG = "InterstitialAdManager"
    }
    
    init {
        loadAd()
    }
    
    fun loadAd() {
        if (interstitialAd != null) return
        
        _adState.value = AdState.Loading
        
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdManager.getInstance(context).getAdUnitId(AdType.INTERSTITIAL)
        
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: ${adError.message}")
                    interstitialAd = null
                    _adState.value = AdState.Failed(adError.message)
                }
                
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    _adState.value = AdState.Loaded
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "Interstitial ad clicked")
                        }
                        
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad dismissed")
                            interstitialAd = null
                            _adState.value = AdState.Dismissed
                            loadAd() // Preload next ad
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Failed to show interstitial ad: ${adError.message}")
                            interstitialAd = null
                            _adState.value = AdState.Failed(adError.message)
                        }
                        
                        override fun onAdImpression() {
                            Log.d(TAG, "Interstitial ad impression recorded")
                        }
                        
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad showed full screen content")
                            lastShownTime = System.currentTimeMillis()
                            _adState.value = AdState.Shown
                            onAdShown()
                        }
                    }
                }
            }
        )
    }
    
    fun showAd(activity: Activity, onAdClosed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad != null && AdManager.getInstance(context).canShowInterstitial()) {
            ad.show(activity)
            
            // Override the callback to include custom logic
            val originalCallback = ad.fullScreenContentCallback
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
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
        } else {
            Log.w(TAG, "Interstitial ad not ready or frequency cap reached")
            onAdClosed()
        }
    }
    
    fun isAdReady(): Boolean = interstitialAd != null
    
    fun destroy() {
        interstitialAd = null
    }
}