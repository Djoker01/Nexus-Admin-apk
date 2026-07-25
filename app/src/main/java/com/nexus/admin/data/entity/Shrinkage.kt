package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shrinkages")
data class Shrinkage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: String,
    val quantity: Int,
    val loss: Double,
    val date: Long = System.currentTimeMillis(),
    val reason: String = ""
)