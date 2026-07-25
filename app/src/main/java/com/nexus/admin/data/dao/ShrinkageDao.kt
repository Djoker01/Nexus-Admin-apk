package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.Shrinkage
import kotlinx.coroutines.flow.Flow

@Dao
interface ShrinkageDao {
    @Query("SELECT * FROM shrinkages ORDER BY date DESC")
    fun getAllShrinkages(): Flow<List<Shrinkage>>

    @Query("SELECT SUM(loss) FROM shrinkages WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalLossByDateRange(startDate: Long, endDate: Long): Double?

    @Insert
    suspend fun insert(shrinkage: Shrinkage): Long

    @Query("DELETE FROM shrinkages")
    suspend fun deleteAll()
}