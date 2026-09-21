package com.example.data.local

import com.example.util.CustomerTrustScoreEngine
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ShopRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val shopProfileDao: ShopProfileDao,
    private val memoDao: MemoDao,
    private val communityCreditDao: CommunityCreditDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val productsWithQr: Flow<List<ProductEntity>> = productDao.getProductsWithQr()
    val totalStock: Flow<Int?> = productDao.getTotalStockCount()
    val totalProducts: Flow<Int> = productDao.getTotalProductCount()

    val allTransactions: Flow<List<SaleTransactionEntity>> = saleDao.getAllTransactions()
    val allDueTransactions: Flow<List<SaleTransactionEntity>> = saleDao.getAllDueTransactions()
    val allPendingDueTotal: Flow<Double?> = saleDao.getAllPendingDueTotal()
    val shopProfile: Flow<ShopProfileEntity?> = shopProfileDao.getShopProfile()
    val allMemos: Flow<List<MemoEntity>> = memoDao.getAllMemos()
    val allCommunityRecords: Flow<List<CommunityCreditRecordEntity>> = communityCreditDao.getAllRecords()

    suspend fun getCommunityRecordByPhone(phone: String): CommunityCreditRecordEntity? {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.isBlank()) return null
        return communityCreditDao.getRecordByPhone(cleanPhone)
    }

    suspend fun reportDefaulterToCommunity(
        phone: String,
        customerName: String,
        amount: Double,
        note: String
    ): Long {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val hash = CustomerTrustScoreEngine.hashPhoneNumber(cleanPhone)
        val existing = communityCreditDao.getRecordByPhone(cleanPhone)
        val reportCount = (existing?.defaultReportsCount ?: 0) + 1
        val record = CommunityCreditRecordEntity(
            id = existing?.id ?: 0,
            customerPhone = cleanPhone,
            phoneHash = hash,
            customerName = customerName,
            defaultReportsCount = reportCount,
            totalOverdueReported = (existing?.totalOverdueReported ?: 0.0) + amount,
            isDefaulter = true,
            riskLevel = "HIGH",
            anonymousNote = note.ifBlank { "অন্য ১টি দোকানে অপরিশোধিত বাকি রয়েছে।" },
            reportedTimestamp = System.currentTimeMillis()
        )
        return communityCreditDao.insertOrUpdate(record)
    }

    suspend fun removeCommunityReport(record: CommunityCreditRecordEntity) {
        communityCreditDao.delete(record)
    }

    suspend fun seedSampleCommunityRecordsIfEmpty() {
        if (communityCreditDao.getCount() == 0) {
            // Seed a few sample community anonymous alert records
            val sample1 = CommunityCreditRecordEntity(
                customerPhone = "01711000000",
                phoneHash = CustomerTrustScoreEngine.hashPhoneNumber("01711000000"),
                customerName = "করিম (নমুনা)",
                defaultReportsCount = 2,
                totalOverdueReported = 6500.0,
                isDefaulter = true,
                riskLevel = "HIGH",
                anonymousNote = "অন্য ২টি দোকানে বড় অংকের বকেয়া পরিশোধ করেনি।"
            )
            val sample2 = CommunityCreditRecordEntity(
                customerPhone = "01819000000",
                phoneHash = CustomerTrustScoreEngine.hashPhoneNumber("01819000000"),
                customerName = "আলমগীর (নমুনা)",
                defaultReportsCount = 1,
                totalOverdueReported = 3200.0,
                isDefaulter = true,
                riskLevel = "HIGH",
                anonymousNote = "অন্য ১টি দোকানে মেয়াদ শেষ হওয়ার পরও টাকা দেয়নি।"
            )
            communityCreditDao.insertOrUpdate(sample1)
            communityCreditDao.insertOrUpdate(sample2)
        }
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)
    fun searchTransactions(query: String): Flow<List<SaleTransactionEntity>> = saleDao.searchTransactions(query)
    fun searchMemos(query: String): Flow<List<MemoEntity>> = memoDao.searchMemos(query)

    fun getTransactionsForDate(dateMillis: Long): Flow<List<SaleTransactionEntity>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis
        return saleDao.getTransactionsForDateRange(startOfDay, endOfDay)
    }

    fun getTodaySales(startOfDay: Long, endOfDay: Long): Flow<Double?> =
        saleDao.getTodayTotalSales(startOfDay, endOfDay)

    fun getTodayDue(startOfDay: Long, endOfDay: Long): Flow<Double?> =
        saleDao.getTodayTotalDue(startOfDay, endOfDay)

    fun getPendingDueReminders(endOfDay: Long): Flow<List<SaleTransactionEntity>> =
        saleDao.getPendingDueReminders(endOfDay)

    suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)

    suspend fun getProductByQr(qrCode: String): ProductEntity? =
        productDao.getProductByQrCode(qrCode.trim())

    suspend fun isQrCodeUnique(qrCode: String, excludeId: Long = -1): Boolean {
        if (qrCode.isBlank()) return true
        val count = productDao.countQrCodeUsage(qrCode.trim(), excludeId)
        return count == 0
    }

    suspend fun insertProduct(product: ProductEntity): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    suspend fun performSale(
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
        note: String? = null
    ): Result<SaleTransactionEntity> {
        if (quantitySold <= 0) {
            return Result.failure(IllegalArgumentException("বিক্রির পরিমাণ অবশ্যই ০-এর বেশি হতে হবে"))
        }
        if (quantitySold > product.stock) {
            return Result.failure(IllegalStateException("পর্যাপ্ত Stock নেই। বর্তমান স্টক: ${product.stock} ${product.unit}"))
        }

        val previousStock = product.stock
        val newStock = previousStock - quantitySold

        // 1. Update product stock in DB
        productDao.updateStock(product.id, newStock)

        // 2. Insert transaction
        val transaction = SaleTransactionEntity(
            productId = product.id,
            productName = product.name,
            previousStock = previousStock,
            quantitySold = quantitySold,
            currentStock = newStock,
            unitPrice = product.sellPrice,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            dueAmount = if (isDue) dueAmount else 0.0,
            isDue = isDue && dueAmount > 0.0,
            isPaid = !isDue || dueAmount <= 0.0,
            customerName = customerName?.takeIf { it.isNotBlank() },
            customerPhone = customerPhone?.takeIf { it.isNotBlank() },
            shopkeeperName = shopkeeperName?.takeIf { it.isNotBlank() },
            dueDate = if (isDue) dueDate else null,
            note = note,
            qrCodeUsed = product.qrCode,
            createdAt = System.currentTimeMillis()
        )

        val transactionId = saleDao.insertTransaction(transaction)
        return Result.success(transaction.copy(id = transactionId))
    }

    suspend fun addStock(productId: Long, quantityToAdd: Int): Result<ProductEntity> {
        if (quantityToAdd <= 0) {
            return Result.failure(IllegalArgumentException("যুক্ত করার পরিমাণ ০-এর বেশি হতে হবে"))
        }
        val product = productDao.getProductById(productId)
            ?: return Result.failure(IllegalArgumentException("পণ্যটি পাওয়া যায়নি"))
        val newStock = product.stock + quantityToAdd
        val updatedProduct = product.copy(stock = newStock, updatedAt = System.currentTimeMillis())
        productDao.updateProduct(updatedProduct)
        return Result.success(updatedProduct)
    }

    suspend fun insertMemo(memo: MemoEntity): Long = memoDao.insertMemo(memo)
    suspend fun updateMemo(memo: MemoEntity) = memoDao.updateMemo(memo)
    suspend fun deleteMemo(memo: MemoEntity) = memoDao.deleteMemo(memo)
    suspend fun getMemoById(id: Long): MemoEntity? = memoDao.getMemoById(id)

    suspend fun markDueAsPaid(transactionId: Long) {
        saleDao.markAsPaid(transactionId)
    }

    suspend fun saveShopProfile(profile: ShopProfileEntity) {
        shopProfileDao.insertOrUpdate(profile)
    }

    suspend fun getShopProfileOnce(): ShopProfileEntity {
        return shopProfileDao.getShopProfileOnce() ?: ShopProfileEntity(
            shopName = "আমার ব্যবসা",
            ownerName = "",
            phone = "",
            address = ""
        ).also {
            shopProfileDao.insertOrUpdate(it)
        }
    }

    suspend fun clearAllData() {
        productDao.deleteAllProducts()
        saleDao.deleteAllTransactions()
        memoDao.deleteAllMemos()
        val defaultProfile = ShopProfileEntity(
            id = 1,
            shopName = "আমার ব্যবসা",
            ownerName = "",
            phone = "",
            address = ""
        )
        shopProfileDao.insertOrUpdate(defaultProfile)
    }
}
