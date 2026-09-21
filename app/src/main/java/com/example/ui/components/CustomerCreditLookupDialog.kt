package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CommunityCreditRecordEntity
import com.example.data.local.SaleTransactionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters
import com.example.util.CreditRatingCategory
import com.example.util.CustomerTrustScoreEngine
import com.example.util.TrustScoreResult
import kotlinx.coroutines.launch

@Composable
fun CustomerCreditLookupDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchPhoneOrName by remember { mutableStateOf("") }
    var trustResult by remember { mutableStateOf<TrustScoreResult?>(null) }
    var selectedCustName by remember { mutableStateOf("") }
    var selectedCustPhone by remember { mutableStateOf("") }
    var showDetailedDialog by remember { mutableStateOf(false) }

    val allTransactions by viewModel.allTransactions.collectAsState()
    val communityRecords by viewModel.communityCreditRecords.collectAsState()

    // Find unique customers from transactions
    val customerList = remember(allTransactions) {
        allTransactions
            .filter { !it.customerName.isNullOrBlank() || !it.customerPhone.isNullOrBlank() }
            .distinctBy { (it.customerPhone?.ifBlank { null } ?: it.customerName) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("dialog_customer_credit_lookup")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald50,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Emerald700)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI কাস্টমার ট্রাস্ট স্কোর",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DeepIndigo)
                            )
                            Text(
                                text = "বাকি দেওয়ার আগে CIB ঝুঁকি যাচাই করুন",
                                style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted, fontSize = 11.sp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CharcoalMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input
                OutlinedTextField(
                    value = searchPhoneOrName,
                    onValueChange = {
                        searchPhoneOrName = it
                        if (it.length >= 3) {
                            scope.launch {
                                val res = viewModel.getCustomerTrustScore(it, it)
                                trustResult = res
                                selectedCustName = it
                                selectedCustPhone = it
                            }
                        } else {
                            trustResult = null
                        }
                    },
                    label = { Text("গ্রাহকের মোবাইল নম্বর বা নাম") },
                    placeholder = { Text("যেমন: 01711XXXXXX বা রহিম") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalBlue700) },
                    trailingIcon = {
                        if (searchPhoneOrName.isNotEmpty()) {
                            IconButton(onClick = {
                                searchPhoneOrName = ""
                                trustResult = null
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_search_credit_customer")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // If searched customer result is available
                trustResult?.let { res ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDetailedDialog = true }
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (res.ratingCategory) {
                                CreditRatingCategory.EXCELLENT -> Emerald50
                                CreditRatingCategory.GOOD -> Color(0xFFECFDF5)
                                CreditRatingCategory.MODERATE -> Amber50
                                CreditRatingCategory.HIGH_RISK -> Red50
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when (res.ratingCategory) {
                                CreditRatingCategory.EXCELLENT -> Emerald300
                                CreditRatingCategory.GOOD -> Color(0xFF6EE7B7)
                                CreditRatingCategory.MODERATE -> Amber300
                                CreditRatingCategory.HIGH_RISK -> Red300
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedCustName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                                    )
                                    Text(
                                        text = "স্কোর: ${res.score}/১০০ (${res.titleBangla})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when (res.ratingCategory) {
                                                CreditRatingCategory.EXCELLENT -> Emerald700
                                                CreditRatingCategory.GOOD -> Emerald700
                                                CreditRatingCategory.MODERATE -> Amber700
                                                CreditRatingCategory.HIGH_RISK -> Red700
                                            }
                                        )
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = when (res.ratingCategory) {
                                        CreditRatingCategory.EXCELLENT -> Emerald700
                                        CreditRatingCategory.GOOD -> Emerald700
                                        CreditRatingCategory.MODERATE -> Amber700
                                        CreditRatingCategory.HIGH_RISK -> Red700
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${res.score}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }

                            if (res.isCommunityDefaulter) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = Red600) {
                                    Text(
                                        "🚨 কমিউনিটি রিস্ক: অন্য দোকানে ডিফল্টার মার্ক করা!",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "👆 বিস্তারিত দেখতে ট্যাপ করুন",
                                style = MaterialTheme.typography.labelSmall.copy(color = CharcoalMuted, fontSize = 10.sp)
                            )
                        }
                    }
                }

                // Known Customers List
                Text(
                    text = "দোকানের খাতার কাস্টমার তালিকা:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalMuted),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(customerList.take(15)) { tx ->
                        val name = tx.customerName ?: tx.customerPhone ?: "অজ্ঞাত"
                        val phone = tx.customerPhone
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val res = viewModel.getCustomerTrustScore(name, phone)
                                        trustResult = res
                                        selectedCustName = name
                                        selectedCustPhone = phone ?: ""
                                        showDetailedDialog = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBlue700, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark))
                                        if (!phone.isNullOrBlank()) {
                                            Text(phone, style = MaterialTheme.typography.labelSmall.copy(color = CharcoalMuted, fontSize = 10.sp))
                                        }
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CharcoalMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetailedDialog && trustResult != null) {
        TrustScoreDialog(
            customerName = selectedCustName,
            customerPhone = selectedCustPhone,
            trustScoreResult = trustResult!!,
            onDismiss = { showDetailedDialog = false },
            onReportDefaulter = { phone, name, amt, note ->
                viewModel.reportDefaulter(phone, name, amt, note)
            }
        )
    }
}
