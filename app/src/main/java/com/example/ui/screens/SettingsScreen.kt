package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DokanTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

@Composable
fun SettingsScreen(
    viewModel: ShopViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()

    var shopName by remember(shopProfile) { mutableStateOf(shopProfile?.shopName ?: "আমার ব্যবসা") }
    var ownerName by remember(shopProfile) { mutableStateOf(shopProfile?.ownerName ?: "") }
    var phone by remember(shopProfile) { mutableStateOf(shopProfile?.phone ?: "") }
    var address by remember(shopProfile) { mutableStateOf(shopProfile?.address ?: "") }

    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = "দোকানের প্রোফাইল ও সেটিংস",
                subtitle = "ব্যবসার মৌলিক তথ্য পরিবর্তন",
                dueAlertCount = dueReminders.size
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = RoyalBlue800,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "দোকানের তথ্য",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalDark
                                )
                            )
                            Text(
                                text = "রসিদ ও মেসেজে প্রদর্শিত হবে",
                                style = MaterialTheme.typography.bodySmall.copy(color = CharcoalLight)
                            )
                        }
                    }

                    Divider()

                    // Shop Name
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("দোকানের নাম") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = RoyalBlue700) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_shop_name_input")
                    )

                    // Owner Name
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("মালিকের নাম") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBlue700) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_owner_name_input")
                    )

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RoyalBlue700) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_phone_input")
                    )

                    // Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("দোকানের ঠিকানা") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoyalBlue700) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_address_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.saveShopProfile(shopName, ownerName, phone, address)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_shop_profile_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("তথ্য সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Free WhatsApp Automation Info Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald50),
                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WhatsApp অটোমেটিক মেসেজ (বিনা মূল্যে)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Emerald900
                            )
                        )
                    }
                    Text(
                        text = "• কোনো রিচার্জ বা API ফি ছাড়াই সরাসরি কাস্টমারের নম্বরে বাকি রিমাইন্ডার ও বিক্রয় রসিদ পাঠানো হয়।\n• এক ক্লিকে সম্পূর্ণ মেসেজ বাংলা ভাষায় প্রস্তুত হয়ে WhatsApp-এ ওপেন হবে।",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CharcoalMedium,
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // Data Management / Reset Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ডাটা ব্যবস্থাপনা",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )
                    Text(
                        text = "ডাটাবেজ সম্পূর্ণ খালি করে নতুন করে ফ্রেশ শুরু করতে নিচের বাটনে চাপ দিন।",
                        style = MaterialTheme.typography.bodySmall.copy(color = CharcoalLight)
                    )

                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
                        border = BorderStroke(1.dp, Red500),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সকল পণ্য ও লেনদেন মুছুন", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // App Version & Credits
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = LightGraySurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "দোকান ও QR হিসাব অ্যাপ v1.0",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )
                    Text(
                        text = "আধুনিক ও প্রিমিয়াম বাংলা ব্যবসা ব্যবস্থাপনা",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CharcoalLight,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("সকল তথ্য মুছে ফেলবেন?") },
            text = { Text("ডাটাবেজের সকল পণ্য ও লেনদেনের তথ্য চিরতরে মুছে যাবে এবং অ্যাপ নতুন অবস্থায় রিসেট হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("হ্যাঁ, সব মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
