package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restocks")
data class Restock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplier: String,
    val products: List<RestockProduct> = emptyList(),
    val total: Double,
    val date: Long = System.currentTimeMillis(),
    val invoice: String = "",
    val discountFromCash: Boolean = false,
    val notes: String = ""
)

data class RestockProduct(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val cost: Double
)