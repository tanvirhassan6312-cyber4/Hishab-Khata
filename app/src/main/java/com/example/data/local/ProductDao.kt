package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getProductByQrCode(qrCode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE qrCode IS NOT NULL AND qrCode != '' ORDER BY updatedAt DESC")
    fun getProductsWithQr(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR qrCode LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products WHERE qrCode = :qrCode AND id != :excludeId")
    suspend fun countQrCodeUsage(qrCode: String, excludeId: Long = -1): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = :newStock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT SUM(stock) FROM products")
    fun getTotalStockCount(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM products")
    fun getTotalProductCount(): Flow<Int>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
