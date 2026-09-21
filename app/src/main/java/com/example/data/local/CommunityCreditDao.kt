package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityCreditDao {

    @Query("SELECT * FROM community_credit_records ORDER BY reportedTimestamp DESC")
    fun getAllRecords(): Flow<List<CommunityCreditRecordEntity>>

    @Query("SELECT * FROM community_credit_records WHERE customerPhone = :phone LIMIT 1")
    suspend fun getRecordByPhone(phone: String): CommunityCreditRecordEntity?

    @Query("SELECT * FROM community_credit_records WHERE phoneHash = :hash LIMIT 1")
    suspend fun getRecordByHash(hash: String): CommunityCreditRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: CommunityCreditRecordEntity): Long

    @Delete
    suspend fun delete(record: CommunityCreditRecordEntity)

    @Query("DELETE FROM community_credit_records WHERE customerPhone = :phone")
    suspend fun deleteByPhone(phone: String)

    @Query("SELECT COUNT(*) FROM community_credit_records")
    suspend fun getCount(): Int
}
