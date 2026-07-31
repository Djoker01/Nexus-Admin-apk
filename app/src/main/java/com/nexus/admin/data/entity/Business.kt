package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val ownerName: String,
    val address: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
