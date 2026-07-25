package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val client: String = "",
    val products: List<SaleProduct> = emptyList(),
    val total: Double,
    val cost: Double,
    val paymentMethod: String,
    val date: Long = System.currentTimeMillis()
)

data class SaleProduct(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val price: Double,
    val cost: Double
)