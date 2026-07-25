package com.nexus.admin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long? = null,
    val type: String,
    val message: String,
    val read: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val section: String = ""
)