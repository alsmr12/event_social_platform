package com.ark.socialevent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WallPostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: WallPost)

    @Query("SELECT * FROM wall_posts WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getPostsForUser(userId: Int): List<WallPost>
}
