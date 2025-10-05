package com.ainotebuddy.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RewardedAdManager(private val context: Context) {
    private val _adState = MutableStateFlow<AdState>(AdState.Loading)
    val adState: StateFlow<AdState> = _adState.asStateFlow()
    
    private var rewardedAd: RewardedAd? = null
    
    companion object {
        private const val TAG = "RewardedAdManager"
    }
    
    init {
        loadAd()
    }
    
    fun loadAd() {
        if (rewardedAd != null) return
        
        _adState.value = AdState.Loading
        
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdManager.getInstance(context).getAdUnitId(AdType.REWARDED)
        
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Failed to load rewarded ad: ${adError.message}")
                    rewardedAd = null
                    _adState.value = AdState.Failed(adError.message)
                }
                
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    _adState.value = AdState.Loaded
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "Rewarded ad clicked")
                        }
                        
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Rewarded ad dismissed")
                            rewardedAd = null
                            _adState.value = AdState.Dismissed
                            loadAd() // Preload next ad
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Failed to show rewarded ad: ${adError.message}")
                            rewardedAd = null
                            _adState.value = AdState.Failed(adError.message)
                        }
                        
                        override fun onAdImpression() {
                            Log.d(TAG, "Rewarded ad impression recorded")
                        }
                        
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Rewarded ad showed full screen content")
                            _adState.value = AdState.Shown
                        }
                    }
                }
            }
        )
    }
    
    fun showAd(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int, rewardType: String) -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "User earned reward: $rewardAmount $rewardType")
                _adState.value = AdState.Rewarded(rewardAmount, rewardType)
                onRewardEarned(rewardAmount, rewardType)
            }
            
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
            Log.w(TAG, "Rewarded ad not ready")
            onAdClosed()
        }
    }
    
    fun isAdReady(): Boolean = rewardedAd != null
    
    fun destroy() {
        rewardedAd = null
    }
}