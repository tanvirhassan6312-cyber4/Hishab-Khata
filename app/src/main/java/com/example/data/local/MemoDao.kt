package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity): Long

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("SELECT * FROM memos ORDER BY date DESC, id DESC")
    fun getAllMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    suspend fun getMemoById(id: Long): MemoEntity?

    @Query("SELECT * FROM memos WHERE memoNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%' OR customerPhone LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchMemos(query: String): Flow<List<MemoEntity>>

    @Query("SELECT COUNT(*) FROM memos")
    fun getTotalMemoCount(): Flow<Int>

    @Query("DELETE FROM memos")
    suspend fun deleteAllMemos()
}
