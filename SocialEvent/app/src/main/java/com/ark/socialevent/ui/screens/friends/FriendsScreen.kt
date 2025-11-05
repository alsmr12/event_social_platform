// app/src/main/java/com/ark/socialevent/ui/screens/friends/FriendsScreen.kt
package com.ark.socialevent.ui.screens.friends

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FriendsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Друзья",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // TODO: Добавить список друзей, заявки в друзья
        Text("Здесь х друзmz")
    }
}