package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CommunityCreditRecordEntity
import com.example.ui.theme.*
import com.example.util.BengaliFormatters
import com.example.util.CreditRatingCategory
import com.example.util.TrustScoreResult

@Composable
fun TrustScoreDialog(
    customerName: String,
    customerPhone: String?,
    trustScoreResult: TrustScoreResult,
    onDismiss: () -> Unit,
    onReportDefaulter: (phone: String, name: String, amount: Double, reason: String) -> Unit
) {
    var showReportSheet by remember { mutableStateOf(false) }
    var reportAmount by remember { mutableStateOf(if (trustScoreResult.activeDueAmount > 0) trustScoreResult.activeDueAmount.toString() else "") }
    var reportReason by remember { mutableStateOf("") }

    val categoryColor = when (trustScoreResult.ratingCategory) {
        CreditRatingCategory.EXCELLENT -> Emerald700
        CreditRatingCategory.GOOD -> Color(0xFF059669)
        CreditRatingCategory.MODERATE -> Amber700
        CreditRatingCategory.HIGH_RISK -> Red700
    }

    val categoryBg = when (trustScoreResult.ratingCategory) {
        CreditRatingCategory.EXCELLENT -> Emerald50
        CreditRatingCategory.GOOD -> Color(0xFFECFDF5)
        CreditRatingCategory.MODERATE -> Amber50
        CreditRatingCategory.HIGH_RISK -> Red50
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("dialog_trust_score")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                            color = RoyalBlue50,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = RoyalBlue700)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI ক্রেডিট ট্রাস্ট স্কোর",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DeepIndigo)
                            )
                            Text(
                                text = "ক্ষুদ্র ব্যাংকিং CIB ও ঝুঁকি বিশ্লেষণ",
                                style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CharcoalMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Customer Header Info
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = customerName.ifBlank { "গ্রাহক" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                            )
                            if (!customerPhone.isNullOrBlank()) {
                                Text(
                                    text = "📱 $customerPhone",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = categoryBg,
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = trustScoreResult.statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = categoryColor),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Circular Trust Score Meter
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(categoryBg)
                        .border(4.dp, categoryColor, CircleShape)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${trustScoreResult.score}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = categoryColor
                        )
                        Text(
                            text = "/ ১০০",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = trustScoreResult.titleBangla,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = categoryColor)
                )

                // Community Risk Alert Banner (If flagged in anonymous shared registry)
                if (trustScoreResult.isCommunityDefaulter) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Red50),
                        border = BorderStroke(1.5.dp, Red600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Red600, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🔴 কমিউনিটি রিস্ক অ্যালার্ট!",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Red700)
                                )
                                Text(
                                    text = trustScoreResult.communityWarningMessage ?: "এই কাস্টমার অন্য দোকানে বকেয়া বাকি পরিশোধ করেননি।",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CharcoalDark, fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🔒 গোপনীয়তা রক্ষা: তথ্যটি সম্পূর্ণ অ্যানোনিমাস/বেনামী রাখা হয়েছে।",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CharcoalMuted, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats breakdown Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Safe credit limit recommendation
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RoyalBlue50),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("প্রস্তাবিত বাকি লিমিট", style = MaterialTheme.typography.labelSmall.copy(color = CharcoalMuted, fontSize = 10.sp))
                            Text(
                                "৳${BengaliFormatters.toBanglaCurrency(trustScoreResult.recommendedCreditLimit)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = RoyalBlue700)
                            )
                        }
                    }

                    // Active Store Due
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Amber50),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("বর্তমান বকেয়া", style = MaterialTheme.typography.labelSmall.copy(color = CharcoalMuted, fontSize = 10.sp))
                            Text(
                                "৳${BengaliFormatters.toBanglaCurrency(trustScoreResult.activeDueAmount)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Amber700)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scoring Factors list
                if (trustScoreResult.positiveFactors.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("✅ ইতিবাচক পয়েন্ট:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Emerald700))
                        Spacer(modifier = Modifier.height(4.dp))
                        trustScoreResult.positiveFactors.forEach { factor ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(factor, style = MaterialTheme.typography.bodySmall.copy(color = CharcoalDark, fontSize = 11.sp))
                            }
                        }
                    }
                }

                if (trustScoreResult.riskFactors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("⚠️ ঝুঁকি পয়েন্ট:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Red700))
                        Spacer(modifier = Modifier.height(4.dp))
                        trustScoreResult.riskFactors.forEach { factor ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Red700, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(factor, style = MaterialTheme.typography.bodySmall.copy(color = CharcoalDark, fontSize = 11.sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Anonymous Defaulter Report Sheet Toggle
                if (!showReportSheet) {
                    OutlinedButton(
                        onClick = { showReportSheet = true },
                        modifier = Modifier.fillMaxWidth().testTag("btn_show_report_defaulter"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
                        border = BorderStroke(1.dp, Red200),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Red600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("বেনামে ডিফল্টার রিপোর্ট করুন (Defaulter Mark)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🚨 কাস্টমারকে বেনামে ডিফল্টার হিসেবে যোগ করুন",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Red700)
                            )
                            Text(
                                text = "আপনার নাম বা দোকানের পরিচয় সম্পূর্ণ গোপন থাকবে। শুধু এই নম্বরে কমিউনিটি অ্যালার্ট তৈরি হবে।",
                                style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted, fontSize = 10.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = reportAmount,
                                onValueChange = { reportAmount = it },
                                label = { Text("অপরিশোধিত টাকার পরিমাণ (৳)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = reportReason,
                                onValueChange = { reportReason = it },
                                label = { Text("বকেয়া বিবরণ / কারণ") },
                                placeholder = { Text("যেমন: বারবার তাগাদা দেওয়ার পরও ৩ মাস ধরে টাকা দিচ্ছে না") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { showReportSheet = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("বাতিল", color = CharcoalMuted)
                                }

                                Button(
                                    onClick = {
                                        val amt = reportAmount.toDoubleOrNull() ?: 0.0
                                        val phone = customerPhone ?: ""
                                        onReportDefaulter(phone, customerName, amt, reportReason)
                                        showReportSheet = false
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_submit_defaulter_report"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Red700),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("রিপোর্ট জমা দিন", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
