package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE date >= :startDate AND date <= :endDate")
    fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT SUM(total) FROM sales WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalSalesByDateRange(startDate: Long, endDate: Long): Double?

    @Query("SELECT COUNT(*) FROM sales WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTransactionCountByDateRange(startDate: Long, endDate: Long): Int

    @Insert
    suspend fun insert(sale: Sale): Long

    @Update
    suspend fun update(sale: Sale)   // ← AGREGAR ESTA LÍNEA

    @Delete
    suspend fun delete(sale: Sale)

    @Query("DELETE FROM sales")
    suspend fun deleteAll()
}
