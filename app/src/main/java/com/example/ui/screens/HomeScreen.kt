package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SaleTransactionEntity
import com.example.ui.components.AtomicProximityDialog
import com.example.ui.components.CustomerCreditLookupDialog
import com.example.ui.components.DokanTopBar
import com.example.ui.components.DueStatusBadge
import com.example.ui.components.SmartRestockSpeakerDialog
import com.example.ui.components.StatCard
import com.example.ui.components.VoiceLedgerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters
import com.example.util.SmartReStockSpeakerHelper

@Composable
fun HomeScreen(
    viewModel: ShopViewModel,
    onNavigateToTab: (Int) -> Unit,
    onNavigateToQrScan: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToMemo: () -> Unit,
    onNavigateToDailyLedger: () -> Unit,
    onNavigateToQrHistory: () -> Unit,
    onNavigateToProductHistory: () -> Unit,
    onShowDueReminders: () -> Unit,
    onShowSettings: () -> Unit,
    onShowFaq: () -> Unit = {}
) {
    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val todaySales by viewModel.todaySalesTotal.collectAsStateWithLifecycle()
    val todayDue by viewModel.todayDueTotal.collectAsStateWithLifecycle()
    val totalProducts by viewModel.totalProductsCount.collectAsStateWithLifecycle()
    val totalStock by viewModel.totalStock.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()

    var showVoiceLedgerDialog by remember { mutableStateOf(false) }
    var showCreditLookupDialog by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var showAtomicProximityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = shopProfile?.shopName?.takeIf { it.isNotBlank() } ?: "আমার ব্যবসা",
                subtitle = shopProfile?.ownerName?.takeIf { it.isNotBlank() },
                dueAlertCount = dueReminders.size,
                onNotificationClick = onShowDueReminders,
                onProfileClick = onShowSettings,
                onFaqClick = onShowFaq
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showVoiceLedgerDialog = true },
                containerColor = DeepIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("fab_voice_ledger")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "ভয়েস হিসাব")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI মুখে বলা হিসাব", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            // 1. Due Alert Banner (if any customer has payment due today)
            if (dueReminders.isNotEmpty()) {
                item {
                    DueReminderBanner(
                        count = dueReminders.size,
                        onClick = onShowDueReminders
                    )
                }
            }

            // 1.1 Low Stock Smart Speaker Alert Banner (if low stock products exist)
            if (lowStockProducts.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Amber50),
                        border = BorderStroke(1.dp, Amber300),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRestockDialog = true }
                            .testTag("banner_smart_restock")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Amber100, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "স্পিকার",
                                        tint = Amber800,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "📢 স্মার্ট রি-স্টক স্পিকার",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Amber900
                                        )
                                    )
                                    Text(
                                        text = "${BengaliFormatters.toBanglaNumber(lowStockProducts.size)}টি পণ্যের স্টক কম! ভয়েস শুনুন ও পাইকারি অর্ডার দিন",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = CharcoalDark,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            // Instant Voice Speaker Button
                            IconButton(
                                onClick = {
                                    lowStockProducts.firstOrNull()?.let { prod ->
                                        SmartReStockSpeakerHelper.speakRestockAlert(context, prod, shopProfile)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Amber200, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "শুনুন",
                                    tint = Amber900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Stats Grid (আজকের বিক্রি ও আজকের বাকি)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "আজকের বিক্রি",
                        value = BengaliFormatters.toBanglaCurrency(todaySales),
                        valueColor = Emerald500,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_today_sales"),
                        onClick = onNavigateToDailyLedger
                    )

                    StatCard(
                        title = "আজকের বাকি",
                        value = BengaliFormatters.toBanglaCurrency(todayDue),
                        valueColor = Orange500,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_today_due"),
                        onClick = onShowDueReminders
                    )
                }
            }

            // 3. Hero QR Scanner & Memo Action Banners
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // QR Scanner Hero Card
                    Surface(
                        color = DeepIndigo,
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToQrScan() }
                            .testTag("hero_qr_scan_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "স্ক্যান",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "QR কোড স্ক্যান ও প্রিন্ট",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "স্ক্যান করে বিক্রি, স্টিকার ও A4 শিট প্রিন্ট করুন",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Memo Create Hero Card (Specially designed for shopkeepers, managers, businessmen)
                    Surface(
                        color = RoyalBlue700,
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToMemo() }
                            .testTag("hero_memo_create_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = "মেমো",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "মেমো তৈরি (Memo Create)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "দোকানদার, ব্যবসায়ী ও ম্যানেজারদের ডিজিটাল মেমো",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 4. AI New Features Spotlight Row (Voice Ledger, AI Trust Score & Atomic Proximity Pay)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1B4B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF4338CA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAtomicProximityDialog = true }
                        .testTag("banner_atomic_proximity_pay")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Emerald500, Emerald700))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "অ্যাটোমিক প্রক্সিমিটি",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡ অ্যাটোমিক প্রক্সিমিটি পে",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Emerald500
                                    ) {
                                        Text(
                                            text = "অফলাইন",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "কোনো কিউআর বা ইন্টারনেট ছাড়া কাস্টমারের সাথে টাচলেস অডিও হ্যান্ডশেকে বাকি পরিশোধ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MinimalActionCard(
                        title = "🎙️ AI মুখে বলা হিসাব",
                        icon = Icons.Default.Mic,
                        iconBgColor = RoyalBlue50,
                        iconTintColor = RoyalBlue700,
                        onClick = { showVoiceLedgerDialog = true },
                        modifier = Modifier.weight(1f),
                        testTag = "home_opt_voice_ledger"
                    )

                    MinimalActionCard(
                        title = "🛡️ কাস্টমার ক্রেডিট স্কোর",
                        icon = Icons.Default.Shield,
                        iconBgColor = Emerald50,
                        iconTintColor = Emerald700,
                        onClick = { showCreditLookupDialog = true },
                        modifier = Modifier.weight(1f),
                        testTag = "home_opt_credit_trust"
                    )
                }
            }

            // 5. Quick Action Grid (2x2 Clean Action Buttons)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MinimalActionCard(
                            title = "পণ্য যোগ ও স্টক",
                            icon = Icons.Default.Add,
                            iconBgColor = Emerald50,
                            iconTintColor = Emerald600,
                            onClick = onNavigateToAddProduct,
                            modifier = Modifier.weight(1f),
                            testTag = "home_opt_add_product"
                        )

                        MinimalActionCard(
                            title = "দিনের হিসাব",
                            icon = Icons.Default.CalendarMonth,
                            iconBgColor = Sky50,
                            iconTintColor = Sky700,
                            onClick = onNavigateToDailyLedger,
                            modifier = Modifier.weight(1f),
                            testTag = "home_opt_daily_ledger"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MinimalActionCard(
                            title = "QR তৈরি ও হাব",
                            icon = Icons.Default.QrCode2,
                            iconBgColor = RoyalBlue50,
                            iconTintColor = DeepIndigo,
                            onClick = { onNavigateToTab(2) },
                            modifier = Modifier.weight(1f),
                            testTag = "home_opt_qr_hub"
                        )

                        MinimalActionCard(
                            title = "বাকি রিমাইন্ডার",
                            icon = Icons.Default.NotificationsActive,
                            iconBgColor = Orange50,
                            iconTintColor = Orange600,
                            onClick = onShowDueReminders,
                            modifier = Modifier.weight(1f),
                            testTag = "home_opt_due_reminders"
                        )
                    }
                }
            }

            // 5.1 AI FAQ & Developer Attribution Helper Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalBlue50),
                    border = BorderStroke(1.dp, RoyalBlue200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowFaq() }
                        .testTag("home_faq_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(DeepIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "FAQ",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "প্রশ্ন ও AI সহায়তা (FAQ)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DeepIndigo,
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                                Text(
                                    text = "অ্যাপ না বুঝলে প্রশ্ন করুন • ডেভলপার: তাফসির এবং তানভির",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CharcoalDark.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = DeepIndigo.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 6. Recent Transactions Header & List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সাম্প্রতিক বিক্রয় লেনদেন",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )

                    TextButton(onClick = onNavigateToDailyLedger) {
                        Text("সব দেখুন", color = DeepIndigo, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (allTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = CharcoalMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "আজ কোনো বিক্রি হয়নি",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = CharcoalLight,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            } else {
                items(allTransactions.take(5), key = { it.id }) { tx ->
                    RecentSaleRow(transaction = tx)
                }
            }
        }
    }

    // Voice Ledger Dialog
    if (showVoiceLedgerDialog) {
        VoiceLedgerDialog(
            allProducts = allProducts,
            onDismiss = { showVoiceLedgerDialog = false },
            onConfirmEntry = { parsed ->
                viewModel.processVoiceTransaction(parsed) {
                    showVoiceLedgerDialog = false
                }
            }
        )
    }

    // Customer Credit Trust Lookup Dialog
    if (showCreditLookupDialog) {
        CustomerCreditLookupDialog(
            viewModel = viewModel,
            onDismiss = { showCreditLookupDialog = false }
        )
    }

    // Smart Restock Speaker Dialog
    if (showRestockDialog) {
        SmartRestockSpeakerDialog(
            lowStockProducts = lowStockProducts,
            shopProfile = shopProfile,
            onDismiss = { showRestockDialog = false }
        )
    }

    // Atomic Proximity Pay & Contactless Due Settlement Dialog
    if (showAtomicProximityDialog) {
        AtomicProximityDialog(
            shopProfile = shopProfile,
            allTransactions = allTransactions,
            onDismiss = { showAtomicProximityDialog = false },
            onDueSettled = { tx ->
                viewModel.markDueAsPaid(tx)
            }
        )
    }
}

@Composable
fun MinimalActionCard(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp,
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CharcoalDark,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DueReminderBanner(
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        color = Orange50,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Orange500),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("banner_due_reminder")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Orange500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "আজ ${BengaliFormatters.toBanglaNumber(count)} জন কাস্টমারের বাকি পরিশোধের দিন",
                        fontWeight = FontWeight.Bold,
                        color = Orange600,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "কাস্টমারদের তাগাদা মেসেজ পাঠাতে ট্যাপ করুন",
                        fontSize = 11.sp,
                        color = CharcoalLight
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Orange600)
        }
    }
}

@Composable
fun RecentSaleRow(transaction: SaleTransactionEntity) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (transaction.isDue) Orange50 else Emerald50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.isDue) Icons.Default.AccessTime else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (transaction.isDue) Orange600 else Emerald600,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.productName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )
                    Text(
                        text = "${BengaliFormatters.toBanglaNumber(transaction.quantitySold)} টি • ${BengaliFormatters.formatTimeBangla(transaction.createdAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = CharcoalLight)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = BengaliFormatters.toBanglaCurrency(transaction.totalAmount),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                )
                DueStatusBadge(isPaid = transaction.isPaid, dueDate = transaction.dueDate)
            }
        }
    }
}
