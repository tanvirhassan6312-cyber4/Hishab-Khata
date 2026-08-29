package com.example.ui.screens

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
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters
import com.example.util.ReminderUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductHistoryScreen(
    viewModel: ShopViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()

    var selectedTxForDetails by remember { mutableStateOf<SaleTransactionEntity?>(null) }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = "পণ্য ও স্টক হিস্টোরি",
                subtitle = "বিক্রি, স্টক পরিবর্তন ও খতিয়ান",
                dueAlertCount = dueReminders.size
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Box: "পণ্য অনুযায়ী সমস্ত হিস্টোরি খুঁজুন"
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.historySearchQuery.value = it },
                placeholder = { Text("পণ্য, খরিদ্দার বা মোবাইল নম্বর দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalBlue700) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.historySearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "ক্লিয়ার")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                BengaliEmptyState(
                    icon = Icons.Default.HistoryEdu,
                    title = "কোনো বিক্রির হিস্টোরি পাওয়া যায়নি",
                    description = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে কোনো রেকর্ড মিলছে না" else "পণ্য বিক্রি শুরু করলে এখানে সমস্ত খতিয়ান দেখা যাবে"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        ComprehensiveProductHistoryCard(
                            tx = tx,
                            shopName = shopProfile?.shopName ?: "আমার ব্যবসা",
                            shopkeeperName = shopProfile?.ownerName ?: "তানভির আহমেদ",
                            onPayDue = { viewModel.markDueAsPaid(tx) },
                            onClick = { selectedTxForDetails = tx }
                        )
                    }
                }
            }
        }
    }

    selectedTxForDetails?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            shopName = shopProfile?.shopName ?: "আমার ব্যবসা",
            onDismiss = { selectedTxForDetails = null },
            onPayDue = {
                viewModel.markDueAsPaid(tx)
                selectedTxForDetails = null
            }
        )
    }
}

// -------------------------------------------------------------
// COMPREHENSIVE PRODUCT HISTORY CARD (Step 13 of requirements)
// -------------------------------------------------------------
@Composable
fun ComprehensiveProductHistoryCard(
    tx: SaleTransactionEntity,
    shopName: String,
    shopkeeperName: String,
    onPayDue: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current

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
            // Top: Product Name & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.productName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = BengaliFormatters.formatDateTimeBangla(tx.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CharcoalLight,
                            fontSize = 11.sp
                        )
                    )
                }

                DueStatusBadge(isPaid = tx.isPaid, dueDate = tx.dueDate)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider()

            Spacer(modifier = Modifier.height(10.dp))

            // Step 13 Requirements:
            // - কতটি বিক্রি হয়েছে
            // - আগের Stock কত ছিল
            // - বর্তমান Stock কত
            // - কত টাকা
            // - কত টাকা বাকি
            // - কত তারিখে বাকি পরিশোধের কথা
            // - Customer-এর মোবাইল নম্বর
            // - দোকানদারের নাম
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("বিক্রির সংখ্যা", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = "${BengaliFormatters.toBanglaNumber(tx.quantitySold)} টি",
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue800,
                        fontSize = 14.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("পূর্ব স্টক ➔ বর্তমান", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = "${BengaliFormatters.toBanglaNumber(tx.previousStock)} ➔ ${BengaliFormatters.toBanglaNumber(tx.currentStock)}",
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark,
                        fontSize = 13.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("মোট মূল্য", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = BengaliFormatters.toBanglaCurrency(tx.totalAmount),
                        fontWeight = FontWeight.Bold,
                        color = Emerald700,
                        fontSize = 14.sp
                    )
                }
            }

            // Customer, Due & Shopkeeper info
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = LightGraySurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (tx.isDue) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "বাকি টাকা: ${BengaliFormatters.toBanglaCurrency(tx.dueAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = Orange600,
                                fontSize = 12.sp
                            )
                            if (tx.dueDate != null) {
                                Text(
                                    text = "পরিশোধের তারিখ: ${BengaliFormatters.formatDateBangla(tx.dueDate)}",
                                    fontSize = 11.sp,
                                    color = if (BengaliFormatters.isOverdue(tx.dueDate) && !tx.isPaid) Red600 else CharcoalMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!tx.customerPhone.isNullOrBlank() || !tx.customerName.isNullOrBlank()) {
                            Text(
                                text = "খরিদ্দার: ${tx.customerName ?: ""} (${tx.customerPhone ?: ""})",
                                fontSize = 11.sp,
                                color = CharcoalDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "খরিদ্দার: সরাসরি নগদ বিক্রয়",
                                fontSize = 11.sp,
                                color = CharcoalLight
                            )
                        }

                        if (!tx.shopkeeperName.isNullOrBlank()) {
                            Text(
                                text = "দোকানদার: ${tx.shopkeeperName}",
                                fontSize = 11.sp,
                                color = CharcoalMedium
                            )
                        }
                    }
                }
            }

            // Quick Contact & Reminder Action Toolbar if there is a pending Due and customer phone!
            if (tx.isDue && !tx.isPaid && !tx.customerPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call Button
                    FilledTonalButton(
                        onClick = {
                            ReminderUtils.dialPhoneNumber(context, tx.customerPhone)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = RoyalBlue50, contentColor = RoyalBlue800),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // SMS Button
                    FilledTonalButton(
                        onClick = {
                            ReminderUtils.sendSmsReminder(
                                context = context,
                                phoneNumber = tx.customerPhone,
                                customerName = tx.customerName,
                                shopName = shopName,
                                shopkeeperName = tx.shopkeeperName ?: shopkeeperName,
                                productName = tx.productName,
                                dueAmount = tx.dueAmount
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Orange50, contentColor = Orange600),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // WhatsApp Button
                    FilledTonalButton(
                        onClick = {
                            ReminderUtils.sendWhatsAppReminder(
                                context = context,
                                phoneNumber = tx.customerPhone,
                                customerName = tx.customerName,
                                shopName = shopName,
                                shopkeeperName = tx.shopkeeperName ?: shopkeeperName,
                                productName = tx.productName,
                                dueAmount = tx.dueAmount
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Emerald50, contentColor = Emerald700),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Paid Check Button
                    IconButton(
                        onClick = onPayDue,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Emerald600),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "পরিশোধ চিহ্নিত করুন",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
