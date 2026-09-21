package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AtomicProximityEngine
import com.example.util.BengaliFormatters
import com.example.util.NearbyCustomer
import kotlinx.coroutines.delay

/**
 * CustomerArrivalPopUp:
 * Displays floating radar alert banner when a customer with due enters the shop:
 * "রহিম ভাই দোকানে এসেছেন। ওনার বাকির পরিমাণ: ৪০০ টাকা।"
 */
@Composable
fun CustomerArrivalPopUp(
    onOpenSettlement: (NearbyCustomer) -> Unit
) {
    var activeArrival by remember { mutableStateOf<NearbyCustomer?>(null) }

    LaunchedEffect(Unit) {
        AtomicProximityEngine.customerArrivalEvent.collect { customer ->
            activeArrival = customer
            // Auto-hide after 8 seconds if not clicked
            delay(8000)
            if (activeArrival == customer) {
                activeArrival = null
            }
        }
    }

    AnimatedVisibility(
        visible = activeArrival != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .statusBarsPadding()
    ) {
        activeArrival?.let { customer ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clickable {
                        val selected = customer
                        activeArrival = null
                        onOpenSettlement(selected)
                    }
                    .testTag("customer_arrival_banner"),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, DeepIndigo.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    DeepIndigo.copy(alpha = 0.05f),
                                    Color.White
                                )
                            )
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing radar icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(DeepIndigo, Color(0xFF3730A3)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "প্রক্সিমিটি ডিটেকশন",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ ${customer.name} দোকানে এসেছেন",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = CharcoalDark
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "ওনার বাকির পরিমাণ: ${BengaliFormatters.toBanglaCurrency(customer.totalDue)} (${customer.distanceMeters} মিটার)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = CrimsonError
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 1-Tap Settle Due Button
                    Button(
                        onClick = {
                            val selected = customer
                            activeArrival = null
                            onOpenSettlement(selected)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear Due",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { activeArrival = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = CharcoalMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
