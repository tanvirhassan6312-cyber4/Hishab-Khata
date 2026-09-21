package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.SaleTransactionEntity
import com.example.data.local.ShopProfileEntity
import com.example.ui.theme.*
import com.example.util.AtomicProximityEngine
import com.example.util.AtomicSettlementReceipt
import com.example.util.BengaliFormatters
import com.example.util.NearbyCustomer
import kotlinx.coroutines.launch

/**
 * Atomic Proximity Pay & Contactless Due Settlement Dialog:
 *
 * Feature highlights:
 * 1. Automatic Proximity Tracking (BLE / Acoustic Radar)
 * 2. Encrypted Acoustic Audio Handshake (Near-ultrasonic ~18.5kHz inaudible sound pulse)
 * 3. Printless E-Receipt & Loud Bangla Speech Confirmation ("রহিম ভাইয়ের ৪০০ টাকা বাকি সম্পূর্ণ পরিশোধিত হয়েছে")
 */
@Composable
fun AtomicProximityDialog(
    initialCustomer: NearbyCustomer? = null,
    shopProfile: ShopProfileEntity?,
    allTransactions: List<SaleTransactionEntity>,
    onDismiss: () -> Unit,
    onDueSettled: (SaleTransactionEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val nearbyCustomers by AtomicProximityEngine.nearbyCustomers.collectAsState()
    var selectedCustomer by remember { mutableStateOf(initialCustomer ?: nearbyCustomers.firstOrNull()) }

    // Settlement Workflow States
    var isSettling by remember { mutableStateOf(false) }
    var settleProgress by remember { mutableFloatStateOf(0f) }
    var settleStatusText by remember { mutableStateOf("") }
    var completedReceipt by remember { mutableStateOf<AtomicSettlementReceipt?>(null) }

    // Sync nearby list with current DB
    LaunchedEffect(allTransactions) {
        AtomicProximityEngine.syncWithDueTransactions(context, allTransactions)
        if (selectedCustomer == null && nearbyCustomers.isNotEmpty()) {
            selectedCustomer = nearbyCustomers.firstOrNull()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSettling) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSettling,
            dismissOnClickOutside = !isSettling
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalDark.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(24.dp, RoundedCornerShape(28.dp))
                    .testTag("atomic_proximity_dialog"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(DeepIndigo, Color(0xFF4338CA)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "অ্যাটোমিক প্রক্সিমিটি পে",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = CharcoalDark
                                    )
                                )
                                Text(
                                    text = "টাচলেস অফলাইন অডিও হ্যান্ডশেক",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = CharcoalMuted
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { if (!isSettling) onDismiss() },
                            enabled = !isSettling
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = CharcoalMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (completedReceipt != null) {
                        // 3. E-Receipt & Voice Confirmation View
                        SettlementReceiptView(
                            receipt = completedReceipt!!,
                            onShare = {
                                val shareText = AtomicProximityEngine.buildDigitalReceiptShareText(completedReceipt!!)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "ডিজিটাল রসিদ শেয়ার করুন"))
                            },
                            onDone = {
                                completedReceipt = null
                                onDismiss()
                            }
                        )
                    } else if (isSettling) {
                        // 2. Active Inaudible Ultrasonic Handshake Transmitting View
                        ActiveHandshakeView(
                            customer = selectedCustomer,
                            progress = settleProgress,
                            status = settleStatusText
                        )
                    } else {
                        // 1. Radar Discovery & Customer Selection View
                        RadarDiscoveryView(
                            nearbyCustomers = nearbyCustomers,
                            selectedCustomer = selectedCustomer,
                            onSelectCustomer = { selectedCustomer = it },
                            onTestWalkIn = {
                                val simulated = AtomicProximityEngine.triggerCustomerWalkIn(
                                    context = context,
                                    customerName = "রহিম",
                                    customerPhone = "০১৭৯৮৭৬৫৪৩২",
                                    dueAmount = 400.0,
                                    transactions = allTransactions.filter { it.isDue }
                                )
                                selectedCustomer = simulated
                                Toast.makeText(context, "🔔 রহিম ভাই দোকানে এসেছেন (৪০০ টাকা বাকি)", Toast.LENGTH_SHORT).show()
                            },
                            onClearDueClick = {
                                val target = selectedCustomer
                                if (target != null) {
                                    isSettling = true
                                    settleProgress = 0.05f
                                    settleStatusText = "হ্যান্ডশেক শুরু হচ্ছে..."

                                    coroutineScope.launch {
                                        val receipt = AtomicProximityEngine.executeContactlessDueSettlement(
                                            context = context,
                                            customer = target,
                                            amountToSettle = target.totalDue,
                                            shopProfile = shopProfile,
                                            onProgress = { p, txt ->
                                                settleProgress = p
                                                settleStatusText = txt
                                            }
                                        )

                                        // Mark all related transactions as paid in DB
                                        target.relatedTransactions.forEach { tx ->
                                            onDueSettled(tx)
                                        }

                                        isSettling = false
                                        completedReceipt = receipt
                                    }
                                } else {
                                    Toast.makeText(context, "কোনো কাস্টমার সিলেক্ট করা নেই", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1. Live Radar Scanner View
 */
@Composable
private fun RadarDiscoveryView(
    nearbyCustomers: List<NearbyCustomer>,
    selectedCustomer: NearbyCustomer?,
    onSelectCustomer: (NearbyCustomer) -> Unit,
    onTestWalkIn: () -> Unit,
    onClearDueClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated Concentric Pulse Radar Visualizer
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)))),
            contentAlignment = Alignment.Center
        ) {
            RadarConcentricRings()

            // Center Beacon Tower Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Emerald500, Emerald700))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Radar Status Tag & Test Simulator Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Emerald50,
                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Emerald600)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ব্লুটুথ ও আল্ট্রাসনিক সেন্সর সক্রিয়",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }

            TextButton(
                onClick = onTestWalkIn,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.AddReaction, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepIndigo)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ওয়াক-ইন টেস্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of Detected Nearby Customers or Empty State
        if (nearbyCustomers.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Slate100,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = CharcoalMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "আশেপাশে কোনো বাকি থাকা কাস্টমার পাওয়া যায়নি",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalDark
                    )
                    Text(
                        text = "কাস্টমার দোকানে প্রবেশ করলে স্বয়ংক্রিয়ভাবে ফোনে ভাইব্রেশন ও নোটিফিকেশন আসবে। উপরের 'ওয়াক-ইন টেস্ট' দিয়ে এখনই পরীক্ষা করতে পারেন।",
                        fontSize = 11.sp,
                        color = CharcoalMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Text(
                text = "দোকানে শনাক্তকৃত কাস্টমার (${BengaliFormatters.toBanglaNumber(nearbyCustomers.size)} জন):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(nearbyCustomers) { customer ->
                    val isSelected = selectedCustomer?.phone == customer.phone
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCustomer(customer) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DeepIndigo.copy(alpha = 0.08f) else Slate50,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) DeepIndigo else CardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) DeepIndigo else Slate300),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.take(1).ifBlank { "ক" },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CharcoalDark
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "দূরত্ব: ${customer.distanceMeters} মি • সিগন্যাল: ${customer.signalQuality}",
                                        fontSize = 10.5.sp,
                                        color = CharcoalMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = BengaliFormatters.toBanglaCurrency(customer.totalDue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CrimsonError
                                )
                                Text(
                                    text = "বাকি আছে",
                                    fontSize = 9.5.sp,
                                    color = CharcoalMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Action Button: "Clear Due" with Contactless Ultrasound
        Button(
            onClick = onClearDueClick,
            enabled = selectedCustomer != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Emerald700,
                disabledContainerColor = Slate300
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_clear_due_acoustic")
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedCustomer != null) {
                    "Clear Due (অফলাইন সাউন্ড দিয়ে পরিশোধ)"
                } else {
                    "কাস্টমার শনাক্ত করুন"
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "🔒 কোনো অ্যাপ বা কিউআর কোড স্ক্যান ছাড়াই কাস্টমারের ফোনে অফলাইন অডিও সিগন্যালে নিষ্পত্তি হবে।",
            fontSize = 10.sp,
            color = CharcoalMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 2. Active Acoustic Handshake Animated View
 */
@Composable
private fun ActiveHandshakeView(
    customer: NearbyCustomer?,
    progress: Float,
    status: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-Frequency Wave Pulse Animation
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Emerald500.copy(alpha = 0.2f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            AcousticWaveVisualizer()
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Emerald700, Color(0xFF064E3B)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "নিরাপদ অফলাইন অ্যাকোস্টিক হ্যান্ডশেক",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CharcoalDark
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "১৮.৫ kHz আল্ট্রাসনিক এনক্রিপ্টেড পালস ট্রান্সমিট হচ্ছে...",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = Emerald700,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Emerald700,
            trackColor = Slate200
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = CharcoalMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Slate100,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SHA-256 জিরো-নলেজ অফলাইন ক্রিপ্টোগ্রাফি ভেরিফায়েড",
                    fontSize = 10.5.sp,
                    color = CharcoalDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 3. Settlement Receipt & Voice Confirmation View
 */
@Composable
private fun SettlementReceiptView(
    receipt: AtomicSettlementReceipt,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Badge
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Emerald500, Emerald700))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "বাকি সম্পূর্ণ পরিশোধিত হয়েছে ✓",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Emerald800
            )
        )

        Text(
            text = "লাউড স্পিকারে বাংলায় ঘোষণা সম্পন্ন হয়েছে",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                color = CharcoalMuted
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Digital E-Receipt Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ডিজিটাল ই-রসিদ টোকেন", fontSize = 11.sp, color = CharcoalMuted)
                    Text(
                        text = receipt.signatureToken,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = DeepIndigo
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorder)

                ReceiptInfoRow("গ্রাহকের নাম:", receipt.customerName)
                ReceiptInfoRow("মোবাইল নম্বর:", receipt.customerPhone)
                ReceiptInfoRow("পরিশোধিত বকেয়া:", BengaliFormatters.toBanglaCurrency(receipt.settledAmount), isBold = true, valueColor = Emerald700)
                ReceiptInfoRow("পেমেন্ট মেথড:", "Atomic Proximity (অফলাইন)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("রসিদ শেয়ার", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("সম্পন্ন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = CharcoalDark
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = CharcoalMuted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * Radar Concentric Expanding Animated Rings
 */
@Composable
private fun RadarConcentricRings() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2

        // Base concentric rings
        drawCircle(
            color = Color(0xFF312E81).copy(alpha = 0.4f),
            radius = maxRadius * 0.35f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF312E81).copy(alpha = 0.4f),
            radius = maxRadius * 0.65f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF312E81).copy(alpha = 0.4f),
            radius = maxRadius * 0.95f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Expanding Pulse Ring
        drawCircle(
            color = Emerald500.copy(alpha = (1.0f - ringScale).coerceIn(0f, 1f)),
            radius = maxRadius * ringScale,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Acoustic Wave Visualizer Animation
 */
@Composable
private fun AcousticWaveVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2

        for (i in 1..3) {
            val scale = ((waveOffset + (i * 0.33f)) % 1f)
            val alpha = (1f - scale).coerceIn(0f, 0.8f)
            drawCircle(
                color = Emerald500.copy(alpha = alpha),
                radius = maxRadius * scale,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}
