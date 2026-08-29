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
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters
import com.example.util.ReminderUtils

@Composable
fun DueRemindersScreen(
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit
) {
    val pendingDueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()
    val allDueTransactions by viewModel.allDueTransactions.collectAsStateWithLifecycle()
    val pendingTotal by viewModel.pendingDueTotal.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: আজ ও মেয়াদোত্তীর্ণ, 1: সব বাকি

    val displayList = if (selectedSubTab == 0) pendingDueReminders else allDueTransactions.filter { !it.isPaid }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = "বাকি ও Customer Reminder",
                subtitle = "মোট বকেয়া: ${BengaliFormatters.toBanglaCurrency(pendingTotal)}",
                dueAlertCount = pendingDueReminders.size
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

            // Sub Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    label = {
                        Text(
                            "আজকের রিমাইন্ডার (${BengaliFormatters.toBanglaNumber(pendingDueReminders.size)})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (selectedSubTab == 0) Color.White else Orange600,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Orange500,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_today_reminders")
                )

                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    label = {
                        Text(
                            "সকল বকেয়া তালিকা",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = if (selectedSubTab == 1) Color.White else RoyalBlue700,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoyalBlue800,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Due Summary Banner
            Surface(
                color = Orange50,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Orange500.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "মোট অনাদায়ী বকেয়া",
                            style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMedium)
                        )
                        Text(
                            text = BengaliFormatters.toBanglaCurrency(pendingTotal),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Orange600,
                                fontSize = 20.sp
                            )
                        )
                    }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${BengaliFormatters.toBanglaNumber(displayList.size)} টি বাকি",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (displayList.isEmpty()) {
                BengaliEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = if (selectedSubTab == 0) "আজ কোনো বকেয়া পরিশোধের নোটিফিকেশন নেই" else "দোকানে কোনো বকেয়া হিসাব বাকি নেই",
                    description = "সব খরিদ্দারের বাকি পরিশোধিত হয়েছে।"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(displayList, key = { it.id }) { tx ->
                        DueReminderItemCard(
                            tx = tx,
                            shopName = shopProfile?.shopName ?: "আমার ব্যবসা",
                            shopkeeperName = shopProfile?.ownerName ?: "তানভির আহমেদ",
                            onPayDue = { viewModel.markDueAsPaid(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DueReminderItemCard(
    tx: SaleTransactionEntity,
    shopName: String,
    shopkeeperName: String,
    onPayDue: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Customer Name & Due Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Orange50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Orange600,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = tx.customerName?.takeIf { it.isNotBlank() } ?: "গ্রাহক (নামহীন)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tx.customerPhone?.takeIf { it.isNotBlank() } ?: "মোবাইল নম্বর নেই",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CharcoalLight,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = BengaliFormatters.toBanglaCurrency(tx.dueAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Orange600,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "বাকি টাকা",
                        style = MaterialTheme.typography.labelSmall.copy(color = CharcoalLight, fontSize = 10.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider()

            Spacer(modifier = Modifier.height(10.dp))

            // Details: Product & Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("পণ্যের বিবরণ", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = "${tx.productName} (${BengaliFormatters.toBanglaNumber(tx.quantitySold)} টি)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalDark
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("পরিশোধের শেষ তারিখ", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = tx.dueDate?.let { BengaliFormatters.formatDateBangla(it) } ?: "তারিখ নেই",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (tx.dueDate != null && BengaliFormatters.isToday(tx.dueDate)) Orange600
                            else if (tx.dueDate != null && BengaliFormatters.isOverdue(tx.dueDate)) Red600
                            else CharcoalDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reminder Actions: Primary WhatsApp (Free), Call, SMS, Mark Paid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp Button (Primary - Completely Free)
                Button(
                    onClick = {
                        if (!tx.customerPhone.isNullOrBlank()) {
                            ReminderUtils.sendWhatsAppReminder(
                                context = context,
                                phoneNumber = tx.customerPhone,
                                customerName = tx.customerName,
                                shopName = shopName,
                                shopkeeperName = tx.shopkeeperName ?: shopkeeperName,
                                productName = tx.productName,
                                dueAmount = tx.dueAmount
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp (ফ্রি)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Call Button
                FilledTonalButton(
                    onClick = {
                        if (!tx.customerPhone.isNullOrBlank()) {
                            ReminderUtils.dialPhoneNumber(context, tx.customerPhone)
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = RoyalBlue50, contentColor = RoyalBlue800),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.7f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কল", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // SMS Button
                FilledTonalButton(
                    onClick = {
                        if (!tx.customerPhone.isNullOrBlank()) {
                            ReminderUtils.sendSmsReminder(
                                context = context,
                                phoneNumber = tx.customerPhone,
                                customerName = tx.customerName,
                                shopName = shopName,
                                shopkeeperName = tx.shopkeeperName ?: shopkeeperName,
                                productName = tx.productName,
                                dueAmount = tx.dueAmount
                            )
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Orange50, contentColor = Orange600),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.8f)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // "পরিশোধ হয়েছে (Paid চিহ্নিত করুন)" Button
            OutlinedButton(
                onClick = onPayDue,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald700),
                border = BorderStroke(1.dp, Emerald500),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("পরিশোধ হয়েছে (Paid চিহ্নিত করুন)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
