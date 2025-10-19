package com.ark.socialevent.data.repository

import com.ark.socialevent.data.local.User
import com.ark.socialevent.data.local.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    suspend fun registerUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (userDao.getUserByEmail(user.email) != null) {
                throw Exception("User with this email already exists.")
            }
            val id = userDao.insertUser(user)
            Result.success(user.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, passwordHash: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserByEmail(email) ?: throw Exception("User not found.")
            if (user.passwordHash != passwordHash) { // Plain text comparison for now
                throw Exception("Invalid password.")
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: Int): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId) ?: throw Exception("User not found.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            userDao.updateUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            Result.success(userDao.getAllUsers())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
