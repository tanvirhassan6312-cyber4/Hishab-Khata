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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SaleTransactionEntity
import com.example.ui.components.DokanTopBar
import com.example.ui.components.DueStatusBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters

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
    onShowSettings: () -> Unit
) {
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val todaySales by viewModel.todaySalesTotal.collectAsStateWithLifecycle()
    val todayDue by viewModel.todayDueTotal.collectAsStateWithLifecycle()
    val totalProducts by viewModel.totalProductsCount.collectAsStateWithLifecycle()
    val totalStock by viewModel.totalStock.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DokanTopBar(
                title = shopProfile?.shopName?.takeIf { it.isNotBlank() } ?: "আমার ব্যবসা",
                subtitle = shopProfile?.ownerName?.takeIf { it.isNotBlank() },
                dueAlertCount = dueReminders.size,
                onNotificationClick = onShowDueReminders,
                onProfileClick = onShowSettings
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
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
                                        text = "QR কোড স্ক্যান করুন",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ক্যামেরা দিয়ে স্ক্যান করে পণ্যের বিবরণ দেখুন ও বিক্রি করুন",
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

            // 4. Quick Action Grid (2x2 Clean Action Buttons)
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

            // 5. Recent Transactions Header & List
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
