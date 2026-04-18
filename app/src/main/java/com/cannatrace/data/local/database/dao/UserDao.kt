package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE authToken = :token LIMIT 1")
    suspend fun getUserByToken(token: String): UserEntity?

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET authToken = :token WHERE id = :userId")
    suspend fun updateAuthToken(userId: String, token: String)

    @Query("UPDATE users SET authToken = '' WHERE id = :userId")
    suspend fun clearAuthToken(userId: String)

    @Query("SELECT * FROM users WHERE authToken != '' LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET authToken = '' WHERE authToken != ''")
    suspend fun clearAllTokens()
}
