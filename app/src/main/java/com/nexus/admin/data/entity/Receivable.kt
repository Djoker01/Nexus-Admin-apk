package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receivables")
data class Receivable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val date: Long = System.currentTimeMillis(),
    val concept: String,
    val totalAmount: Double,
    val balance: Double,
    val status: String = "pending",
    val payments: List<Payment> = emptyList(),
    val notes: String = ""
)

data class Payment(
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val method: String
)