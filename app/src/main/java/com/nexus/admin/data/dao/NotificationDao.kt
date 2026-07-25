package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY date DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE read = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert
    suspend fun insert(notification: AppNotification): Long

    @Update
    suspend fun update(notification: AppNotification)

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllAsRead()

    @Delete
    suspend fun delete(notification: AppNotification)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}