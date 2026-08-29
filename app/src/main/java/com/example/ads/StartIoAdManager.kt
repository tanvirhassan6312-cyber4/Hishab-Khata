package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener

object StartIoAdManager {
    const val APP_ID = "207916790"
    private const val TAG = "StartIoAds"

    private var isInitialized = false
    private var interstitialAd: StartAppAd? = null

    /**
     * Initialize Start.io SDK with the user's Start.io App ID: 207916790
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Initialize StartAppSDK with the provided App ID
            StartAppSDK.init(context, APP_ID, false)
            StartAppSDK.enableReturnAds(false)
            StartAppAd.disableSplash()
            isInitialized = true
            Log.d(TAG, "Start.io SDK initialized successfully with App ID: $APP_ID")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Start.io SDK", e)
        }
    }

    /**
     * Show an interstitial ad on key user milestones (e.g. sale completed, memo saved)
     */
    fun showInterstitialAd(activity: Activity?, onAdClosed: (() -> Unit)? = null) {
        if (activity == null) {
            onAdClosed?.invoke()
            return
        }

        try {
            if (interstitialAd == null) {
                interstitialAd = StartAppAd(activity)
            }

            interstitialAd?.showAd(object : AdDisplayListener {
                override fun adHidden(ad: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "Interstitial Ad closed")
                    onAdClosed?.invoke()
                }

                override fun adDisplayed(ad: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "Interstitial Ad displayed")
                }

                override fun adClicked(ad: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "Interstitial Ad clicked")
                }

                override fun adNotDisplayed(ad: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "Interstitial Ad not displayed")
                    onAdClosed?.invoke()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying Start.io Interstitial Ad", e)
            onAdClosed?.invoke()
        }
    }
}

/**
 * Reusable Jetpack Compose component for Start.io Banner Ad
 */
@Composable
fun StartIoBannerAd(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 4.dp)
            .testTag("start_io_banner_ad"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    try {
                        val banner = Banner(context, object : BannerListener {
                            override fun onReceiveAd(p0: android.view.View?) {
                                Log.d("StartIoAds", "Banner Ad received successfully")
                            }

                            override fun onFailedToReceiveAd(p0: android.view.View?) {
                                Log.d("StartIoAds", "Banner Ad failed to receive")
                            }

                            override fun onClick(p0: android.view.View?) {
                                Log.d("StartIoAds", "Banner Ad clicked")
                            }

                            override fun onImpression(p0: android.view.View?) {
                                Log.d("StartIoAds", "Banner Ad impression registered")
                            }
                        })
                        addView(banner)
                    } catch (e: Exception) {
                        Log.e("StartIoAds", "Error creating Start.io Banner view", e)
                    }
                }
            }
        )
    }
}
