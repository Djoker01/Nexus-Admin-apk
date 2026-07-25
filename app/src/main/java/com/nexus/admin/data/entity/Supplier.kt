package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val contact: String = "",
    val deliveryTime: String = "",
    val paymentTerms: String = "",
    val notes: String = ""
)