package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.util.BengaliFormatters
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Promotional Ad Model for dynamic local & Start.io partner ads
 */
data class PromoAd(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val tag: String,
    val ctaText: String,
    val ctaUrl: String? = null,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val badgeText: String = "স্পনসরড",
    val rating: String = "৪.৯ ★"
)

object StartIoAdManager {
    const val APP_ID = "207916790"
    private const val TAG = "StartIoAds"

    private var isInitialized = false
    private var interstitialAd: StartAppAd? = null

    // State flows for dynamic timed ad scheduling
    private val _isBottomBannerVisible = MutableStateFlow(false)
    val isBottomBannerVisible = _isBottomBannerVisible.asStateFlow()

    private val _isFullscreenAdVisible = MutableStateFlow(false)
    val isFullscreenAdVisible = _isFullscreenAdVisible.asStateFlow()

    private val _currentPromoIndex = MutableStateFlow(0)
    val currentPromoIndex = _currentPromoIndex.asStateFlow()

    private val _currentFullscreenPromo = MutableStateFlow<PromoAd?>(null)
    val currentFullscreenPromo = _currentFullscreenPromo.asStateFlow()

    // Sample High-Quality Merchant & Fintech Bangladesh Ads
    val promoAds = listOf(
        PromoAd(
            id = "bkash_merchant",
            title = "বিকাশ মার্চেন্ট কিউআর পেমেন্ট",
            subtitle = "কাস্টমার থেকে সহজে পেমেন্ট গ্রহণ করুন ০% চার্জে!",
            description = "আপনার দোকানের জন্য ফ্রি কিউআর স্ট্যান্ডি ও ডিজিটাল পেমেন্ট সেটআপ নিন। দৈনিক লেনদেনের হিসাব সরাসরি ব্যাংকে জমা হবে।",
            tag = "ডিজিটাল পেমেন্ট",
            ctaText = "ফ্রি কিউআর নিন",
            icon = Icons.Filled.QrCodeScanner,
            gradientColors = listOf(Color(0xFFE2136E), Color(0xFF9E0B4B)),
            badgeText = "অফার",
            rating = "৫.০ ★"
        ),
        PromoAd(
            id = "sme_loan",
            title = "দোকানি সহজ এসএমই লোন",
            subtitle = "৫০,০০০ থেকে ৫,০০,০০০ টাকা পর্যন্ত জামানতবিহীন ঋণ",
            description = "ব্যবসা বৃদ্ধির জন্য সহজ কিস্তিতে ক্ষুদ্র ঋণ সুবিধা। মাত্র ৩ দিনে ভেরিফিকেশন ও সরাসরি ব্যাংক অ্যাকাউন্টে টাকা বিতরণ।",
            tag = "ব্যবসা ঋণ",
            ctaText = "ঋণের জন্য আবেদন করুন",
            icon = Icons.Filled.AccountBalance,
            gradientColors = listOf(Emerald700, Color(0xFF064E3B)),
            badgeText = "জরুরি সুবিধা",
            rating = "৪.৮ ★"
        ),
        PromoAd(
            id = "smart_pos_printer",
            title = "স্মার্ট ব্লুটুথ থার্মাল পিওএস প্রিন্টার",
            subtitle = "মোবাইল থেকে সরাসরি মেমো ও ভাউচার প্রিন্ট করুন!",
            description = "৫৭ মিমি হাই-স্পিড ব্যাটারি চালিত পোর্টেবল প্রিন্টার। বিদ্যুৎ ছাড়াও সারাদিন মেমো প্রিন্ট করা যাবে।",
            tag = "হার্ডওয়্যার অফার",
            ctaText = "৫০% ছাড়ে অর্ডার করুন",
            icon = Icons.Filled.Print,
            gradientColors = listOf(DeepIndigo, Color(0xFF1E1B4B)),
            badgeText = "স্পেশাল অফার",
            rating = "৪.৯ ★"
        ),
        PromoAd(
            id = "voice_speaker",
            title = "দোকান সাউন্ডবক্স স্পিকার",
            subtitle = "টাকা জমা হলে বাংলায় জোরে বলে দেবে!",
            description = "বিকাশ, নগদ ও কিউআর পেমেন্ট আসলেই সাথে সাথে অডিও নোটিফিকেশন শুনুন। জালিয়াতি থেকে ১০০% মুক্ত থাকুন।",
            tag = "সাউন্ড স্পিকার",
            ctaText = "বিস্তারিত দেখুন",
            icon = Icons.Filled.VolumeUp,
            gradientColors = listOf(Color(0xFFD97706), Color(0xFF78350F)),
            badgeText = "বেস্টসেলার",
            rating = "৪.৯ ★"
        )
    )

    /**
     * Initialize Start.io SDK with App ID: 207916790
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            StartAppSDK.init(context, APP_ID, false)
            StartAppSDK.enableReturnAds(false)
            StartAppAd.disableSplash()
            isInitialized = true
            Log.d(TAG, "Start.io SDK initialized successfully with App ID: $APP_ID")

            preloadInterstitial(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Start.io SDK", e)
        }
    }

    /**
     * Preload interstitial ad
     */
    fun preloadInterstitial(context: Context) {
        try {
            if (interstitialAd == null) {
                interstitialAd = StartAppAd(context)
            }
            interstitialAd?.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    Log.d(TAG, "Start.io Interstitial Ad preloaded successfully")
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    Log.d(TAG, "Start.io Interstitial Ad failed to preload")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading Start.io Interstitial", e)
        }
    }

    /**
     * Show Start.io Interstitial or Fullscreen Dialog Promo
     */
    fun showInterstitialAd(activity: Activity?, onAdClosed: (() -> Unit)? = null) {
        var sdkShown = false
        if (activity != null) {
            try {
                if (interstitialAd == null) {
                    interstitialAd = StartAppAd(activity)
                }

                sdkShown = interstitialAd?.showAd(object : AdDisplayListener {
                    override fun adHidden(ad: Ad?) {
                        Log.d(TAG, "Interstitial Ad closed")
                        preloadInterstitial(activity)
                        onAdClosed?.invoke()
                    }

                    override fun adDisplayed(ad: Ad?) {
                        Log.d(TAG, "Interstitial Ad displayed")
                    }

                    override fun adClicked(ad: Ad?) {
                        Log.d(TAG, "Interstitial Ad clicked")
                    }

                    override fun adNotDisplayed(ad: Ad?) {
                        Log.d(TAG, "Interstitial Ad not displayed - showing fallback")
                        showFullscreenPromoAd()
                        preloadInterstitial(activity)
                    }
                }) ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Exception showing Start.io Ad", e)
            }
        }

        if (!sdkShown) {
            // Display fullscreen rich dialog ad
            showFullscreenPromoAd()
            onAdClosed?.invoke()
        }
    }

    fun showFullscreenPromoAd(promo: PromoAd? = null) {
        val selected = promo ?: promoAds[(_currentPromoIndex.value + 1) % promoAds.size]
        _currentPromoIndex.value = (_currentPromoIndex.value + 1) % promoAds.size
        _currentFullscreenPromo.value = selected
        _isFullscreenAdVisible.value = true
    }

    fun dismissFullscreenAd() {
        _isFullscreenAdVisible.value = false
        _currentFullscreenPromo.value = null
    }

    fun showBottomBanner() {
        _currentPromoIndex.value = (_currentPromoIndex.value + 1) % promoAds.size
        _isBottomBannerVisible.value = true
    }

    fun dismissBottomBanner() {
        _isBottomBannerVisible.value = false
    }
}

/**
 * Smart Controller that rotates the periodic dynamic bottom ad banner and periodic fullscreen ads.
 * - Bottom banner: Appears smoothly, stays for 15s, then automatically disappears for 40s (as requested: 'নিচেও কিছুক্ষণ পর আসবে আবার যাবে')
 * - Fullscreen ad: Appears periodically every 2 minutes or upon major actions (as requested: 'পুরোপুরি স্ক্রিনে একটা অ্যাড আসবে')
 */
@Composable
fun SmartAdLifecycleManager(
    activity: Activity? = null
) {
    // Dynamic Bottom Banner Periodic Loop (15 seconds on, 45 seconds off)
    LaunchedEffect(Unit) {
        // Initial delay before first banner appears
        delay(6000)
        while (true) {
            StartIoAdManager.showBottomBanner()
            // Stays visible for 15 seconds
            delay(15000)
            StartIoAdManager.dismissBottomBanner()
            // Stays hidden for 40 seconds before showing again
            delay(40000)
        }
    }

    // Periodic Fullscreen Interstitial Ad Trigger (Every 110 seconds)
    LaunchedEffect(Unit) {
        delay(45000) // Initial delay of 45 seconds after opening app
        while (true) {
            StartIoAdManager.showInterstitialAd(activity)
            // Interval between automated fullscreen ads: 110 seconds
            delay(110000)
        }
    }
}

/**
 * Animated Dynamic Bottom Ad Banner with auto-hide timer, close button, and Start.io / Local promo support
 */
@Composable
fun DynamicBottomAdBanner(
    modifier: Modifier = Modifier
) {
    val isVisible by StartIoAdManager.isBottomBannerVisible.collectAsStateWithLifecycle()
    val promoIndex by StartIoAdManager.currentPromoIndex.collectAsStateWithLifecycle()
    val promo = StartIoAdManager.promoAds[promoIndex % StartIoAdManager.promoAds.size]
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(250)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .testTag("dynamic_bottom_ad_banner"),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with "বিজ্ঞাপন" badge and Close [X] button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "বিজ্ঞাপন",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalMuted,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• কিছুক্ষণ পর স্বয়ংক্রিয়ভাবে বন্ধ হবে",
                            fontSize = 10.sp,
                            color = CharcoalMuted
                        )
                    }

                    // Manual close button
                    IconButton(
                        onClick = { StartIoAdManager.dismissBottomBanner() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বিজ্ঞাপন বন্ধ করুন",
                            tint = CharcoalMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Banner Content Body (Combines Start.io Banner + Rich Local Bengali Ad Card)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "${promo.title} বিস্তারিত শীঘ্রই আসবে", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(promo.gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = promo.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = promo.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CharcoalDark
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Amber100
                            ) {
                                Text(
                                    text = promo.badgeText,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber800,
                                    modifier = Modifier.padding(horizontal = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = promo.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                color = CharcoalMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "${promo.title} অফার সক্রিয় হচ্ছে...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = promo.ctaText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen Interactive Bengali Interstitial Ad Dialog ('পুরোপুরি স্ক্রিনে একটা অ্যাড')
 */
@Composable
fun FullscreenInterstitialAdDialog(
    activity: Activity? = null
) {
    val isVisible by StartIoAdManager.isFullscreenAdVisible.collectAsStateWithLifecycle()
    val promoState by StartIoAdManager.currentFullscreenPromo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val promo = promoState

    if (isVisible && promo != null) {
        var remainingSeconds by remember { mutableIntStateOf(5) }
        var canClose by remember { mutableStateOf(false) }

        // 5-second countdown timer for interstitial skip
        LaunchedEffect(promo.id) {
            remainingSeconds = 5
            canClose = false
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            canClose = true
        }

        Dialog(
            onDismissRequest = {
                if (canClose) StartIoAdManager.dismissFullscreenAd()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = canClose,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A).copy(alpha = 0.96f))
                    .padding(16.dp)
                    .testTag("fullscreen_interstitial_dialog"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Header: "বিজ্ঞাপন (Ad)" & Countdown / Skip Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = CharcoalMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "বিজ্ঞাপন (AD)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalMuted
                                    )
                                }
                            }

                            // Skip Button / Countdown
                            if (canClose) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = CharcoalDark,
                                    modifier = Modifier.clickable {
                                        StartIoAdManager.dismissFullscreenAd()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "স্কিপ করুন",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "বন্ধ করুন",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Slate100,
                                    border = BorderStroke(1.dp, CardBorder)
                                ) {
                                    Text(
                                        text = "${BengaliFormatters.toBanglaNumber(remainingSeconds)} সেকেন্ডে বন্ধ করা যাবে",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CharcoalMuted,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Promo Graphic & Icon
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(promo.gradientColors))
                                .shadow(8.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = promo.icon,
                                contentDescription = promo.title,
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category & Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald50
                            ) {
                                Text(
                                    text = promo.tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = promo.rating,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber600
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title
                        Text(
                            text = promo.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Subtitle
                        Text(
                            text = promo.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DeepIndigo,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Description
                        Text(
                            text = promo.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CharcoalMuted,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Action Button
                        Button(
                            onClick = {
                                Toast.makeText(context, "${promo.title} অফার বুক করা হয়েছে ✓", Toast.LENGTH_LONG).show()
                                StartIoAdManager.dismissFullscreenAd()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = promo.gradientColors.first()
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = promo.ctaText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dismiss button
                        TextButton(
                            onClick = { StartIoAdManager.dismissFullscreenAd() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "এখন নয়, পরে দেখব",
                                color = CharcoalMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

