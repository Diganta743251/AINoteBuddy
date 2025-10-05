package com.ainotebuddy.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ainotebuddy.app.ads.AdManager
import com.ainotebuddy.app.ads.AdState
import com.google.android.gms.ads.AdSize

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.SMART_BANNER
) {
    val context = LocalContext.current
    val adManager = remember { AdManager.getInstance(context) }
    val bannerAdManager = remember { adManager.bannerAdManager }
    val adState by bannerAdManager.adState.collectAsState()
    
    // Only show banner if ad is loaded or loading
    when (adState) {
        is AdState.Loading, is AdState.Loaded, is AdState.Shown -> {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .height(
                        when (adSize) {
                            AdSize.BANNER -> 50.dp
                            AdSize.LARGE_BANNER -> 100.dp
                            AdSize.MEDIUM_RECTANGLE -> 250.dp
                            AdSize.SMART_BANNER -> 50.dp
                            else -> 50.dp
                        }
                    ),
                factory = { context ->
                    bannerAdManager.createBannerAd(adSize).also {
                        bannerAdManager.loadAd()
                    }
                }
            )
        }
        else -> {
            // Don't show anything if ad failed to load
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            bannerAdManager.pause()
        }
    }
}