package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.BengaliFormatters
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DokanTopBar(
    title: String = "আমার ব্যবসা",
    subtitle: String? = null,
    dueAlertCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = DarkCharcoal,
                        letterSpacing = (-0.5).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle ?: "আজ: ${BengaliFormatters.formatDateBangla(System.currentTimeMillis())}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CharcoalLight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Due Notification Button with clean white card & badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onNotificationClick() }
                        .testTag("notification_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BadgedBox(
                            badge = {
                                if (dueAlertCount > 0) {
                                    Badge(
                                        containerColor = Orange500,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = BengaliFormatters.toBanglaNumber(dueAlertCount),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "নোটিফিকেশন",
                                tint = if (dueAlertCount > 0) Orange500 else DeepIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Profile Avatar Button (initial letter badge in clean card)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onProfileClick() }
                        .testTag("profile_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue50),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = title.firstOrNull()?.toString() ?: "দ"
                            Text(
                                text = initial,
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigo,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector? = null,
    iconBgColor: Color = RoyalBlue50,
    iconTintColor: Color = DeepIndigo,
    valueColor: Color = DarkCharcoal,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CharcoalMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTintColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = valueColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!subValue.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = CharcoalLight
                    )
                )
            }
        }
    }
}

@Composable
fun BengaliEmptyState(
    icon: ImageVector = Icons.Outlined.Inventory2,
    title: String,
    description: String? = null,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(RoyalBlue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RoyalBlue700,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CharcoalDark,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CharcoalLight,
                    fontSize = 13.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        if (!buttonText.isNullOrBlank() && onButtonClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DueStatusBadge(
    isPaid: Boolean,
    dueDate: Long?,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when {
        isPaid -> Triple(Emerald50, Emerald700, "পরিশোধিত")
        dueDate != null && BengaliFormatters.isToday(dueDate) -> Triple(Orange50, Orange600, "আজ পরিশোধের দিন")
        dueDate != null && BengaliFormatters.isOverdue(dueDate) -> Triple(Red50, Red600, "বকেয়া (মেয়াদ শেষ)")
        else -> Triple(RoyalBlue50, RoyalBlue700, "বাকি")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun QrPreviewImage(
    qrCodeText: String,
    sizeDp: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(qrCodeText) {
        QrCodeGenerator.generateQrBitmap(qrCodeText, size = 300)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code: $qrCodeText",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = CharcoalLight,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
