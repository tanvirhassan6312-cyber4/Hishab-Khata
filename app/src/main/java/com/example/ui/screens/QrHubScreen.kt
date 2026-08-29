package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProductEntity
import com.example.data.local.SaleTransactionEntity
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.CameraQrScanner
import com.example.ui.components.DokanTopBar
import com.example.ui.components.QrPreviewImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters
import com.example.util.ReminderUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrHubScreen(
    viewModel: ShopViewModel,
    initialSubTab: Int = 0, // 0: Scan, 1: Create, 2: History
    onNavigateBack: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(initialSubTab) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val productsWithQr by viewModel.productsWithQr.collectAsStateWithLifecycle()
    val filteredQrProducts by viewModel.filteredQrProducts.collectAsStateWithLifecycle()
    val qrSearchQuery by viewModel.qrSearchQuery.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()

    // Dialog States
    var scannedProductDetails by remember { mutableStateOf<ProductEntity?>(null) }
    var activeProductForSale by remember { mutableStateOf<ProductEntity?>(null) }
    var productForRestock by remember { mutableStateOf<ProductEntity?>(null) }
    var productForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var scannedProductNotFoundCode by remember { mutableStateOf<String?>(null) }
    var createdProductPreview by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = "QR কোড হাব",
                subtitle = "ক্যামেরা স্ক্যান, তৈরি ও স্টক ব্যবস্থাপনা",
                dueAlertCount = dueReminders.size
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Switch / Tab Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton(
                        title = "① QR Scan",
                        icon = Icons.Default.QrCodeScanner,
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_qr_scan")
                    )

                    TabButton(
                        title = "② QR Create",
                        icon = Icons.Default.AddCircleOutline,
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_qr_create")
                    )

                    TabButton(
                        title = "QR হিস্টোরি",
                        icon = Icons.Default.HistoryEdu,
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_qr_history")
                    )
                }
            }

            // Tab Contents
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> {
                        // ① QR SCANNER (Camera live scanner + manual search)
                        CameraQrScanner(
                            onCodeScanned = { code ->
                                scope.launch {
                                    val matchedProduct = viewModel.getProductByQrCode(code)
                                    if (matchedProduct != null) {
                                        scannedProductDetails = matchedProduct
                                    } else {
                                        scannedProductNotFoundCode = code
                                    }
                                }
                            },
                            availableProductsWithQr = productsWithQr,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    1 -> {
                        // ② QR CREATOR FORM
                        QrCreateForm(
                            viewModel = viewModel,
                            onQrCreated = { product ->
                                createdProductPreview = product
                            }
                        )
                    }

                    2 -> {
                        // QR CODE HISTORY
                        QrHistoryView(
                            products = filteredQrProducts,
                            searchQuery = qrSearchQuery,
                            onSearchChange = { viewModel.qrSearchQuery.value = it },
                            onSelectProductForDetails = { scannedProductDetails = it },
                            onSelectProductForSale = { activeProductForSale = it },
                            onViewQr = { createdProductPreview = it }
                        )
                    }
                }
            }
        }
    }

    // 1. Scanned Product Information Dialog (Shows all product details: Price, Name, Stock, etc.)
    scannedProductDetails?.let { product ->
        ScannedProductDetailsDialog(
            product = product,
            onDismiss = { scannedProductDetails = null },
            onSell = {
                scannedProductDetails = null
                activeProductForSale = product
            },
            onRestock = {
                scannedProductDetails = null
                productForRestock = product
            },
            onEdit = {
                scannedProductDetails = null
                productForEdit = product
            },
            onViewQr = {
                scannedProductDetails = null
                createdProductPreview = product
            }
        )
    }

    // 2. Sale Checkout Dialog (When QR is scanned or product selected to sell)
    activeProductForSale?.let { product ->
        SaleCheckoutDialog(
            product = product,
            shopkeeperDefaultName = shopProfile?.ownerName ?: "তানভির আহমেদ",
            onDismiss = { activeProductForSale = null },
            onConfirmSale = { qty, total, paid, due, isDue, custName, custPhone, shopkeeper, dueDate, note ->
                viewModel.processSale(
                    product = product,
                    quantitySold = qty,
                    totalAmount = total,
                    paidAmount = paid,
                    dueAmount = due,
                    isDue = isDue,
                    customerName = custName,
                    customerPhone = custPhone,
                    shopkeeperName = shopkeeper,
                    dueDate = dueDate,
                    note = note,
                    onSuccess = {
                        activeProductForSale = null
                    }
                )
            }
        )
    }

    // 3. Restock Dialog (মাল ক্রয় / স্টক বৃদ্ধি e.g. add 300)
    productForRestock?.let { product ->
        RestockDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { productForRestock = null }
        )
    }

    // 4. Edit Product Dialog
    productForEdit?.let { product ->
        EditProductDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { productForEdit = null }
        )
    }

    // 5. Scanned QR Not Found Dialog
    scannedProductNotFoundCode?.let { code ->
        AlertDialog(
            onDismissRequest = { scannedProductNotFoundCode = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = Orange600,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "পণ্য পাওয়া যায়নি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CharcoalDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "স্ক্যানকৃত QR Code: $code",
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue800,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "এই কোডের কোনো পণ্য ডাটাবেজে নেই। আপনি কি এই কোড দিয়ে নতুন পণ্য যোগ করতে চান?",
                        color = CharcoalMedium,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scannedProductNotFoundCode = null
                        selectedTab = 1 // Switch to create tab
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("নতুন পণ্য যোগ করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { scannedProductNotFoundCode = null }) {
                    Text("বাতিল", color = CharcoalLight)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 6. QR Code Preview & Share Dialog
    createdProductPreview?.let { product ->
        QrPreviewDialog(
            product = product,
            shopName = shopProfile?.shopName ?: "আমার ব্যবসা",
            onDismiss = { createdProductPreview = null },
            onSellNow = {
                createdProductPreview = null
                activeProductForSale = product
            }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else LightGraySurface,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else CharcoalMedium,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else CharcoalMedium,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------
// SCANNED PRODUCT DETAILS DIALOG (Shows all product details)
// -------------------------------------------------------------
@Composable
fun ScannedProductDetailsDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSell: () -> Unit,
    onRestock: () -> Unit,
    onEdit: () -> Unit,
    onViewQr: () -> Unit
) {
    val profit = (product.sellPrice - product.buyPrice).coerceAtLeast(0.0)
    val profitMargin = if (product.sellPrice > 0) (profit / product.sellPrice) * 100 else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR স্ক্যান সফল", color = Emerald700, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ")
                    }
                }

                // Product Name & QR Tag
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(color = RoyalBlue50, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "QR: ${product.qrCode ?: "নাই"}",
                                color = RoyalBlue800,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(color = LightGraySurface, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = product.category,
                                color = CharcoalMedium,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Divider()

                // Information Grid (All details: Sell Price, Buy Price, Stock, Profit)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sell Price Card
                        Surface(
                            color = RoyalBlue50,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("বিক্রয় মূল্য", fontSize = 11.sp, color = CharcoalLight)
                                Text(
                                    text = BengaliFormatters.toBanglaCurrency(product.sellPrice),
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue800,
                                    fontSize = 17.sp
                                )
                            }
                        }

                        // Buy Price Card
                        Surface(
                            color = LightGraySurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("ক্রয় মূল্য", fontSize = 11.sp, color = CharcoalLight)
                                Text(
                                    text = BengaliFormatters.toBanglaCurrency(product.buyPrice),
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalDark,
                                    fontSize = 17.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current Stock Card
                        Surface(
                            color = if (product.stock <= 5) Red50 else Emerald50,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (product.stock <= 5) Red500 else Emerald500),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("বর্তমান Stock (মজুদ)", fontSize = 11.sp, color = CharcoalLight)
                                Text(
                                    text = "${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.stock <= 5) Red600 else Emerald700,
                                    fontSize = 17.sp
                                )
                            }
                        }

                        // Profit Margin Card
                        Surface(
                            color = Sky50,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("সম্ভাব্য লাভ / একক", fontSize = 11.sp, color = CharcoalLight)
                                Text(
                                    text = "${BengaliFormatters.toBanglaCurrency(profit)} (${BengaliFormatters.toBanglaNumber(profitMargin.toInt())}%)",
                                    fontWeight = FontWeight.Bold,
                                    color = Sky700,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons: Sell (Decrements Stock), Restock (Increments Stock), Edit
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSell,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_scanned_sell")
                    ) {
                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পণ্য বিক্রি করুন (স্টক কমবে)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRestock,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue700),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_scanned_restock")
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ক্রয় / স্টক যোগ (+)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.8f).height(44.dp).testTag("btn_scanned_edit")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সম্পাদনা", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    TextButton(
                        onClick = onViewQr,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR কোড কার্ড প্রিভিউ ও প্রিন্ট", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// RESTOCK DIALOG (মাল ক্রয় / স্টক বৃদ্ধি e.g. add 300)
// -------------------------------------------------------------
@Composable
fun RestockDialog(
    product: ProductEntity,
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    var addStockInput by remember { mutableStateOf("") }
    val addStock = addStockInput.toIntOrNull() ?: 0
    val newTotalStock = product.stock + addStock
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "নতুন মাল ক্রয় / স্টক বৃদ্ধি",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ")
                    }
                }

                Text(
                    text = "পণ্য: ${product.name}",
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue800,
                    fontSize = 14.sp
                )

                Surface(
                    color = LightGraySurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("বর্তমান মজুদ (স্টক):", fontSize = 12.sp, color = CharcoalLight)
                        Text(
                            text = "${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit}",
                            fontWeight = FontWeight.Bold,
                            color = Emerald700,
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = addStockInput,
                    onValueChange = {
                        addStockInput = it
                        errorMessage = null
                    },
                    label = { Text("নতুন ক্রয়কৃত সংখ্যা লিখুন (${product.unit}) *") },
                    placeholder = { Text("যেমন: ৩০০") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null, tint = RoyalBlue700) },
                    modifier = Modifier.fillMaxWidth().testTag("input_restock_amount")
                )

                // Quick Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50, 100, 300, 500).forEach { qty ->
                        FilterChip(
                            selected = addStockInput == qty.toString(),
                            onClick = { addStockInput = qty.toString() },
                            label = { Text("+$qty", fontSize = 11.sp) }
                        )
                    }
                }

                if (addStock > 0) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("নতুন মোট স্টক হবে:", fontWeight = FontWeight.Bold, color = Emerald700, fontSize = 13.sp)
                            Text(
                                text = "${BengaliFormatters.toBanglaNumber(newTotalStock)} ${product.unit}",
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald700,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = Red600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("বাতিল")
                    }

                    Button(
                        onClick = {
                            if (addStock <= 0) {
                                errorMessage = "সঠিক সংখ্যা লিখুন (যেমন: ৩০০)"
                                return@Button
                            }
                            viewModel.restockProduct(product, addStock) {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f).testTag("btn_confirm_restock")
                    ) {
                        Text("স্টকে যোগ করুন (+)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SALE CHECKOUT DIALOG (Decrements stock)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleCheckoutDialog(
    product: ProductEntity,
    shopkeeperDefaultName: String,
    onDismiss: () -> Unit,
    onConfirmSale: (
        quantitySold: Int,
        totalAmount: Double,
        paidAmount: Double,
        dueAmount: Double,
        isDue: Boolean,
        customerName: String?,
        customerPhone: String?,
        shopkeeperName: String?,
        dueDate: Long?,
        note: String?
    ) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    val quantity = quantityText.toIntOrNull() ?: 1
    val calculatedTotal = product.sellPrice * quantity

    var customAmountText by remember { mutableStateOf("") }
    val totalAmount = customAmountText.toDoubleOrNull() ?: calculatedTotal

    var isDueChecked by remember { mutableStateOf(false) }
    var paidAmountText by remember { mutableStateOf("") }
    val paidAmount = if (paidAmountText.isEmpty()) (if (isDueChecked) 0.0 else totalAmount) else (paidAmountText.toDoubleOrNull() ?: 0.0)
    val dueAmount = if (isDueChecked) (totalAmount - paidAmount).coerceAtLeast(0.0) else 0.0

    var customerPhone by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var shopkeeperName by remember { mutableStateOf(shopkeeperDefaultName) }
    var note by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val sel = Calendar.getInstance().apply {
                set(year, month, day, 20, 0, 0)
            }
            dueDateMillis = sel.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "বিক্রয় সম্পন্ন করুন",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark
                            )
                        )
                        Text(
                            text = "পণ্য: ${product.name}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = RoyalBlue800,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Product Stock Info Badge
                Surface(
                    color = LightGraySurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("একক মূল্য: ${BengaliFormatters.toBanglaCurrency(product.sellPrice)}", fontSize = 12.sp, color = CharcoalMedium)
                        Text(
                            text = "মজুদ স্টক: ${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.stock <= 5) Red600 else Emerald700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 1. কতটি বিক্রি হয়েছে
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        errorMessage = null
                    },
                    label = { Text("কতটি বিক্রি হয়েছে (${product.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = RoyalBlue700) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_quantity_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. মোট বিক্রয় মূল্য
                OutlinedTextField(
                    value = if (customAmountText.isEmpty()) calculatedTotal.toString() else customAmountText,
                    onValueChange = { customAmountText = it },
                    label = { Text("মোট বিক্রয় মূল্য (টাকা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Text(" ৳ ", fontWeight = FontWeight.Bold, color = RoyalBlue700) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. বাকি আছে Checkbox
                Surface(
                    color = if (isDueChecked) Orange50 else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDueChecked) Orange500 else CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDueChecked = !isDueChecked }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDueChecked,
                            onCheckedChange = { isDueChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Orange600),
                            modifier = Modifier.testTag("checkbox_is_due")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "বাকি আছে",
                                fontWeight = FontWeight.Bold,
                                color = if (isDueChecked) Orange600 else CharcoalDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "বাকি থাকলে কাস্টমার তথ্য ও পরিশোধের তারিখ দিন",
                                fontSize = 11.sp,
                                color = CharcoalLight
                            )
                        }
                    }
                }

                // 4. Additional Fields when বাকি আছে
                AnimatedVisibility(
                    visible = isDueChecked,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text("নগদ পরিশোধের পরিমাণ (টাকা)") },
                            placeholder = { Text("যেমন: 0 বা আংশিক টাকা") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Emerald600) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            color = Orange50,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("বাকি টাকার পরিমাণ:", fontWeight = FontWeight.Bold, color = Orange600, fontSize = 13.sp)
                                Text(
                                    text = BengaliFormatters.toBanglaCurrency(dueAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = Orange600,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        OutlinedCard(
                            onClick = { datePickerDialog.show() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = RoyalBlue700)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("টাকা পরিশোধের তারিখ", fontSize = 11.sp, color = CharcoalLight)
                                        Text(
                                            text = dueDateMillis?.let { BengaliFormatters.formatDateBangla(it) } ?: "তারিখ নির্বাচন করুন",
                                            fontWeight = FontWeight.Bold,
                                            color = CharcoalDark,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = RoyalBlue700)
                            }
                        }

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Customer-এর মোবাইল নম্বর *") },
                            placeholder = { Text("017XXXXXXXX") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RoyalBlue700) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_phone_input")
                        )

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer-এর নাম (ঐচ্ছিক)") },
                            placeholder = { Text("যেমন: রহিম মিয়া") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBlue700) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = shopkeeperName,
                            onValueChange = { shopkeeperName = it },
                            label = { Text("দোকানদারের নাম") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = RoyalBlue700) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Red50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Red600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Sale Button
                Button(
                    onClick = {
                        if (quantity <= 0) {
                            errorMessage = "সঠিক বিক্রয় পরিমাণ দিন"
                            return@Button
                        }
                        if (quantity > product.stock) {
                            errorMessage = "পর্যাপ্ত স্টক নেই! বর্তমান স্টক: ${product.stock}"
                            return@Button
                        }
                        if (totalAmount <= 0) {
                            errorMessage = "সঠিক বিক্রয় মূল্য দিন"
                            return@Button
                        }
                        if (isDueChecked && customerPhone.isBlank()) {
                            errorMessage = "বাকি থাকলে কাস্টমারের মোবাইল নম্বর দিতে হবে"
                            return@Button
                        }

                        onConfirmSale(
                            quantity,
                            totalAmount,
                            paidAmount,
                            dueAmount,
                            isDueChecked,
                            customerName.trim().takeIf { it.isNotBlank() },
                            customerPhone.trim().takeIf { it.isNotBlank() },
                            shopkeeperName.trim().takeIf { it.isNotBlank() },
                            dueDateMillis,
                            note.trim().takeIf { it.isNotBlank() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_sale_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিক্রি সম্পন্ন করুন (স্টক কমবে)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// QR CODE CREATE FORM
// -------------------------------------------------------------
@Composable
fun QrCreateForm(
    viewModel: ShopViewModel,
    onQrCreated: (ProductEntity) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var sellPriceText by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("পিস") }
    var category by remember { mutableStateOf("মুদি ও খাদ্য") }
    var qrCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val units = listOf("পিস", "কেজি", "গ্রাম", "লিটার", "প্যাকেট", "বস্তা", "ডজন", "কার্টন")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RoyalBlue50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = RoyalBlue700)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "নতুন পণ্যের QR কোড তৈরি",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark
                            )
                        )
                        Text(
                            text = "তথ্য পূরণ করুন এবং অনন্য QR কোড তৈরি করুন",
                            style = MaterialTheme.typography.bodySmall.copy(color = CharcoalLight)
                        )
                    }
                }

                Divider()

                // 1. পণ্যের নাম
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        errorMessage = null
                    },
                    label = { Text("পণ্যের নাম *") },
                    placeholder = { Text("যেমন: মিনিকেট চাল / তীর তেল") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_qr_name_input")
                )

                // 2. বিক্রয় মূল্য & ক্রয় মূল্য
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it },
                        label = { Text("বিক্রয় মূল্য (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_qr_sell_price")
                    )

                    OutlinedTextField(
                        value = buyPriceText,
                        onValueChange = { buyPriceText = it },
                        label = { Text("ক্রয় মূল্য (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Stock সংখ্যা & একক
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock সংখ্যা *") },
                        placeholder = { Text("যেমন: 100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_qr_stock_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Unit Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    units.take(4).forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u, fontSize = 11.sp) }
                        )
                    }
                }

                // 4. Unique QR Code
                Column {
                    OutlinedTextField(
                        value = qrCodeInput,
                        onValueChange = {
                            qrCodeInput = it.uppercase()
                            errorMessage = null
                        },
                        label = { Text("Unique QR Code / কোড *") },
                        placeholder = { Text("যেমন: CHAL-001, OIL-101") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    val prefix = if (productName.isNotBlank()) {
                                        productName.take(3).uppercase().replace(" ", "")
                                    } else "PRD"
                                    val randomNum = (1000..9999).random()
                                    qrCodeInput = "$prefix-$randomNum"
                                }
                            ) {
                                Text("অটো তৈরি", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_qr_code_input")
                    )
                    Text(
                        text = "ক্যামেরা স্ক্যান করলে এই কোডের মাধ্যমেই পণ্য পাওয়া যাবে।",
                        fontSize = 11.sp,
                        color = CharcoalLight,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        color = Red50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Red600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Create QR Code Button
                Button(
                    onClick = {
                        if (productName.isBlank()) {
                            errorMessage = "পণ্যের নাম দিন"
                            return@Button
                        }
                        val sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0
                        if (sellPrice <= 0.0) {
                            errorMessage = "সঠিক বিক্রয় মূল্য দিন"
                            return@Button
                        }
                        val stock = stockText.toIntOrNull() ?: -1
                        if (stock < 0) {
                            errorMessage = "সঠিক Stock সংখ্যা দিন"
                            return@Button
                        }
                        val code = qrCodeInput.trim()
                        if (code.isBlank()) {
                            errorMessage = "QR কোডের জন্য একটি Unique Code দিন"
                            return@Button
                        }

                        val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0

                        viewModel.addProduct(
                            name = productName,
                            buyPrice = buyPrice,
                            sellPrice = sellPrice,
                            stock = stock,
                            unit = unit,
                            category = category,
                            qrCode = code,
                            onSuccess = { createdProduct ->
                                onQrCreated(createdProduct)
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_create_qr_btn")
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QR Code তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// QR CODE HISTORY VIEW
// -------------------------------------------------------------
@Composable
fun QrHistoryView(
    products: List<ProductEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectProductForDetails: (ProductEntity) -> Unit,
    onSelectProductForSale: (ProductEntity) -> Unit,
    onViewQr: (ProductEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("QR Code বা পণ্যের নাম দিয়ে খুঁজুন...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalBlue700) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "ক্লিয়ার")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("qr_history_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (products.isEmpty()) {
            BengaliEmptyState(
                icon = Icons.Default.QrCode2,
                title = "কোনো QR Code পাওয়া যায়নি",
                description = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে মিল রেখে কোনো QR পণ্য নেই" else "এখনও কোনো পণ্যের QR Code তৈরি করা হয়নি"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    QrProductHistoryCard(
                        product = product,
                        onClick = { onSelectProductForDetails(product) },
                        onSell = { onSelectProductForSale(product) },
                        onViewQr = { onViewQr(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun QrProductHistoryCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onSell: () -> Unit,
    onViewQr: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RoyalBlue50)
                    .clickable { onViewQr() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = RoyalBlue800,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "QR: ${product.qrCode ?: "নাই"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RoyalBlue800,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "মূল্য: ${BengaliFormatters.toBanglaCurrency(product.sellPrice)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalMedium
                        )
                    )

                    Text(
                        text = "স্টক: ${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (product.stock <= 5) Red600 else Emerald700
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSell,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Emerald600),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = "বিক্রি করুন",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// QR PREVIEW & SHARE DIALOG
// -------------------------------------------------------------
@Composable
fun QrPreviewDialog(
    product: ProductEntity,
    shopName: String,
    onDismiss: () -> Unit,
    onSellNow: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QR Code প্রিভিউ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Image
                QrPreviewImage(
                    qrCodeText = product.qrCode ?: product.name,
                    sizeDp = 180.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Product Details
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark,
                        textAlign = TextAlign.Center
                    )
                )

                Surface(
                    color = RoyalBlue50,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Unique Code: ${product.qrCode}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue800
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("বিক্রয় মূল্য", fontSize = 12.sp, color = CharcoalLight)
                        Text(
                            text = BengaliFormatters.toBanglaCurrency(product.sellPrice),
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue800,
                            fontSize = 16.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("বর্তমান Stock", fontSize = 12.sp, color = CharcoalLight)
                        Text(
                            text = "${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit}",
                            fontWeight = FontWeight.Bold,
                            color = Emerald700,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val textToShare = "$shopName\nপণ্য: ${product.name}\nমূল্য: ৳${product.sellPrice}\nQR কোড: ${product.qrCode}\nস্টক: ${product.stock} ${product.unit}"
                            ReminderUtils.shareText(context, "পণ্য QR কোড: ${product.name}", textToShare)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("শেয়ার", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSellNow,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বিক্রি করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
