package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stock <= minStock AND stock > 0")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stock = 0")
    fun getOutOfStockProducts(): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products WHERE category != ''")
    fun getAllCategories(): Flow<List<String>>

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}