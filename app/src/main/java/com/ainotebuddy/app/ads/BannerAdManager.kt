package com.ainotebuddy.app.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BannerAdManager(private val context: Context) {
    
    private val _adState = MutableStateFlow<AdState>(AdState.Loading)
    val adState: StateFlow<AdState> = _adState.asStateFlow()
    
    private var currentAdView: AdView? = null
    private val adManager = AdManager.getInstance(context)
    
    fun createBannerAd(adSize: AdSize = AdSize.SMART_BANNER): AdView {
        val adView = AdView(context).apply {
            setAdSize(adSize)
            adUnitId = adManager.getAdUnitId(AdType.BANNER)
            adListener = createAdListener()
        }
        
        currentAdView = adView
        return adView
    }
    
    fun loadAd() {
        currentAdView?.let { adView ->
            _adState.value = AdState.Loading
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        }
    }
    
    private fun createAdListener() = object : AdListener() {
        override fun onAdLoaded() {
            super.onAdLoaded()
            _adState.value = AdState.Loaded
            Log.d("BannerAd", "Banner ad loaded successfully")
        }
        
        override fun onAdFailedToLoad(error: LoadAdError) {
            super.onAdFailedToLoad(error)
            _adState.value = AdState.Failed(error.message)
            Log.e("BannerAd", "Banner ad failed to load: ${error.message}")
        }
        
        override fun onAdOpened() {
            super.onAdOpened()
            _adState.value = AdState.Shown
            Log.d("BannerAd", "Banner ad opened")
        }
        
        override fun onAdClosed() {
            super.onAdClosed()
            _adState.value = AdState.Dismissed
            Log.d("BannerAd", "Banner ad closed")
        }
        
        override fun onAdClicked() {
            super.onAdClicked()
            Log.d("BannerAd", "Banner ad clicked")
        }
    }
    
    fun destroy() {
        currentAdView?.destroy()
        currentAdView = null
    }
    
    fun pause() {
        currentAdView?.pause()
    }
    
    fun resume() {
        currentAdView?.resume()
    }
}