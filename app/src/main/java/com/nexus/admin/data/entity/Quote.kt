package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val supplier: String,
    val price: Double,
    val minQuantity: Int = 1,
    val deliveryDays: Int = 0,
    val paymentTerms: String = "",
    val includesShipping: Boolean = false
)