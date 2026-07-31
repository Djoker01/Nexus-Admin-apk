package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.Business
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY name ASC")
    fun getAllBusinesses(): Flow<List<Business>>

    @Query("SELECT * FROM businesses WHERE code = :code LIMIT 1")
    suspend fun getBusinessByCode(code: String): Business?

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: Long): Business?

    @Insert
    suspend fun insert(business: Business): Long

    @Update
    suspend fun update(business: Business)

    @Delete
    suspend fun delete(business: Business)

    @Query("DELETE FROM businesses")
    suspend fun deleteAll()
}
