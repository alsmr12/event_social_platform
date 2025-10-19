package com.ark.socialevent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FriendshipDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFriend(friendship: Friendship)

    @Query("DELETE FROM friendships WHERE (userId = :userId AND friendId = :friendId) OR (userId = :friendId AND friendId = :userId)")
    suspend fun removeFriend(userId: Int, friendId: Int)

    @Query("SELECT * FROM users WHERE id IN (SELECT friendId FROM friendships WHERE userId = :userId UNION SELECT userId FROM friendships WHERE friendId = :userId)")
    suspend fun getFriends(userId: Int): List<User>
}
