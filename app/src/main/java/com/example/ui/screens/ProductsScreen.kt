package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.DokanTopBar
import com.example.ui.components.QrPreviewImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ShopViewModel,
    onOpenQrScanForProduct: (ProductEntity) -> Unit = {}
) {
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()

    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var viewingQrProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productForRestock by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = "দোকানের পণ্যসমূহ",
                subtitle = "মোট ${BengaliFormatters.toBanglaNumber(products.size)} টি পণ্য তালিকাভুক্ত",
                dueAlertCount = dueReminders.size
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("পণ্য যোগ করুন", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("fab_add_product")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar: "পণ্যের নাম বা কোড দিয়ে খুঁজুন"
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.productSearchQuery.value = it },
                placeholder = { Text("পণ্যের নাম বা কোড দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalBlue700) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.productSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "ক্লিয়ার")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (products.isEmpty()) {
                BengaliEmptyState(
                    icon = Icons.Default.Inventory,
                    title = "এখনও কোনো পণ্য যোগ করা হয়নি",
                    description = "দোকানের হিসাব ও QR স্ক্যান চালু করতে প্রথম পণ্য যোগ করুন",
                    buttonText = "পণ্য যোগ করুন",
                    onButtonClick = { showAddProductDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = { editingProduct = product },
                            onRestock = { productForRestock = product },
                            onViewQr = { viewingQrProduct = product },
                            onSell = { onOpenQrScanForProduct(product) }
                        )
                    }
                }
            }
        }
    }

    // Add Product Modal Flow
    if (showAddProductDialog) {
        AddProductFlowDialog(
            viewModel = viewModel,
            onDismiss = { showAddProductDialog = false },
            onProductAdded = {
                showAddProductDialog = false
            }
        )
    }

    // Edit Product Modal
    editingProduct?.let { product ->
        EditProductDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { editingProduct = null }
        )
    }

    // Restock Modal (মাল ক্রয় / স্টক বৃদ্ধি e.g. add 300)
    productForRestock?.let { product ->
        RestockDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { productForRestock = null }
        )
    }

    // View QR Dialog
    viewingQrProduct?.let { product ->
        if (!product.qrCode.isNullOrBlank()) {
            QrPreviewDialog(
                product = product,
                shopProfile = shopProfile,
                onDismiss = { viewingQrProduct = null },
                onSellNow = {
                    viewingQrProduct = null
                    onOpenQrScanForProduct(product)
                }
            )
        }
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onViewQr: () -> Unit,
    onSell: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = LightGraySurface,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CharcoalMedium,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!product.qrCode.isNullOrBlank()) {
                            Surface(
                                color = RoyalBlue50,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { onViewQr() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = RoyalBlue800,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = product.qrCode,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RoyalBlue800,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "QR কোড নেই",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFB45309),
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "সম্পাদনা",
                        tint = CharcoalLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider()

            Spacer(modifier = Modifier.height(10.dp))

            // Stock & Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("বিক্রয় মূল্য", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = BengaliFormatters.toBanglaCurrency(product.sellPrice),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue800
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ক্রয় মূল্য", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = if (product.buyPrice > 0) BengaliFormatters.toBanglaCurrency(product.buyPrice) else "—",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CharcoalMedium,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("বর্তমান স্টক", fontSize = 11.sp, color = CharcoalLight)
                    Text(
                        text = "${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (product.stock <= 5) Red600 else Emerald700
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row (Sell, Restock, QR Preview)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRestock,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalBlue700)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ স্টক বৃদ্ধি", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBlue800)
                }

                Button(
                    onClick = onSell,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("বিক্রি করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD PRODUCT FLOW DIALOG (Step 7, 8, 9 of requirements)
// -------------------------------------------------------------
@Composable
fun AddProductFlowDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit,
    onProductAdded: (ProductEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Form, 2: QR Prompt Dialog, 3: Unique Code Input

    var name by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("") }
    var sellPriceText by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("পিস") }
    var category by remember { mutableStateOf("সাধারণ") }
    var qrCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    when (step) {
        1 -> {
            // Step 1: Base Product Form (Step 7)
            Dialog(onDismissRequest = onDismiss) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "নতুন পণ্য যোগ করুন",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalDark,
                                    fontSize = 18.sp
                                )
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "বন্ধ")
                            }
                        }

                        Divider()

                        // পণ্যের নাম
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorMessage = null
                            },
                            label = { Text("পণ্যের নাম *") },
                            placeholder = { Text("যেমন: চাল, ডাল, তেল") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_product_name_input")
                        )

                        // Stock
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("Stock সংখ্যা *") },
                            placeholder = { Text("যেমন: 100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_product_stock_input")
                        )

                        // পণ্যের দাম (বিক্রয় মূল্য)
                        OutlinedTextField(
                            value = sellPriceText,
                            onValueChange = { sellPriceText = it },
                            label = { Text("পণ্যের বিক্রয় মূল্য (৳) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_product_sell_price_input")
                        )

                        // ক্রয় মূল্য
                        OutlinedTextField(
                            value = buyPriceText,
                            onValueChange = { buyPriceText = it },
                            label = { Text("ক্রয় মূল্য (৳)") },
                            placeholder = { Text("ঐচ্ছিক") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // একক (Unit)
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("একক (যেমন: কেজি, পিস, লিটার)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

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

                        // "পণ্য যোগ করুন" Button -> Leads to Step 2 Prompt!
                        Button(
                            onClick = {
                                if (name.isBlank()) {
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

                                // Proceed to QR Prompt Dialog!
                                step = 2
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_proceed_add_product")
                        ) {
                            Text("পণ্য যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        2 -> {
            // Step 2 Dialog: "আপনি কি এই পণ্যের জন্য QR Code তৈরি করতে চান?" (Step 7/8)
            AlertDialog(
                onDismissRequest = { step = 1 },
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = RoyalBlue800,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = "QR Code তৈরি করবেন?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CharcoalDark,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "আপনি কি এই পণ্যের জন্য QR Code তৈরি করতে চান? QR কোড তৈরি করলে পরবর্তীতে সরাসরি ক্যামেরা দিয়ে স্ক্যান করে বিক্রি করতে পারবেন।",
                        color = CharcoalMedium,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Yes, want QR -> Move to step 3 (Enter unique code)
                            step = 3
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_yes_create_qr")
                    ) {
                        Text("হ্যাঁ, QR Code তৈরি করুন", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            // Step 8: "না, QR Code ছাড়াই যোগ করুন"
                            val sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0
                            val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0
                            val stock = stockText.toIntOrNull() ?: 0

                            viewModel.addProduct(
                                name = name,
                                buyPrice = buyPrice,
                                sellPrice = sellPrice,
                                stock = stock,
                                unit = unit,
                                category = category,
                                qrCode = null,
                                onSuccess = { created ->
                                    onProductAdded(created)
                                }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_no_qr_add")
                    ) {
                        Text("না, QR Code ছাড়াই যোগ করুন", color = CharcoalDark)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }

        3 -> {
            // Step 3 Dialog: "QR Code-এর শিরোনাম বা একটি Unique Code দিন" (Step 9)
            Dialog(onDismissRequest = { step = 2 }) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "QR Code-এর Unique Code দিন",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark
                            )
                        )

                        Text(
                            text = "উদাহরণ: CHAL-001, RICE-2026-001",
                            style = MaterialTheme.typography.bodySmall.copy(color = CharcoalLight)
                        )

                        OutlinedTextField(
                            value = qrCodeInput,
                            onValueChange = {
                                qrCodeInput = it.uppercase()
                                errorMessage = null
                            },
                            label = { Text("Unique Code *") },
                            placeholder = { Text("CHAL-001") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        val prefix = name.take(3).uppercase().replace(" ", "").ifBlank { "PRD" }
                                        val randomNum = (1000..9999).random()
                                        qrCodeInput = "$prefix-$randomNum"
                                    }
                                ) {
                                    Text("অটো তৈরি", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_unique_qr_code")
                        )

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

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("পেছনে")
                            }

                            Button(
                                onClick = {
                                    val code = qrCodeInput.trim()
                                    if (code.isBlank()) {
                                        errorMessage = "একটি Unique Code দিন"
                                        return@Button
                                    }

                                    val sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0
                                    val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0
                                    val stock = stockText.toIntOrNull() ?: 0

                                    viewModel.addProduct(
                                        name = name,
                                        buyPrice = buyPrice,
                                        sellPrice = sellPrice,
                                        stock = stock,
                                        unit = unit,
                                        category = category,
                                        qrCode = code,
                                        onSuccess = { created ->
                                            onProductAdded(created)
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("btn_confirm_add_with_qr")
                            ) {
                                Text("যুক্ত ও QR তৈরি", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// EDIT PRODUCT DIALOG
// -------------------------------------------------------------
@Composable
fun EditProductDialog(
    product: ProductEntity,
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var sellPriceText by remember { mutableStateOf(product.sellPrice.toString()) }
    var buyPriceText by remember { mutableStateOf(product.buyPrice.toString()) }
    var stockText by remember { mutableStateOf(product.stock.toString()) }
    var qrCode by remember { mutableStateOf(product.qrCode ?: "") }
    var unit by remember { mutableStateOf(product.unit) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পণ্য সম্পাদনা",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark,
                            fontSize = 18.sp
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ")
                    }
                }

                Divider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("পণ্যের নাম") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it },
                        label = { Text("বিক্রয় মূল্য (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock (${product.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = qrCode,
                    onValueChange = { qrCode = it.uppercase() },
                    label = { Text("Unique QR Code") },
                    placeholder = { Text("যেমন: CHAL-001") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মুছুন")
                    }

                    Button(
                        onClick = {
                            val sell = sellPriceText.toDoubleOrNull() ?: product.sellPrice
                            val buy = buyPriceText.toDoubleOrNull() ?: product.buyPrice
                            val stock = stockText.toIntOrNull() ?: product.stock
                            val updated = product.copy(
                                name = name.trim(),
                                sellPrice = sell,
                                buyPrice = buy,
                                stock = stock,
                                unit = unit,
                                qrCode = qrCode.trim().takeIf { it.isNotBlank() }
                            )
                            viewModel.updateProduct(updated) {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("পণ্যটি মুছে ফেলতে চান?") },
            text = { Text("এই পণ্য মুছে ফেললে এর সমস্ত তথ্য মুছে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("হ্যাঁ, মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
