package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.Receivable
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM receivables ORDER BY date DESC")
    fun getAllReceivables(): Flow<List<Receivable>>

    @Query("SELECT * FROM receivables WHERE id = :id")
    suspend fun getReceivableById(id: Long): Receivable?

    @Query("SELECT SUM(balance) FROM receivables WHERE status != 'paid'")
    suspend fun getTotalPending(): Double?

    @Query("SELECT COUNT(*) FROM receivables WHERE status != 'paid'")
    suspend fun getPendingCount(): Int

    @Insert
    suspend fun insert(receivable: Receivable): Long

    @Update
    suspend fun update(receivable: Receivable)

    @Query("DELETE FROM receivables")
    suspend fun deleteAll()
}