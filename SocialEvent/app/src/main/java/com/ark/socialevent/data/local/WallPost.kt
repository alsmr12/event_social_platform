package com.ark.socialevent.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wall_posts")
data class WallPost(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val authorId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
