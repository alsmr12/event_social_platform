package com.ark.socialevent.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val firstName: String?,
    val lastName: String?,
    val gender: String?,
    val birthDate: String?,
    val phone: String?
)
