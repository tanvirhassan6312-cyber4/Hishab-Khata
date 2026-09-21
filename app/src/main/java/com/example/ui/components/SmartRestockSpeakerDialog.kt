package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ProductEntity
import com.example.data.local.ShopProfileEntity
import com.example.ui.theme.*
import com.example.util.BengaliFormatters
import com.example.util.SmartReStockSpeakerHelper

@Composable
fun SmartRestockSpeakerDialog(
    lowStockProducts: List<ProductEntity>,
    shopProfile: ShopProfileEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedProductForOrder by remember { mutableStateOf<ProductEntity?>(null) }
    var customOrderQty by remember { mutableStateOf("") }
    var mohajonPhone by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            SmartReStockSpeakerHelper.stopSpeaking()
        }
    }

    Dialog(onDismissRequest = {
        SmartReStockSpeakerHelper.stopSpeaking()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("dialog_smart_restock")
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
                            color = Amber50,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Amber700)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "স্মার্ট রি-স্টক স্পিকার",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DeepIndigo)
                            )
                            Text(
                                text = "মালামাল ফুরানোর ভয়েস সতর্কবার্তা ও পাইকারি অর্ডার",
                                style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted, fontSize = 11.sp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CharcoalMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (lowStockProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald700, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("সব পণ্যে পর্যাপ্ত স্টক রয়েছে!", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark))
                            Text("আপাতত কোনো মালামাল শেষ হওয়ার ঝুঁকি নেই।", style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted))
                        }
                    }
                } else {
                    Text(
                        text = "⚠️ ${BengaliFormatters.toBanglaNumber(lowStockProducts.size)}টি পণ্যের স্টক শেষ হওয়ার পথে:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Red700),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lowStockProducts) { product ->
                            val wholesaleEst = SmartReStockSpeakerHelper.estimateWholesalePrice(product)
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                border = BorderStroke(1.dp, if (product.stock <= 2) Red200 else Amber200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                                            )
                                            Text(
                                                text = "বর্তমান স্টক: ${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit} (মিনিমাম: ৫)",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (product.stock <= 0) Red600 else Amber700,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }

                                        // Speaker Audio Button
                                        IconButton(
                                            onClick = {
                                                SmartReStockSpeakerHelper.speakRestockAlert(context, product, shopProfile)
                                            },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Amber100, CircleShape)
                                        ) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "স্পিকার শুনুন", tint = Amber800, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Wholesale market estimate & order button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RoyalBlue50
                                        ) {
                                            Text(
                                                text = "পাইকারি দর: ৳${BengaliFormatters.toBanglaCurrency(wholesaleEst)} / ${product.unit}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RoyalBlue700),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                selectedProductForOrder = product
                                                customOrderQty = "20"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("অর্ডার পাঠান", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modal for WhatsApp Purchase Order Generator
                AnimatedVisibility(visible = selectedProductForOrder != null) {
                    selectedProductForOrder?.let { prod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Emerald50),
                            border = BorderStroke(1.dp, Emerald200)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🛒 পাইকারি মহাজনের কাছে WhatsApp স্লিপ",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Emerald800)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = customOrderQty,
                                    onValueChange = { customOrderQty = it },
                                    label = { Text("অর্ডারের পরিমাণ (${prod.unit})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = mohajonPhone,
                                    onValueChange = { mohajonPhone = it },
                                    label = { Text("মহাজন / ডিলারের মোবাইল নম্বর (ঐচ্ছিক)") },
                                    placeholder = { Text("০১৭XXXXXXXX") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = { selectedProductForOrder = null },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("বাতিল", color = CharcoalMuted)
                                    }

                                    Button(
                                        onClick = {
                                            val qty = customOrderQty.toDoubleOrNull() ?: 10.0
                                            SmartReStockSpeakerHelper.shareWholesaleOrderViaWhatsApp(
                                                context = context,
                                                product = prod,
                                                suggestedOrderQty = qty,
                                                shopProfile = shopProfile,
                                                mohajonPhone = mohajonPhone
                                            )
                                            selectedProductForOrder = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp-এ পাঠান", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
