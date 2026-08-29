package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopProfileDao {

    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    fun getShopProfile(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    suspend fun getShopProfileOnce(): ShopProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: ShopProfileEntity)

    @Update
    suspend fun update(profile: ShopProfileEntity)
}
