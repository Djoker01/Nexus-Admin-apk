package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.Quote
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes")
    fun getAllQuotes(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE productId = :productId")
    fun getQuotesByProduct(productId: Long): Flow<List<Quote>>

    @Insert
    suspend fun insert(quote: Quote): Long

    @Query("DELETE FROM quotes")
    suspend fun deleteAll()
}