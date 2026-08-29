package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SaleTransactionEntity
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.DokanTopBar
import com.example.ui.components.DueStatusBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DailySummary
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters
import com.example.util.ReminderUtils
import java.util.Calendar

@Composable
fun DailyLedgerScreen(
    viewModel: ShopViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                }
                viewModel.setLedgerDate(selectedCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    var selectedTransactionForDetails by remember { mutableStateOf<SaleTransactionEntity?>(null) }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = "সারা দিনের মোট হিস্টোরি",
                subtitle = "দৈনিক কেনাবেচা ও হিসাব খাতা",
                dueAlertCount = dueReminders.size
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // 1. Date Selector Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .clickable { datePickerDialog.show() }
                        .testTag("date_picker_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RoyalBlue50),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = RoyalBlue800,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "নির্বাচিত হিসাবের তারিখ",
                                    style = MaterialTheme.typography.labelMedium.copy(color = CharcoalLight)
                                )
                                Text(
                                    text = BengaliFormatters.formatDateBangla(dailySummary.dateMillis),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalDark,
                                        fontSize = 17.sp
                                    )
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { datePickerDialog.show() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = RoyalBlue50, contentColor = RoyalBlue800)
                        ) {
                            Text("তারিখ বদলান", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. Day Summary Stats Grid (Step 12 requirements)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Total Sales & Total Cash Received
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "মোট বিক্রি",
                            value = BengaliFormatters.toBanglaCurrency(dailySummary.totalSales),
                            icon = Icons.Default.MonetizationOn,
                            iconBgColor = Emerald50,
                            iconTintColor = Emerald600,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "নগদ আদায়",
                            value = BengaliFormatters.toBanglaCurrency(dailySummary.totalCashReceived),
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBgColor = RoyalBlue50,
                            iconTintColor = RoyalBlue700,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Total Due & Due Customers Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "মোট বাকি",
                            value = BengaliFormatters.toBanglaCurrency(dailySummary.totalDue),
                            icon = Icons.Default.ReceiptLong,
                            iconBgColor = Orange50,
                            iconTintColor = Orange600,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "বাকি খরিদ্দার",
                            value = "${BengaliFormatters.toBanglaNumber(dailySummary.dueCustomersCount)} জন",
                            icon = Icons.Default.PeopleOutline,
                            iconBgColor = Red50,
                            iconTintColor = Red600,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Total Items Sold
                    StatCard(
                        title = "মোট পণ্য বিক্রি",
                        value = "${BengaliFormatters.toBanglaNumber(dailySummary.totalItemsSold)} টি (${BengaliFormatters.toBanglaNumber(dailySummary.totalTransactionsCount)} টি রসিদ)",
                        icon = Icons.Default.ShoppingCartCheckout,
                        iconBgColor = Sky100,
                        iconTintColor = Sky700,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Transactions on this date
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "এই দিনের বিক্রয় তালিকা",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "মোট: ${BengaliFormatters.toBanglaNumber(dailySummary.transactions.size)} টি",
                        style = MaterialTheme.typography.bodySmall.copy(color = CharcoalLight)
                    )
                }
            }

            if (dailySummary.transactions.isEmpty()) {
                item {
                    BengaliEmptyState(
                        icon = Icons.Default.Receipt,
                        title = "এই তারিখে কোনো বিক্রির রেকর্ড নেই",
                        description = "অন্য কোনো তারিখ নির্বাচন করে হিসাব দেখুন"
                    )
                }
            } else {
                items(dailySummary.transactions, key = { it.id }) { tx ->
                    DayTransactionDetailCard(
                        tx = tx,
                        onPayDue = { viewModel.markDueAsPaid(tx) },
                        onClick = { selectedTransactionForDetails = tx }
                    )
                }
            }
        }
    }

    // Transaction Details Dialog
    selectedTransactionForDetails?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            shopName = shopProfile?.shopName ?: "আমার ব্যবসা",
            onDismiss = { selectedTransactionForDetails = null },
            onPayDue = {
                viewModel.markDueAsPaid(tx)
                selectedTransactionForDetails = null
            }
        )
    }
}

@Composable
fun DayTransactionDetailCard(
    tx: SaleTransactionEntity,
    onPayDue: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tx.isDue && !tx.isPaid) Orange50 else Emerald50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tx.isDue && !tx.isPaid) Icons.Default.ReceiptLong else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (tx.isDue && !tx.isPaid) Orange600 else Emerald600,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = tx.productName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = BengaliFormatters.formatTimeBangla(tx.createdAt),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CharcoalLight,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                DueStatusBadge(isPaid = tx.isPaid, dueDate = tx.dueDate)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider()

            Spacer(modifier = Modifier.height(10.dp))

            // Quantities & Financials
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("পরিমাণ", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = "${BengaliFormatters.toBanglaNumber(tx.quantitySold)} টি",
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("মোট বিল", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = BengaliFormatters.toBanglaCurrency(tx.totalAmount),
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue800
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("নগদ আদায়", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = BengaliFormatters.toBanglaCurrency(tx.paidAmount),
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }

            // Customer info & Quick Actions if due
            if (tx.isDue && !tx.isPaid) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Orange50,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "বাকি: ${BengaliFormatters.toBanglaCurrency(tx.dueAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = Orange600,
                                fontSize = 13.sp
                            )
                            if (!tx.customerName.isNullOrBlank() || !tx.customerPhone.isNullOrBlank()) {
                                Text(
                                    text = "${tx.customerName ?: "গ্রাহক"} (${tx.customerPhone ?: ""})",
                                    fontSize = 11.sp,
                                    color = CharcoalDark
                                )
                            }
                        }

                        Button(
                            onClick = onPayDue,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("পরিশোধ হয়েছে", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    tx: SaleTransactionEntity,
    shopName: String,
    onDismiss: () -> Unit,
    onPayDue: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "বিক্রয়ের সম্পূর্ণ বিবরণ",
                fontWeight = FontWeight.Bold,
                color = CharcoalDark,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("পণ্যের নাম:", tx.productName)
                DetailRow("তারিখ ও সময়:", BengaliFormatters.formatDateTimeBangla(tx.createdAt))
                DetailRow("বিক্রির পরিমাণ:", "${BengaliFormatters.toBanglaNumber(tx.quantitySold)} টি")
                DetailRow("পূর্ববর্তী স্টক:", "${BengaliFormatters.toBanglaNumber(tx.previousStock)} টি")
                DetailRow("বর্তমান স্টক:", "${BengaliFormatters.toBanglaNumber(tx.currentStock)} টি")
                DetailRow("মোট বিক্রয় মূল্য:", BengaliFormatters.toBanglaCurrency(tx.totalAmount))
                DetailRow("নগদ পরিশোধ:", BengaliFormatters.toBanglaCurrency(tx.paidAmount))

                if (tx.isDue) {
                    DetailRow("বাকি টাকার পরিমাণ:", BengaliFormatters.toBanglaCurrency(tx.dueAmount), isBold = true, color = Orange600)
                    DetailRow("পরিশোধের অবস্থা:", if (tx.isPaid) "পরিশোধিত ✓" else "বাকি রয়েছে", color = if (tx.isPaid) Emerald700 else Orange600)
                    tx.dueDate?.let {
                        DetailRow("পরিশোধের শেষ তারিখ:", BengaliFormatters.formatDateBangla(it))
                    }
                    if (!tx.customerName.isNullOrBlank()) {
                        DetailRow("কাস্টমারের নাম:", tx.customerName)
                    }
                    if (!tx.customerPhone.isNullOrBlank()) {
                        DetailRow("মোবাইল নম্বর:", tx.customerPhone)
                    }
                }

                if (!tx.qrCodeUsed.isNullOrBlank()) {
                    DetailRow("QR Code:", tx.qrCodeUsed)
                }
                if (!tx.shopkeeperName.isNullOrBlank()) {
                    DetailRow("দোকানদার:", tx.shopkeeperName)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share receipt
                OutlinedButton(
                    onClick = {
                        val receiptText = """
                            ===== $shopName =====
                            পণ্য: ${tx.productName}
                            পরিমাণ: ${tx.quantitySold}
                            মোট মূল্য: ৳${tx.totalAmount}
                            নগদ: ৳${tx.paidAmount}
                            বাকি: ৳${tx.dueAmount}
                            তারিখ: ${BengaliFormatters.formatDateTimeBangla(tx.createdAt)}
                            ======================
                        """.trimIndent()
                        ReminderUtils.shareText(context, "রসিদ", receiptText)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("শেয়ার")
                }

                if (tx.isDue && !tx.isPaid) {
                    Button(
                        onClick = onPayDue,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Paid চিহ্নিত করুন")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = CharcoalLight)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false, color: Color = CharcoalDark) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = CharcoalLight)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
