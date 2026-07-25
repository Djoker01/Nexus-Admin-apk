package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val isFixed: Boolean = false,
    val fromCash: Boolean = false,
    val invoice: String = "",
    val notes: String = ""
)