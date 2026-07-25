package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.Restock
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockDao {
    @Query("SELECT * FROM restocks ORDER BY date DESC")
    fun getAllRestocks(): Flow<List<Restock>>

    @Query("SELECT SUM(total) FROM restocks WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalRestocksByDateRange(startDate: Long, endDate: Long): Double?

    @Insert
    suspend fun insert(restock: Restock): Long

    @Query("DELETE FROM restocks")
    suspend fun deleteAll()
}