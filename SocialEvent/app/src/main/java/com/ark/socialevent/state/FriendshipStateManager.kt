package com.ark.socialevent.state

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object FriendshipStateManager {
    var refreshFriendsTrigger by mutableStateOf(0)
    var refreshPeopleTrigger by mutableStateOf(0)

    fun refreshFriends() {
        refreshFriendsTrigger++
    }

    fun refreshPeople() {
        refreshPeopleTrigger++
    }

    fun refreshAll() {
        refreshFriendsTrigger++
        refreshPeopleTrigger++
    }
}