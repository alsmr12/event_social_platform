// app/src/main/java/com/ark/socialevent/ui/screens/home/HomeScreen.kt
package com.ark.socialevent.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Добро пожаловать!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Здесь будет лента событий и активностей",
            style = MaterialTheme.typography.bodyMedium
        )

        // Можно добавить список событий, посты и т.д.
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO */ }) {
            Text("Создать событие")
        }
    }
}