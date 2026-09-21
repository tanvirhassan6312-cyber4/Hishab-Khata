package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CommunityCreditRecordEntity
import com.example.data.local.MemoEntity
import com.example.data.local.MemoItem
import com.example.data.local.ProductEntity
import com.example.data.local.SaleTransactionEntity
import com.example.data.local.ShopProfileEntity
import com.example.data.local.ShopRepository
import com.example.util.BengaliFormatters
import com.example.util.CustomerTrustScoreEngine
import com.example.util.MemoUtils
import com.example.util.ParsedVoiceEntry
import com.example.util.TrustScoreResult
import com.example.util.VoiceTransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DailySummary(
    val dateMillis: Long,
    val totalSales: Double,
    val totalCashReceived: Double,
    val totalDue: Double,
    val totalItemsSold: Int,
    val totalTransactionsCount: Int,
    val dueCustomersCount: Int,
    val transactions: List<SaleTransactionEntity>
)

sealed class UiEvent {
    data class ShowToast(val message: String, val isError: Boolean = false) : UiEvent()
    data class ProductAdded(val product: ProductEntity) : UiEvent()
    data class SaleCompleted(val transaction: SaleTransactionEntity) : UiEvent()
    data class QrCreated(val product: ProductEntity) : UiEvent()
    data class MemoSaved(val memo: MemoEntity) : UiEvent()
    data class PaymentPaid(val customerName: String, val amount: Double) : UiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShopRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ShopRepository(db.productDao(), db.saleDao(), db.shopProfileDao(), db.memoDao(), db.communityCreditDao())
        viewModelScope.launch {
            repository.getShopProfileOnce()
        }
    }

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // Navigation & Tab state (0: Home, 1: Memo, 2: QR Hub, 3: Products/Stock, 4: Ledger, 5: Settings)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    // Shop Profile
    val shopProfile: StateFlow<ShopProfileEntity?> = repository.shopProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Products
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productsWithQr: StateFlow<List<ProductEntity>> = repository.productsWithQr
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .map { list -> list.filter { it.stock <= 5 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStock: StateFlow<Int> = repository.totalStock
        .combine(MutableStateFlow(0)) { stock, _ -> stock ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalProductsCount: StateFlow<Int> = repository.totalProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Search queries
    val productSearchQuery = MutableStateFlow("")
    val qrSearchQuery = MutableStateFlow("")
    val historySearchQuery = MutableStateFlow("")
    val memoSearchQuery = MutableStateFlow("")

    val filteredProducts: StateFlow<List<ProductEntity>> = productSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allProducts else repository.searchProducts(query.trim())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredQrProducts: StateFlow<List<ProductEntity>> = qrSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.productsWithQr else repository.searchProducts(query.trim())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<SaleTransactionEntity>> = historySearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allTransactions else repository.searchTransactions(query.trim())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions
    val allTransactions: StateFlow<List<SaleTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDueTransactions: StateFlow<List<SaleTransactionEntity>> = repository.allDueTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDueTotal: StateFlow<Double> = repository.allPendingDueTotal
        .combine(MutableStateFlow(0.0)) { total, _ -> total ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Memos
    val allMemos: StateFlow<List<MemoEntity>> = repository.allMemos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMemos: StateFlow<List<MemoEntity>> = memoSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allMemos else repository.searchMemos(query.trim())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's Stats
    private val startOfToday: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val endOfToday: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    val todaySalesTotal: StateFlow<Double> = repository.getTodaySales(startOfToday, endOfToday)
        .combine(MutableStateFlow(0.0)) { amount, _ -> amount ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayDueTotal: StateFlow<Double> = repository.getTodayDue(startOfToday, endOfToday)
        .combine(MutableStateFlow(0.0)) { amount, _ -> amount ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingDueReminders: StateFlow<List<SaleTransactionEntity>> = repository.getPendingDueReminders(endOfToday)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Date for Ledger
    val selectedLedgerDate = MutableStateFlow(System.currentTimeMillis())

    val selectedLedgerTransactions: StateFlow<List<SaleTransactionEntity>> = selectedLedgerDate
        .flatMapLatest { date -> repository.getTransactionsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummary: StateFlow<DailySummary> = combine(
        selectedLedgerDate,
        selectedLedgerTransactions
    ) { date, txs ->
        var totalSales = 0.0
        var totalPaid = 0.0
        var totalDue = 0.0
        var totalQty = 0
        val dueCustomerSet = mutableSetOf<String>()

        for (item in txs) {
            totalSales += item.totalAmount
            totalPaid += item.paidAmount
            if (item.isDue && !item.isPaid) {
                totalDue += item.dueAmount
                item.customerPhone?.let { dueCustomerSet.add(it) }
                    ?: item.customerName?.let { dueCustomerSet.add(it) }
            }
            totalQty += item.quantitySold
        }

        DailySummary(
            dateMillis = date,
            totalSales = totalSales,
            totalCashReceived = totalPaid,
            totalDue = totalDue,
            totalItemsSold = totalQty,
            totalTransactionsCount = txs.size,
            dueCustomersCount = dueCustomerSet.size,
            transactions = txs
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailySummary(System.currentTimeMillis(), 0.0, 0.0, 0.0, 0, 0, 0, emptyList())
    )

    fun setLedgerDate(dateMillis: Long) {
        selectedLedgerDate.value = dateMillis
    }

    // ================= ACTIONS =================

    // Add Product
    fun addProduct(
        name: String,
        buyPrice: Double,
        sellPrice: Double,
        stock: Int,
        unit: String,
        category: String,
        qrCode: String?,
        onSuccess: (ProductEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("পণ্যের নাম দিন", isError = true))
                return@launch
            }
            if (sellPrice < 0.0 || buyPrice < 0.0) {
                _eventFlow.emit(UiEvent.ShowToast("মূল্য ০ বা তার বেশি হতে হবে", isError = true))
                return@launch
            }
            if (stock < 0) {
                _eventFlow.emit(UiEvent.ShowToast("Stock কখনো নেগেটিভ হতে পারে না", isError = true))
                return@launch
            }

            val cleanQr = qrCode?.trim()?.takeIf { it.isNotBlank() }
            if (cleanQr != null) {
                val isUnique = repository.isQrCodeUnique(cleanQr)
                if (!isUnique) {
                    _eventFlow.emit(UiEvent.ShowToast("এই QR Code ইতিমধ্যে ব্যবহার করা হয়েছে। অন্য একটি কোড দিন।", isError = true))
                    return@launch
                }
            }

            val product = ProductEntity(
                name = name.trim(),
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                unit = unit.trim().ifBlank { "পিস" },
                category = category.trim().ifBlank { "সাধারণ" },
                qrCode = cleanQr,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val newId = repository.insertProduct(product)
            val createdProduct = product.copy(id = newId)
            _eventFlow.emit(UiEvent.ShowToast("✓ '${createdProduct.name}' সফলভাবে যুক্ত হয়েছে"))
            _eventFlow.emit(UiEvent.ProductAdded(createdProduct))
            if (cleanQr != null) {
                _eventFlow.emit(UiEvent.QrCreated(createdProduct))
            }
            onSuccess(createdProduct)
        }
    }

    // Stock In / Restock (ক্রয়কৃত পণ্য স্টকে যোগ)
    fun restockProduct(product: ProductEntity, quantityToAdd: Int, onSuccess: (ProductEntity) -> Unit = {}) {
        viewModelScope.launch {
            if (quantityToAdd <= 0) {
                _eventFlow.emit(UiEvent.ShowToast("ক্রয়কৃত পরিমাণ অবশ্যই ০-এর বেশি হতে হবে", isError = true))
                return@launch
            }
            val result = repository.addStock(product.id, quantityToAdd)
            result.onSuccess { updatedProduct ->
                val banglaQty = BengaliFormatters.toBanglaNumber(quantityToAdd)
                val totalBangla = BengaliFormatters.toBanglaNumber(updatedProduct.stock)
                _eventFlow.emit(UiEvent.ShowToast("✓ $banglaQty ${updatedProduct.unit} স্টকে যোগ হয়েছে! বর্তমান মোট স্টক: $totalBangla ${updatedProduct.unit}"))
                onSuccess(updatedProduct)
            }.onFailure { error ->
                _eventFlow.emit(UiEvent.ShowToast(error.message ?: "স্টক বৃদ্ধি করা সম্ভব হয়নি", isError = true))
            }
        }
    }

    // Update Product / Restock on Edit
    fun updateProduct(
        product: ProductEntity,
        additionalStockPurchased: Int = 0,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (product.name.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("পণ্যের নাম খালি রাখা যাবে না", isError = true))
                return@launch
            }
            val cleanQr = product.qrCode?.trim()?.takeIf { it.isNotBlank() }
            if (cleanQr != null) {
                val isUnique = repository.isQrCodeUnique(cleanQr, excludeId = product.id)
                if (!isUnique) {
                    _eventFlow.emit(UiEvent.ShowToast("এই Code ইতিমধ্যে ব্যবহার করা হয়েছে। অন্য একটি Code দিন।", isError = true))
                    return@launch
                }
            }

            val finalStock = if (additionalStockPurchased > 0) {
                product.stock + additionalStockPurchased
            } else {
                product.stock
            }

            repository.updateProduct(
                product.copy(
                    stock = finalStock.coerceAtLeast(0),
                    qrCode = cleanQr,
                    updatedAt = System.currentTimeMillis()
                )
            )
            val msg = if (additionalStockPurchased > 0) {
                "✓ পণ্য আপডেট ও +${BengaliFormatters.toBanglaNumber(additionalStockPurchased)} টি নতুন স্টক যুক্ত হয়েছে! মোট স্টক: ${BengaliFormatters.toBanglaNumber(finalStock.coerceAtLeast(0))}"
            } else {
                "✓ পণ্য সফলভাবে আপডেট হয়েছে"
            }
            _eventFlow.emit(UiEvent.ShowToast(msg))
            onSuccess()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _eventFlow.emit(UiEvent.ShowToast("পণ্য মুছে ফেলা হয়েছে"))
        }
    }

    // Process Sale from QR Code or Manual
    fun processSale(
        product: ProductEntity,
        quantitySold: Int,
        totalAmount: Double,
        paidAmount: Double,
        dueAmount: Double,
        isDue: Boolean,
        customerName: String?,
        customerPhone: String?,
        shopkeeperName: String?,
        dueDate: Long?,
        note: String? = null,
        onSuccess: (SaleTransactionEntity) -> Unit
    ) {
        viewModelScope.launch {
            if (quantitySold <= 0) {
                _eventFlow.emit(UiEvent.ShowToast("বিক্রির পরিমাণ অবশ্যই ০-এর বেশি হতে হবে", isError = true))
                return@launch
            }
            if (quantitySold > product.stock) {
                _eventFlow.emit(UiEvent.ShowToast("পর্যাপ্ত Stock নেই। বর্তমান স্টক: ${product.stock} ${product.unit}", isError = true))
                return@launch
            }

            if (isDue) {
                if (dueAmount <= 0.0) {
                    _eventFlow.emit(UiEvent.ShowToast("বাকি টাকার পরিমাণ সঠিক নয়", isError = true))
                    return@launch
                }
                if (customerPhone.isNullOrBlank()) {
                    _eventFlow.emit(UiEvent.ShowToast("বাকি থাকলে Customer-এর মোবাইল নম্বর প্রয়োজন", isError = true))
                    return@launch
                }
            }

            val owner = shopkeeperName?.takeIf { it.isNotBlank() }
                ?: shopProfile.value?.ownerName
                ?: "দোকানদার"

            val result = repository.performSale(
                product = product,
                quantitySold = quantitySold,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                dueAmount = dueAmount,
                isDue = isDue,
                customerName = customerName,
                customerPhone = customerPhone,
                shopkeeperName = owner,
                dueDate = dueDate,
                note = note
            )

            result.onSuccess { transaction ->
                val remainingBangla = BengaliFormatters.toBanglaNumber(transaction.currentStock)
                _eventFlow.emit(UiEvent.ShowToast("✓ বিক্রি সম্পন্ন! অবশিষ্ট স্টক: $remainingBangla ${product.unit}"))
                _eventFlow.emit(UiEvent.SaleCompleted(transaction))
                onSuccess(transaction)
            }.onFailure { error ->
                _eventFlow.emit(UiEvent.ShowToast(error.message ?: "বিক্রি সম্পন্ন করা সম্ভব হয়নি", isError = true))
            }
        }
    }

    // Save Memo
    fun saveMemo(
        memo: MemoEntity,
        items: List<MemoItem>,
        deductFromInventory: Boolean = false,
        onSuccess: (MemoEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (memo.customerName.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("ক্রেতা বা প্রাপকের নাম লিখুন", isError = true))
                return@launch
            }
            if (items.isEmpty()) {
                _eventFlow.emit(UiEvent.ShowToast("মেমোতে অন্তত একটি পণ্য বা বিবরণ যোগ করুন", isError = true))
                return@launch
            }

            val memoId = repository.insertMemo(memo)
            val savedMemo = memo.copy(id = memoId)

            _eventFlow.emit(UiEvent.ShowToast("✓ মেমো নং: ${savedMemo.memoNumber} সফলভাবে সংরক্ষিত হয়েছে"))
            _eventFlow.emit(UiEvent.MemoSaved(savedMemo))
            onSuccess(savedMemo)
        }
    }

    fun deleteMemo(memo: MemoEntity) {
        viewModelScope.launch {
            repository.deleteMemo(memo)
            _eventFlow.emit(UiEvent.ShowToast("মেমো মুছে ফেলা হয়েছে"))
        }
    }

    fun deleteTransaction(transaction: SaleTransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _eventFlow.emit(UiEvent.ShowToast("লেনদেনের হিসাব মুছে ফেলা হয়েছে"))
        }
    }

    // Mark Due As Paid
    fun markDueAsPaid(transaction: SaleTransactionEntity) {
        viewModelScope.launch {
            repository.markDueAsPaid(transaction.id)
            val name = transaction.customerName ?: transaction.customerPhone ?: "গ্রাহক"
            _eventFlow.emit(UiEvent.ShowToast("✓ বাকি টাকা Paid হিসেবে চিহ্নিত হয়েছে"))
            _eventFlow.emit(UiEvent.PaymentPaid(name, transaction.dueAmount))
        }
    }

    // Lookup Product by QR Code
    suspend fun getProductByQrCode(qrCode: String): ProductEntity? {
        return repository.getProductByQr(qrCode)
    }

    // Save Shop Profile
    fun saveShopProfile(
        shopName: String,
        ownerName: String,
        phone: String,
        address: String
    ) {
        viewModelScope.launch {
            val current = repository.getShopProfileOnce()
            val updated = current.copy(
                shopName = shopName.trim().ifBlank { "আমার ব্যবসা" },
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                address = address.trim()
            )
            repository.saveShopProfile(updated)
            _eventFlow.emit(UiEvent.ShowToast("✓ প্রতিষ্ঠানের তথ্য সংরক্ষণ হয়েছে"))
        }
    }

    // Clear / Reset All Data
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _eventFlow.emit(UiEvent.ShowToast("✓ সকল তথ্য মুছে ফেলা হয়েছে"))
        }
    }

    // Community Credit Records Flow
    val communityCreditRecords: StateFlow<List<CommunityCreditRecordEntity>> = repository.allCommunityRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Calculates AI Credit Trust Score for a customer by combining local transaction history and community reports.
     */
    suspend fun getCustomerTrustScore(name: String, phone: String?): TrustScoreResult {
        val cleanPhone = phone?.replace(Regex("[^0-9]"), "")
        val communityRecord = if (!cleanPhone.isNullOrBlank()) {
            repository.getCommunityRecordByPhone(cleanPhone)
        } else null

        val localSales = allTransactions.value
        val localMemos = allMemos.value

        return CustomerTrustScoreEngine.calculateScore(
            customerName = name,
            customerPhone = phone,
            localSales = localSales,
            localMemos = localMemos,
            communityRecord = communityRecord
        )
    }

    /**
     * Anonymously reports a defaulter customer to the decentralized Community Credit Register.
     */
    fun reportDefaulter(
        phone: String,
        customerName: String,
        amount: Double,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanPhone = phone.replace(Regex("[^0-9]"), "")
            if (cleanPhone.length < 11) {
                _eventFlow.emit(UiEvent.ShowToast("সঠিক মোবাইল নম্বর দিন (কমপক্ষে ১১ ডিজিট)", isError = true))
                return@launch
            }

            repository.reportDefaulterToCommunity(
                phone = cleanPhone,
                customerName = customerName.trim(),
                amount = amount,
                note = note.trim()
            )

            _eventFlow.emit(UiEvent.ShowToast("✓ কাস্টমারকে বেনামে কমিউনিটি রিস্ক ডাটাবেজে ডিফল্টার মার্ক করা হয়েছে"))
            onSuccess()
        }
    }

    /**
     * Removes an anonymous community report.
     */
    fun removeCommunityReport(record: CommunityCreditRecordEntity) {
        viewModelScope.launch {
            repository.removeCommunityReport(record)
            _eventFlow.emit(UiEvent.ShowToast("কমিউনিটি রিপোর্ট প্রত্যাহার করা হয়েছে"))
        }
    }

    /**
     * Processes AI Voice-to-Ledger transaction.
     */
    fun processVoiceTransaction(
        parsed: ParsedVoiceEntry,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val shop = repository.getShopProfileOnce()
            val memoNo = MemoUtils.generateMemoNumber()

            when (parsed.transactionType) {
                VoiceTransactionType.DUE_SALE -> {
                    // Create Due Sale / Memo
                    val memo = MemoEntity(
                        memoNumber = memoNo,
                        memoType = "বাকি মেমো (Due)",
                        date = System.currentTimeMillis(),
                        shopName = shop.shopName.ifBlank { "আমার ব্যবসা" },
                        preparedBy = shop.ownerName,
                        shopPhone = shop.phone,
                        shopAddress = shop.address,
                        customerName = parsed.customerName,
                        customerPhone = parsed.customerPhone,
                        itemsJson = MemoUtils.serializeItems(parsed.items),
                        subtotal = parsed.totalAmount,
                        grandTotal = parsed.totalAmount,
                        paidAmount = parsed.paidAmount,
                        dueAmount = parsed.dueAmount,
                        paymentMethod = "বাকি",
                        notes = "${parsed.notes}${if (parsed.promisedDueDateMillis != null) " | পরিশোধের প্রতিশ্রুত তারিখ: " + BengaliFormatters.formatDateBangla(parsed.promisedDueDateMillis) else ""}",
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertMemo(memo)
                    _eventFlow.emit(UiEvent.ShowToast("✓ মুখে বলা বাকির হিসাব (৳${BengaliFormatters.toBanglaCurrency(parsed.dueAmount)}) সংরক্ষিত হয়েছে"))
                    _eventFlow.emit(UiEvent.MemoSaved(memo))
                }

                VoiceTransactionType.CASH_SALE -> {
                    val memo = MemoEntity(
                        memoNumber = memoNo,
                        memoType = "ক্যাশ মেমো (Cash)",
                        date = System.currentTimeMillis(),
                        shopName = shop.shopName.ifBlank { "আমার ব্যবসা" },
                        preparedBy = shop.ownerName,
                        shopPhone = shop.phone,
                        shopAddress = shop.address,
                        customerName = parsed.customerName,
                        customerPhone = parsed.customerPhone,
                        itemsJson = MemoUtils.serializeItems(parsed.items),
                        subtotal = parsed.totalAmount,
                        grandTotal = parsed.totalAmount,
                        paidAmount = parsed.totalAmount,
                        dueAmount = 0.0,
                        paymentMethod = "নগদ",
                        notes = parsed.notes,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertMemo(memo)
                    _eventFlow.emit(UiEvent.ShowToast("✓ মুখে বলা নগদ বিক্রি (৳${BengaliFormatters.toBanglaCurrency(parsed.totalAmount)}) সংরক্ষিত হয়েছে"))
                    _eventFlow.emit(UiEvent.MemoSaved(memo))
                }

                VoiceTransactionType.PAYMENT_IN -> {
                    // Payment received
                    val memo = MemoEntity(
                        memoNumber = memoNo,
                        memoType = "জমা রশিদ (Payment In)",
                        date = System.currentTimeMillis(),
                        shopName = shop.shopName.ifBlank { "আমার ব্যবসা" },
                        preparedBy = shop.ownerName,
                        shopPhone = shop.phone,
                        shopAddress = shop.address,
                        customerName = parsed.customerName,
                        customerPhone = parsed.customerPhone,
                        itemsJson = MemoUtils.serializeItems(parsed.items),
                        subtotal = parsed.totalAmount,
                        grandTotal = parsed.totalAmount,
                        paidAmount = parsed.totalAmount,
                        dueAmount = 0.0,
                        paymentMethod = "নগদ জমা",
                        notes = "বকেয়া উসুল / জমা",
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertMemo(memo)
                    _eventFlow.emit(UiEvent.ShowToast("✓ ${parsed.customerName}-এর ৳${BengaliFormatters.toBanglaCurrency(parsed.totalAmount)} টাকা জমা নেওয়া হয়েছে"))
                    _eventFlow.emit(UiEvent.PaymentPaid(parsed.customerName, parsed.totalAmount))
                }
            }
            onSuccess()
        }
    }
}
