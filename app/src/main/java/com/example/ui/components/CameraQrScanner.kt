package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.local.ProductEntity
import com.example.ui.theme.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.Executors

@Composable
fun CameraQrScanner(
    onCodeScanned: (String) -> Unit,
    availableProductsWithQr: List<ProductEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var manualCodeInput by remember { mutableStateOf("") }
    var scanCooldown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalDark)
    ) {
        // Camera Viewport Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (hasCameraPermission) {
                CameraPreviewView(
                    onQrDetected = { code ->
                        if (!scanCooldown) {
                            scanCooldown = true
                            triggerHaptic(context)
                            onCodeScanned(code)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Permission Request Box
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ক্যামেরা দিয়ে QR ও বারকোড স্ক্যান",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "পণ্য সরাসরি স্ক্যান করে তথ্য দেখতে ও বিক্রি করতে ক্যামেরার অনুমতি দিন",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = RoyalBlue100,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("request_camera_permission_button")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ক্যামেরার অনুমতি দিন", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scanner Overlay with Laser Line
            ScannerOverlay(modifier = Modifier.fillMaxSize())

            // Top Status Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Emerald500)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "লাইভ ক্যামেরা স্ক্যানার",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (scanCooldown) {
                    Button(
                        onClick = { scanCooldown = false },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পুনরায় স্ক্যান", fontSize = 12.sp)
                    }
                }
            }

            // Bottom Instruction
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "পণ্যের QR কোড বা বারকোড ক্যামেরার সামনে ধরুন",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Quick Input & Stored Product Selector
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "ম্যানুয়াল কোড লিখুন অথবা সংরক্ষিত পণ্য বেছে নিন:",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        placeholder = { Text("যেমন: P-001 বা QR কোড") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = RoyalBlue700)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_qr_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (manualCodeInput.isNotBlank()) {
                                onCodeScanned(manualCodeInput.trim())
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("manual_qr_submit")
                    ) {
                        Text("খুঁজুন", fontWeight = FontWeight.Bold)
                    }
                }

                if (availableProductsWithQr.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "দোকানের সংরক্ষিত QR পণ্য (এক ট্যাপে ওপেন করুন):",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CharcoalLight,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableProductsWithQr.take(3).forEach { product ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RoyalBlue50,
                                border = BorderStroke(1.dp, RoyalBlue100),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        product.qrCode?.let { onCodeScanned(it) }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBlue800,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = product.qrCode ?: "",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Emerald700,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val scanBoxSize = size.width * 0.72f
        val left = (size.width - scanBoxSize) / 2f
        val top = (size.height - scanBoxSize) / 2.3f
        val right = left + scanBoxSize
        val bottom = top + scanBoxSize

        // Dim surrounding background
        drawRect(
            color = Color.Black.copy(alpha = 0.55f),
            size = size
        )

        // Clear middle box with rounded corners
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scanBoxSize, scanBoxSize),
            cornerRadius = CornerRadius(24.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Draw scan border frame
        drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(left, top),
            size = Size(scanBoxSize, scanBoxSize),
            cornerRadius = CornerRadius(24.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // Corner highlights
        val cornerLen = 28.dp.toPx()
        val cornerWidth = 5.dp.toPx()

        // Top Left
        drawLine(Color(0xFF38BDF8), Offset(left - 2, top + cornerLen), Offset(left - 2, top), cornerWidth)
        drawLine(Color(0xFF38BDF8), Offset(left, top - 2), Offset(left + cornerLen, top - 2), cornerWidth)

        // Top Right
        drawLine(Color(0xFF38BDF8), Offset(right + 2, top + cornerLen), Offset(right + 2, top), cornerWidth)
        drawLine(Color(0xFF38BDF8), Offset(right, top - 2), Offset(right - cornerLen, top - 2), cornerWidth)

        // Bottom Left
        drawLine(Color(0xFF38BDF8), Offset(left - 2, bottom - cornerLen), Offset(left - 2, bottom), cornerWidth)
        drawLine(Color(0xFF38BDF8), Offset(left, bottom + 2), Offset(left + cornerLen, bottom + 2), cornerWidth)

        // Bottom Right
        drawLine(Color(0xFF38BDF8), Offset(right + 2, bottom - cornerLen), Offset(right + 2, bottom), cornerWidth)
        drawLine(Color(0xFF38BDF8), Offset(right, bottom + 2), Offset(right - cornerLen, bottom + 2), cornerWidth)

        // Animated Laser Beam
        val laserY = top + (scanBoxSize * laserPosition)
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    Color(0xFF10B981).copy(alpha = 0.9f),
                    Color(0xFF38BDF8),
                    Color(0xFF10B981).copy(alpha = 0.9f),
                    Color.Transparent
                )
            ),
            start = Offset(left + 10.dp.toPx(), laserY),
            end = Offset(right - 10.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

@Composable
private fun CameraPreviewView(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val multiFormatReader = remember {
        MultiFormatReader().apply {
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(
                    DecodeHintType.POSSIBLE_FORMATS,
                    listOf(
                        BarcodeFormat.QR_CODE,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.DATA_MATRIX
                    )
                )
                put(DecodeHintType.TRY_HARDER, true)
            }
            setHints(hints)
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val qrCode = decodeImageProxy(imageProxy, multiFormatReader)
                    if (qrCode != null) {
                        previewView.post {
                            onQrDetected(qrCode)
                        }
                    }
                    imageProxy.close()
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

private fun decodeImageProxy(image: ImageProxy, reader: MultiFormatReader): String? {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val width = image.width
    val height = image.height

    // Extract exact luminance bytes
    val data = if (rowStride == width && pixelStride == 1) {
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        bytes
    } else {
        val bytes = ByteArray(width * height)
        val row = ByteArray(rowStride)
        for (y in 0 until height) {
            buffer.position(y * rowStride)
            buffer.get(row, 0, Math.min(rowStride, buffer.remaining()))
            for (x in 0 until width) {
                bytes[y * width + x] = row[x * pixelStride]
            }
        }
        bytes
    }

    // Try 1: Standard orientation
    val source1 = PlanarYUVLuminanceSource(
        data, width, height, 0, 0, width, height, false
    )
    val bitmap1 = BinaryBitmap(HybridBinarizer(source1))
    try {
        val result = reader.decodeWithState(bitmap1)
        return result.text
    } catch (_: Exception) {
    } finally {
        reader.reset()
    }

    // Try 2: Rotated 90 degrees (Portrait camera orientation standard)
    val rotatedData = ByteArray(data.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            rotatedData[x * height + height - y - 1] = data[x + y * width]
        }
    }
    val source2 = PlanarYUVLuminanceSource(
        rotatedData, height, width, 0, 0, height, width, false
    )
    val bitmap2 = BinaryBitmap(HybridBinarizer(source2))
    return try {
        val result = reader.decodeWithState(bitmap2)
        result.text
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }
}

private fun triggerHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(70)
            }
        }
    } catch (e: Exception) {
        // ignore
    }
}
