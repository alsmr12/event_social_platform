// app/src/main/java/com/ark/socialevent/ui/screens/profile/ProfileScreen.kt
package com.ark.socialevent.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Мой профиль",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // TODO: Добавить информацию о пользователе, аватар, статистику
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Имя пользователя", style = MaterialTheme.typography.titleMedium)
                Text("email@example.com", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}