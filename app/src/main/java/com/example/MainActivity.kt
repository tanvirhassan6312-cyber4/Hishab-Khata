package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.DynamicBottomAdBanner
import com.example.ads.FullscreenInterstitialAdDialog
import com.example.ads.SmartAdLifecycleManager
import com.example.ads.StartIoAdManager
import com.example.ui.components.AtomicProximityDialog
import com.example.ui.components.CustomerArrivalPopUp
import com.example.ui.components.StartupPermissionHandler
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.util.AtomicProximityEngine
import com.example.util.NearbyCustomer

enum class ScreenRoute {
    HOME,
    PRODUCTS,
    QR_HUB,
    MEMO,
    DAILY_LEDGER,
    PRODUCT_HISTORY,
    DUE_REMINDERS,
    FAQ,
    SETTINGS
}

data class BottomNavItem(
    val route: ScreenRoute,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val viewModel: ShopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Start.io Ads SDK with App ID: 207916790
        StartIoAdManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel, activity = this)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ShopViewModel, activity: Activity? = null) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ScreenRoute.HOME) }
    var qrHubInitialSubTab by remember { mutableIntStateOf(0) }

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    var showProximityDialog by remember { mutableStateOf(false) }
    var proximityCustomerForDialog by remember { mutableStateOf<NearbyCustomer?>(null) }

    // Start proximity radar sync with active due transactions
    LaunchedEffect(allTransactions) {
        AtomicProximityEngine.startProximityRadar(context, allTransactions)
    }

    // Startup Camera & Audio Permission Handler (Prompts immediately on start, never again)
    StartupPermissionHandler()

    // Periodic smart background controller for bottom dynamic banner and periodic fullscreen ads
    SmartAdLifecycleManager(activity = activity)

    // Listen to UiEvents (Toasts and Ad triggers)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ProductAdded -> {
                    // Handled
                }
                is UiEvent.SaleCompleted -> {
                    // Trigger Start.io Interstitial Ad on successful sale
                    StartIoAdManager.showInterstitialAd(activity)
                }
                is UiEvent.QrCreated -> {
                    // Handled
                }
                is UiEvent.MemoSaved -> {
                    Toast.makeText(context, "মেমো #${event.memo.memoNumber} তৈরি ও সংরক্ষিত হয়েছে ✓", Toast.LENGTH_SHORT).show()
                    // Trigger Start.io Interstitial Ad on memo saved
                    StartIoAdManager.showInterstitialAd(activity)
                }
                is UiEvent.PaymentPaid -> {
                    Toast.makeText(context, "${event.customerName}-এর বাকি টাকা পরিশোধ হয়েছে ✓", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem(ScreenRoute.HOME, "হোম", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        BottomNavItem(ScreenRoute.PRODUCTS, "পণ্যসমূহ", Icons.Filled.Inventory, Icons.Outlined.Inventory2, "nav_products"),
        BottomNavItem(ScreenRoute.QR_HUB, "QR হাব", Icons.Filled.QrCode2, Icons.Outlined.QrCode, "nav_qr_hub"),
        BottomNavItem(ScreenRoute.MEMO, "মেমো", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_memo"),
        BottomNavItem(ScreenRoute.DAILY_LEDGER, "হিসাব খাতা", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_ledger")
    )

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dynamic periodic bottom banner (appears smoothly, stays 15s, disappears for 40s, or can be dismissed)
                DynamicBottomAdBanner()

                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorder),
                    shadowElevation = 4.dp
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = when (currentScreen) {
                                item.route -> true
                                ScreenRoute.PRODUCT_HISTORY -> item.route == ScreenRoute.DAILY_LEDGER
                                ScreenRoute.DUE_REMINDERS -> item.route == ScreenRoute.HOME
                                else -> false
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (item.route == ScreenRoute.QR_HUB) {
                                        qrHubInitialSubTab = 0
                                    }
                                    currentScreen = item.route
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                        tint = if (isSelected) DeepIndigo else CharcoalMuted
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) DeepIndigo else CharcoalMuted
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = RoyalBlue50,
                                    selectedIconColor = DeepIndigo,
                                    unselectedIconColor = CharcoalMuted,
                                    selectedTextColor = DeepIndigo,
                                    unselectedTextColor = CharcoalMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenRoute.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { tabIndex ->
                            when (tabIndex) {
                                1 -> currentScreen = ScreenRoute.PRODUCTS
                                2 -> {
                                    qrHubInitialSubTab = 0
                                    currentScreen = ScreenRoute.QR_HUB
                                }
                                3 -> currentScreen = ScreenRoute.MEMO
                                4 -> currentScreen = ScreenRoute.DAILY_LEDGER
                            }
                        },
                        onNavigateToQrScan = {
                            qrHubInitialSubTab = 0
                            currentScreen = ScreenRoute.QR_HUB
                        },
                        onNavigateToAddProduct = {
                            currentScreen = ScreenRoute.PRODUCTS
                        },
                        onNavigateToMemo = {
                            currentScreen = ScreenRoute.MEMO
                        },
                        onNavigateToDailyLedger = {
                            currentScreen = ScreenRoute.DAILY_LEDGER
                        },
                        onNavigateToQrHistory = {
                            qrHubInitialSubTab = 2
                            currentScreen = ScreenRoute.QR_HUB
                        },
                        onNavigateToProductHistory = {
                            currentScreen = ScreenRoute.PRODUCT_HISTORY
                        },
                        onShowDueReminders = {
                            currentScreen = ScreenRoute.DUE_REMINDERS
                        },
                        onShowSettings = {
                            currentScreen = ScreenRoute.SETTINGS
                        },
                        onShowFaq = {
                            currentScreen = ScreenRoute.FAQ
                        }
                    )
                }

                ScreenRoute.PRODUCTS -> {
                    ProductsScreen(
                        viewModel = viewModel,
                        onOpenQrScanForProduct = {
                            qrHubInitialSubTab = 0
                            currentScreen = ScreenRoute.QR_HUB
                        }
                    )
                }

                ScreenRoute.QR_HUB -> {
                    QrHubScreen(
                        viewModel = viewModel,
                        initialSubTab = qrHubInitialSubTab,
                        onNavigateBack = { currentScreen = ScreenRoute.HOME }
                    )
                }

                ScreenRoute.MEMO -> {
                    MemoScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = ScreenRoute.HOME }
                    )
                }

                ScreenRoute.DAILY_LEDGER -> {
                    DailyLedgerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = ScreenRoute.HOME }
                    )
                }

                ScreenRoute.PRODUCT_HISTORY -> {
                    ProductHistoryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = ScreenRoute.HOME }
                    )
                }

                ScreenRoute.DUE_REMINDERS -> {
                    DueRemindersScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = ScreenRoute.HOME }
                    )
                }

                ScreenRoute.FAQ -> {
                    AiFaqScreen(
                        onNavigateBack = { currentScreen = ScreenRoute.HOME }
                    )
                }

                ScreenRoute.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = ScreenRoute.HOME }
                    )
                }
            }

            // Customer Arrival Proximity Floating Pop-up Banner
            CustomerArrivalPopUp(
                onOpenSettlement = { customer ->
                    proximityCustomerForDialog = customer
                    showProximityDialog = true
                }
            )

            // Atomic Proximity Pay & Contactless Due Settlement Full Dialog
            if (showProximityDialog) {
                AtomicProximityDialog(
                    initialCustomer = proximityCustomerForDialog,
                    shopProfile = shopProfile,
                    allTransactions = allTransactions,
                    onDismiss = {
                        showProximityDialog = false
                        proximityCustomerForDialog = null
                    },
                    onDueSettled = { tx ->
                        viewModel.markDueAsPaid(tx)
                    }
                )
            }

            // Fullscreen Interstitial Ad Dialog Overlay
            FullscreenInterstitialAdDialog(activity = activity)
        }
    }
}
