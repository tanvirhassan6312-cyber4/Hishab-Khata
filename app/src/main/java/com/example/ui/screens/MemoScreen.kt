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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MemoEntity
import com.example.data.local.MemoItem
import com.example.data.local.ProductEntity
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.DokanTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BengaliFormatters
import com.example.util.MemoUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoScreen(
    viewModel: ShopViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val allMemos by viewModel.filteredMemos.collectAsStateWithLifecycle()
    val memoSearchQuery by viewModel.memoSearchQuery.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val dueReminders by viewModel.pendingDueReminders.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: নতুন মেমো তৈরি (Create), 1: মেমো তালিকা ও ইতিহাস (History)
    var selectedMemoForPreview by remember { mutableStateOf<MemoEntity?>(null) }

    Scaffold(
        topBar = {
            DokanTopBar(
                title = "মেমো ব্যবস্থাপনা",
                subtitle = "দোকানদার, ব্যবসায়ী ও ম্যানেজারদের ডিজিটাল মেমো",
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
            // Segmented Switch / SubTabs
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
                    MemoTabButton(
                        title = "① নতুন মেমো তৈরি",
                        icon = Icons.Default.PostAdd,
                        isSelected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        modifier = Modifier.weight(1f).testTag("tab_memo_create")
                    )

                    MemoTabButton(
                        title = "② মেমোর ইতিহাস (${BengaliFormatters.toBanglaNumber(allMemos.size)})",
                        icon = Icons.Default.ReceiptLong,
                        isSelected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        modifier = Modifier.weight(1f).testTag("tab_memo_history")
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeSubTab) {
                    0 -> {
                        MemoCreateView(
                            viewModel = viewModel,
                            shopName = shopProfile?.shopName ?: "আমার ব্যবসা",
                            shopPhone = shopProfile?.phone ?: "",
                            shopAddress = shopProfile?.address ?: "",
                            preparedBy = shopProfile?.ownerName ?: "",
                            availableProducts = allProducts,
                            onMemoSaved = { savedMemo ->
                                selectedMemoForPreview = savedMemo
                            }
                        )
                    }
                    1 -> {
                        MemoHistoryView(
                            memos = allMemos,
                            searchQuery = memoSearchQuery,
                            onSearchChange = { viewModel.memoSearchQuery.value = it },
                            onSelectMemo = { selectedMemoForPreview = it },
                            onDeleteMemo = { viewModel.deleteMemo(it) }
                        )
                    }
                }
            }
        }
    }

    // Memo Preview & Print/Share Dialog
    selectedMemoForPreview?.let { memo ->
        MemoPreviewDialog(
            memo = memo,
            onDismiss = { selectedMemoForPreview = null },
            onShareWhatsApp = {
                val items = MemoUtils.deserializeItems(memo.itemsJson)
                MemoUtils.shareViaWhatsApp(context, memo, items)
            },
            onShareText = {
                val items = MemoUtils.deserializeItems(memo.itemsJson)
                MemoUtils.shareAsText(context, memo, items)
            }
        )
    }
}

@Composable
private fun MemoTabButton(
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
// MEMO CREATE VIEW (Supports Retail, Business, Office Voucher, etc.)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoCreateView(
    viewModel: ShopViewModel,
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    preparedBy: String,
    availableProducts: List<ProductEntity>,
    onMemoSaved: (MemoEntity) -> Unit
) {
    val context = LocalContext.current

    // Memo Types
    val memoTypes = listOf(
        "বিক্রয় মেমো / ক্যাশ মেমো",
        "ব্যবসায়ী / হোলসেল চালান",
        "ম্যানেজার / অফিস ভাউচার",
        "ক্রয় মেমো",
        "বকেয়া মেমো"
    )
    var selectedMemoType by remember { mutableStateOf(memoTypes[0]) }

    var currentShopName by remember(shopName) { mutableStateOf(shopName) }
    var currentShopPhone by remember(shopPhone) { mutableStateOf(shopPhone) }
    var currentShopAddress by remember(shopAddress) { mutableStateOf(shopAddress) }
    var currentPreparedBy by remember(preparedBy) { mutableStateOf(preparedBy) }

    var memoNumber by remember { mutableStateOf(MemoUtils.generateMemoNumber()) }
    var memoDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }

    // Dynamic Items in Memo
    val itemsList = remember {
        mutableStateListOf(
            MemoItem(sl = 1, itemName = "", quantity = 1.0, unit = "পিস", unitPrice = 0.0, total = 0.0)
        )
    }

    var discountText by remember { mutableStateOf("0") }
    var vatPercentText by remember { mutableStateOf("0") }
    var paidAmountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("নগদ (Cash)") }
    var memoNotes by remember { mutableStateOf("বিক্রিত মাল ফেরত নেওয়া হয় না। ধন্যবাদ!") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Calculated amounts
    val subtotal = itemsList.sumOf { it.total }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val vatPercent = vatPercentText.toDoubleOrNull() ?: 0.0
    val vatAmount = ((subtotal - discount).coerceAtLeast(0.0) * vatPercent) / 100.0
    val grandTotal = (subtotal - discount + vatAmount).coerceAtLeast(0.0)

    val paidAmount = if (paidAmountText.isBlank()) grandTotal else (paidAmountText.toDoubleOrNull() ?: grandTotal)
    val dueAmount = (grandTotal - paidAmount).coerceAtLeast(0.0)

    val paymentMethods = listOf("নগদ (Cash)", "বিকাশ (bKash)", "নগদ (Nagad)", "ব্যাংক (Bank)", "বাকি (Due)", "কার্ড (Card)")

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val sel = Calendar.getInstance().apply {
                set(year, month, day, 12, 0, 0)
            }
            memoDateMillis = sel.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Template / Memo Type Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "মেমোর ধরণ নির্বাচন করুন (দোকানদার / ব্যবসায়ী / ম্যানেজার):",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue800
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    memoTypes.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedMemoType == type,
                            onClick = { selectedMemoType = type },
                            label = { Text(type.split("/")[0].trim(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    memoTypes.drop(3).forEach { type ->
                        FilterChip(
                            selected = selectedMemoType == type,
                            onClick = { selectedMemoType = type },
                            label = { Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        // 2. Shop & Meta Header Info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "দোকান / প্রতিষ্ঠানের তথ্য",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                    )
                    OutlinedButton(
                        onClick = { memoNumber = MemoUtils.generateMemoNumber() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(memoNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = currentShopName,
                    onValueChange = { currentShopName = it },
                    label = { Text("দোকান / কোম্পানির নাম") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("memo_shop_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentShopPhone,
                        onValueChange = { currentShopPhone = it },
                        label = { Text("মোবাইল নম্বর") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = currentPreparedBy,
                        onValueChange = { currentPreparedBy = it },
                        label = { Text("ম্যানেজার / ক্যাশিয়ার") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = currentShopAddress,
                    onValueChange = { currentShopAddress = it },
                    label = { Text("প্রতিষ্ঠানের ঠিকানা") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Picker row
                OutlinedCard(
                    onClick = { datePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = RoyalBlue700)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("মেমোর তারিখ: ${BengaliFormatters.formatDateBangla(memoDateMillis)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.EditCalendar, contentDescription = null, tint = RoyalBlue700)
                    }
                }
            }
        }

        // 3. Customer / Client Details
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ক্রেতা / প্রাপকের বিবরণ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                )

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it; errorMessage = null },
                    label = { Text("ক্রেতা বা পার্টির নাম *") },
                    placeholder = { Text("যেমন: জনাব আবুল কালাম / মেসার্স ট্রেডার্স") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBlue700) },
                    modifier = Modifier.fillMaxWidth().testTag("memo_customer_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("মোবাইল নম্বর") },
                        placeholder = { Text("017XXXXXXXX") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RoyalBlue700) },
                        modifier = Modifier.weight(1f).testTag("memo_customer_phone_input")
                    )

                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("ঠিকানা / এলাকা") },
                        placeholder = { Text("যেমন: ঢাকা") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Products / Items Table in Memo
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পণ্যের বিবরণ ও মূল্য তালিকা",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                    )
                    Button(
                        onClick = {
                            itemsList.add(
                                MemoItem(
                                    sl = itemsList.size + 1,
                                    itemName = "",
                                    quantity = 1.0,
                                    unit = "পিস",
                                    unitPrice = 0.0,
                                    total = 0.0
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue700),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_memo_item")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পণ্য যোগ (+)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // If store has products, allow 1-click add from inventory
                if (availableProducts.isNotEmpty()) {
                    Text(
                        text = "দোকানের স্টক থেকে দ্রুত যোগ করুন:",
                        fontSize = 11.sp,
                        color = CharcoalLight,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableProducts.take(4).forEach { p ->
                            AssistChip(
                                onClick = {
                                    itemsList.add(
                                        MemoItem(
                                            sl = itemsList.size + 1,
                                            itemName = p.name,
                                            quantity = 1.0,
                                            unit = p.unit,
                                            unitPrice = p.sellPrice,
                                            total = p.sellPrice
                                        )
                                    )
                                },
                                label = { Text("${p.name} (${BengaliFormatters.toBanglaCurrency(p.sellPrice)})", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Divider()

                itemsList.forEachIndexed { index, item ->
                    var itemName by remember(item.itemName) { mutableStateOf(item.itemName) }
                    var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }
                    var unitText by remember(item.unit) { mutableStateOf(item.unit) }
                    var rateText by remember(item.unitPrice) { mutableStateOf(if (item.unitPrice == 0.0) "" else item.unitPrice.toString()) }

                    Surface(
                        color = LightGraySurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "আইটেম #${BengaliFormatters.toBanglaNumber(index + 1)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RoyalBlue800
                                )

                                if (itemsList.size > 1) {
                                    IconButton(
                                        onClick = { itemsList.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = Red600, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = itemName,
                                onValueChange = {
                                    itemName = it
                                    itemsList[index] = itemsList[index].copy(itemName = it)
                                },
                                label = { Text("পণ্যের নাম বা বিবরণ") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = qtyText,
                                    onValueChange = {
                                        qtyText = it
                                        val q = it.toDoubleOrNull() ?: 0.0
                                        val r = rateText.toDoubleOrNull() ?: 0.0
                                        itemsList[index] = itemsList[index].copy(quantity = q, total = q * r)
                                    },
                                    label = { Text("পরিমাণ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = unitText,
                                    onValueChange = {
                                        unitText = it
                                        itemsList[index] = itemsList[index].copy(unit = it)
                                    },
                                    label = { Text("একক") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(0.9f)
                                )

                                OutlinedTextField(
                                    value = rateText,
                                    onValueChange = {
                                        rateText = it
                                        val r = it.toDoubleOrNull() ?: 0.0
                                        val q = qtyText.toDoubleOrNull() ?: 0.0
                                        itemsList[index] = itemsList[index].copy(unitPrice = r, total = q * r)
                                    },
                                    label = { Text("দর (৳)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("মোট: ", fontSize = 12.sp, color = CharcoalLight)
                                Text(
                                    text = BengaliFormatters.toBanglaCurrency(item.total),
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Total Calculation, Discount, VAT, & Payment
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "হিসাব ও পেমেন্ট বিবরণ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("উপমোট (Subtotal):", fontSize = 13.sp, color = CharcoalMedium)
                    Text(BengaliFormatters.toBanglaCurrency(subtotal), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("ছাড় / ডিসকাউন্ট (টাকা)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = vatPercentText,
                        onValueChange = { vatPercentText = it },
                        label = { Text("ভ্যাট / ট্যাক্স (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider()

                // Grand Total Banner
                Surface(
                    color = RoyalBlue50,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("সর্বমোট বিল (Grand Total):", fontWeight = FontWeight.Bold, color = RoyalBlue800, fontSize = 15.sp)
                        Text(
                            text = BengaliFormatters.toBanglaCurrency(grandTotal),
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBlue800,
                            fontSize = 18.sp
                        )
                    }
                }

                // In words
                Text(
                    text = "কথায়: ${MemoUtils.numberToBanglaWords(grandTotal)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RoyalBlue700,
                        fontWeight = FontWeight.Medium
                    )
                )

                // Paid & Due
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("জমা / পরিশোধিত (৳)") },
                        placeholder = { Text(grandTotal.toString()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("memo_paid_amount_input")
                    )

                    // Payment Method Dropdown
                    var expandedMethod by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedMethod,
                        onExpandedChange = { expandedMethod = !expandedMethod },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("পদ্ধতি") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMethod) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMethod,
                            onDismissRequest = { expandedMethod = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        expandedMethod = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Due Banner if any
                if (dueAmount > 0) {
                    Surface(
                        color = Orange50,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Orange500),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("বাকি টাকা (Due Amount):", fontWeight = FontWeight.Bold, color = Orange600, fontSize = 13.sp)
                            Text(BengaliFormatters.toBanglaCurrency(dueAmount), fontWeight = FontWeight.Bold, color = Orange600, fontSize = 15.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = memoNotes,
                    onValueChange = { memoNotes = it },
                    label = { Text("শর্তাবলী / নোট") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Save & Print Action Buttons
        Button(
            onClick = {
                if (customerName.isBlank()) {
                    errorMessage = "ক্রেতা বা প্রাপকের নাম লিখুন"
                    return@Button
                }
                val validItems = itemsList.filter { it.itemName.isNotBlank() && it.quantity > 0 }
                if (validItems.isEmpty()) {
                    errorMessage = "মেমোতে অন্তত একটি পণ্যের নাম ও পরিমাণ লিখুন"
                    return@Button
                }

                val memo = MemoEntity(
                    memoNumber = memoNumber,
                    memoType = selectedMemoType,
                    date = memoDateMillis,
                    shopName = currentShopName.ifBlank { "আমার ব্যবসা" },
                    preparedBy = currentPreparedBy,
                    shopPhone = currentShopPhone,
                    shopAddress = currentShopAddress,
                    customerName = customerName.trim(),
                    customerPhone = customerPhone.trim().takeIf { it.isNotBlank() },
                    customerAddress = customerAddress.trim().takeIf { it.isNotBlank() },
                    itemsJson = MemoUtils.serializeItems(validItems),
                    subtotal = subtotal,
                    discountAmount = discount,
                    vatPercent = vatPercent,
                    vatAmount = vatAmount,
                    grandTotal = grandTotal,
                    paidAmount = paidAmount,
                    dueAmount = dueAmount,
                    paymentMethod = paymentMethod,
                    notes = memoNotes,
                    terms = "বিক্রিত মাল ফেরত নেওয়া হয় না।",
                    createdAt = System.currentTimeMillis()
                )

                viewModel.saveMemo(
                    memo = memo,
                    items = validItems,
                    deductFromInventory = true,
                    onSuccess = { saved ->
                        onMemoSaved(saved)
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_save_memo")
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("মেমো তৈরি ও সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// -------------------------------------------------------------
// MEMO HISTORY VIEW (Search & List)
// -------------------------------------------------------------
@Composable
fun MemoHistoryView(
    memos: List<MemoEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectMemo: (MemoEntity) -> Unit,
    onDeleteMemo: (MemoEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("মেমো নং, কাস্টমার নাম বা ফোন দিয়ে খুঁজুন...") },
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
            modifier = Modifier.fillMaxWidth().testTag("memo_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (memos.isEmpty()) {
            BengaliEmptyState(
                icon = Icons.Default.ReceiptLong,
                title = "কোনো মেমো পাওয়া যায়নি",
                description = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে মিল রেখে কোনো মেমো পাওয়া যায়নি" else "এখনও কোনো মেমো তৈরি করা হয়নি। 'নতুন মেমো তৈরি' থেকে মেমো বানান।"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(memos, key = { it.id }) { memo ->
                    MemoHistoryCard(
                        memo = memo,
                        onView = { onSelectMemo(memo) },
                        onDelete = { onDeleteMemo(memo) }
                    )
                }
            }
        }
    }
}

@Composable
fun MemoHistoryCard(
    memo: MemoEntity,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val items = remember(memo.itemsJson) { MemoUtils.deserializeItems(memo.itemsJson) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { onView() }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = RoyalBlue50,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = memo.memoNumber,
                        color = RoyalBlue800,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (memo.dueAmount > 0) Orange50 else Emerald50,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (memo.dueAmount > 0) "বাকি আছে" else "পরিশোধিত",
                        color = if (memo.dueAmount > 0) Orange600 else Emerald700,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = memo.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                    )
                    Text(
                        text = "${BengaliFormatters.formatDateBangla(memo.date)} • ${items.size} টি আইটেম",
                        fontSize = 12.sp,
                        color = CharcoalLight
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = BengaliFormatters.toBanglaCurrency(memo.grandTotal),
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue800,
                        fontSize = 16.sp
                    )
                    if (memo.dueAmount > 0) {
                        Text(
                            text = "বাকি: ${BengaliFormatters.toBanglaCurrency(memo.dueAmount)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Orange600
                        )
                    }
                }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memo.memoType,
                    fontSize = 11.sp,
                    color = CharcoalLight,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = Red600, modifier = Modifier.size(18.dp))
                    }

                    Button(
                        onClick = onView,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মেমো দেখুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("মেমোটি মুছে ফেলতে চান?") },
            text = { Text("মেমো নং: ${memo.memoNumber} মুছে ফেললে আর পুনরুদ্ধার করা যাবে না।") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
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

// -------------------------------------------------------------
// MEMO PREVIEW / RECEIPT DIALOG (Authentic printed invoice style)
// -------------------------------------------------------------
@Composable
fun MemoPreviewDialog(
    memo: MemoEntity,
    onDismiss: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareText: () -> Unit
) {
    val items = remember(memo.itemsJson) { MemoUtils.deserializeItems(memo.itemsJson) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Top Header Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = RoyalBlue50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "মেমো নং: ${memo.memoNumber}",
                            color = RoyalBlue800,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ", tint = CharcoalDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Printable Memo Slip Container (Paper Look)
                Surface(
                    color = Color(0xFFFAFBFD),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Shop Title
                        Text(
                            text = memo.shopName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = RoyalBlue900,
                            textAlign = TextAlign.Center
                        )
                        if (memo.shopAddress.isNotBlank()) {
                            Text(
                                text = memo.shopAddress,
                                fontSize = 11.sp,
                                color = CharcoalLight,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (memo.shopPhone.isNotBlank()) {
                            Text(
                                text = "মোবাইল: ${memo.shopPhone}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = CharcoalMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = RoyalBlue700,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = memo.memoType.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer & Date Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("প্রাপক / কাস্টমার:", fontSize = 10.sp, color = CharcoalLight)
                                Text(memo.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                                if (!memo.customerPhone.isNullOrBlank()) {
                                    Text("ফোন: ${memo.customerPhone}", fontSize = 11.sp, color = CharcoalMedium)
                                }
                                if (!memo.customerAddress.isNullOrBlank()) {
                                    Text("ঠিকানা: ${memo.customerAddress}", fontSize = 11.sp, color = CharcoalMedium)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("তারিখ:", fontSize = 10.sp, color = CharcoalLight)
                                Text(BengaliFormatters.formatDateBangla(memo.date), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalDark)
                                if (memo.preparedBy.isNotBlank()) {
                                    Text("প্রস্তুতকারক: ${memo.preparedBy}", fontSize = 10.sp, color = CharcoalLight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Item Table Header
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("বিবরণ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CharcoalDark, modifier = Modifier.weight(1.5f))
                                Text("পরিমাণ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CharcoalDark, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("দর", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CharcoalDark, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                                Text("মোট", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CharcoalDark, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                        }

                        // Items
                        items.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}. ${item.itemName}", fontSize = 11.sp, color = CharcoalDark, modifier = Modifier.weight(1.5f))
                                Text("${BengaliFormatters.toBanglaNumber(item.quantity)} ${item.unit}", fontSize = 11.sp, color = CharcoalMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(BengaliFormatters.toBanglaCurrency(item.unitPrice), fontSize = 11.sp, color = CharcoalMedium, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                                Text(BengaliFormatters.toBanglaCurrency(item.total), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CharcoalDark, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            Divider(color = Color(0xFFF1F5F9))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calculations
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("উপমোট:", fontSize = 11.sp, color = CharcoalLight)
                                Text(BengaliFormatters.toBanglaCurrency(memo.subtotal), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            if (memo.discountAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ছাড় / ডিসকাউন্ট:", fontSize = 11.sp, color = Red600)
                                    Text("-${BengaliFormatters.toBanglaCurrency(memo.discountAmount)}", fontSize = 11.sp, color = Red600, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (memo.vatAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ভ্যাট (${BengaliFormatters.toBanglaNumber(memo.vatPercent)}%):", fontSize = 11.sp, color = CharcoalMedium)
                                    Text("+${BengaliFormatters.toBanglaCurrency(memo.vatAmount)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Divider(color = Color(0xFFCBD5E1))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("সর্বমোট বিল:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue900)
                                Text(BengaliFormatters.toBanglaCurrency(memo.grandTotal), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = RoyalBlue900)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("পরিশোধিত (${memo.paymentMethod}):", fontSize = 11.sp, color = Emerald700)
                                Text(BengaliFormatters.toBanglaCurrency(memo.paidAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                            }
                            if (memo.dueAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("অবশিষ্ট বাকি:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Orange600)
                                    Text(BengaliFormatters.toBanglaCurrency(memo.dueAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Orange600)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "কথায়: ${MemoUtils.numberToBanglaWords(memo.grandTotal)}",
                            fontSize = 11.sp,
                            color = RoyalBlue700,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Signatures
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Divider(modifier = Modifier.width(70.dp), color = Color.Gray)
                                Text("ক্রেতার স্বাক্ষর", fontSize = 9.sp, color = CharcoalLight)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Divider(modifier = Modifier.width(70.dp), color = Color.Gray)
                                Text("অনুমোদিত স্বাক্ষর", fontSize = 9.sp, color = CharcoalLight)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WhatsApp & Share Action Buttons (বিনা মূল্যে WhatsApp শেয়ার)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShareWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_share_memo_whatsapp")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp মেমো", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onShareText,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.9f).testTag("btn_share_memo_text")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মেমো শেয়ার", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
