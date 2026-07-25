package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    val category: String = "",
    val supplier: String = "",
    val cost: Double,
    val price: Double,
    val stock: Int,
    val minStock: Int = 5,
    val description: String = ""
)