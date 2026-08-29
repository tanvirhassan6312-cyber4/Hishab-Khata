package com.example.data.local

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ShopRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val shopProfileDao: ShopProfileDao,
    private val memoDao: MemoDao
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
