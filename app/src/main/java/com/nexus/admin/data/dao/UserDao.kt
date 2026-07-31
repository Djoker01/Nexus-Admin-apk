package com.nexus.admin.data.dao

import androidx.room.*
import com.nexus.admin.data.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): User?

    @Query("SELECT * FROM users WHERE role = 'admin' LIMIT 1")
    suspend fun getAdmin(): User?

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
