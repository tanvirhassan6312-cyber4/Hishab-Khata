package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sensors
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

/**
 * StartupPermissionHandler:
 * As requested:
 * 1. "আর এই অ্যাপ এ ঢুকলেই 'অ্যাপ ডেভোলোপার তাফসির এবং তানভির' এটি আসবে।"
 * 2. "আর ক্যামেরা পারমিশন এবং মাইক্রোফোন পারমিশন দিতে বলবে।"
 */
@Composable
fun StartupPermissionHandler() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("dokan_permissions_prefs", Context.MODE_PRIVATE) }
    var hasPromptedBefore by remember { mutableStateOf(sharedPrefs.getBoolean("has_prompted_camera_mic", false)) }

    val isCameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val isAudioGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val needsPermission = (!isCameraGranted || !isAudioGranted) && !hasPromptedBefore
    var showDialog by remember { mutableStateOf(needsPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        sharedPrefs.edit().putBoolean("has_prompted_camera_mic", true).apply()
        hasPromptedBefore = true
        showDialog = false
        Toast.makeText(context, "ধন্যবাদ! অ্যাপ ডেভেলপার: তাফসির এবং তানভির", Toast.LENGTH_SHORT).show()
    }

    // Greet developer attribution on app startup toast
    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "স্বাগতম! অ্যাপ ডেভেলপার: তাফসির এবং তানভির",
            Toast.LENGTH_LONG
        ).show()

        if (needsPermission) {
            val permissionsList = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            permissionLauncher.launch(permissionsList)
        }
    }

    if (showDialog && (!isCameraGranted || !isAudioGranted)) {
        Dialog(
            onDismissRequest = {
                sharedPrefs.edit().putBoolean("has_prompted_camera_mic", true).apply()
                showDialog = false
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .testTag("startup_permission_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Developer Attribution Badge Header
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = RoyalBlue50,
                        border = BorderStroke(1.dp, RoyalBlue200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = DeepIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "অ্যাপ ডেভেলপার: তাফসির এবং তানভির",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Emerald50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = DeepIndigo,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ক্যামেরা ও মাইক্রোফোন পারমিশন",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark,
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "দোকানের হিসাব সহজ করতে ক্যামেরা ও মাইক্রোফোন পারমিশন একবার অনুমোদন দিন। পরে আর কখনো পারমিশন চাইবে না।",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CharcoalMuted,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PermissionBenefitRow(
                            icon = Icons.Default.CameraAlt,
                            title = "📷 ক্যামেরা পারমিশন",
                            desc = "দ্রুত কিউআর ও বারকোড স্ক্যানের জন্য"
                        )
                        PermissionBenefitRow(
                            icon = Icons.Default.Mic,
                            title = "🎙️ মাইক্রোফোন পারমিশন",
                            desc = "ভয়েস লেজার ও টাচলেস অডিও পেমেন্ট ভেরিফিকেশনের জন্য"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val permissionsList = arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                            permissionLauncher.launch(permissionsList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পারমিশন অনুমোদন করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = {
                            sharedPrefs.edit().putBoolean("has_prompted_camera_mic", true).apply()
                            showDialog = false
                        }
                    ) {
                        Text("এখন নয়, পরে দেব", color = CharcoalMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Emerald700, modifier = Modifier.size(15.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalDark)
            Text(text = desc, fontSize = 10.5.sp, color = CharcoalMuted)
        }
    }
}
