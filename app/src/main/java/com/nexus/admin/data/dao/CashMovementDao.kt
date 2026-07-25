package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.CashMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface CashMovementDao {
    @Query("SELECT * FROM cash_movements ORDER BY date DESC")
    fun getAllMovements(): Flow<List<CashMovement>>

    @Query("SELECT * FROM cash_movements WHERE date >= :startDate AND date <= :endDate")
    fun getMovementsByDateRange(startDate: Long, endDate: Long): Flow<List<CashMovement>>

    @Query("SELECT SUM(CASE WHEN type = 'Ingreso' THEN amount ELSE -amount END) FROM cash_movements")
    suspend fun getCurrentBalance(): Double?

    @Insert
    suspend fun insert(movement: CashMovement): Long

    @Query("DELETE FROM cash_movements")
    suspend fun deleteAll()
}