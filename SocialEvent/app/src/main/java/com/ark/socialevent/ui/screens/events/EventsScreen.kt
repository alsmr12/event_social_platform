// app/src/main/java/com/ark/socialevent/ui/screens/events/EventsScreen.kt
package com.ark.socialevent.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EventsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "События",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Здесь будет список событий
        // TODO: Добавить LazyColumn с событиями

        Button(
            onClick = { /* TODO: Навигация на создание события */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Создать новое событие")
        }
    }
}