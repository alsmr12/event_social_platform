package com.ark.socialevent.data.local

import androidx.room.Entity

@Entity(tableName = "friendships", primaryKeys = ["userId", "friendId"])
data class Friendship(
    val userId: Int,
    val friendId: Int
)
