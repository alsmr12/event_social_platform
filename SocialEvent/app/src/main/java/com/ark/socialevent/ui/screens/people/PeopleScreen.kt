// app/src/main/java/com/ark/socialevent/ui/screens/people/PeopleScreen.kt
package com.ark.socialevent.ui.screens.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PeopleScreen() {
    // Временные тестовые данные
    val testUsers = listOf(
        TestUser(1, "user1@test.com", "Иван", "Иванов", "Мужской", 25, "+7 (123) 456-78-90"),
        TestUser(2, "user2@test.com", "Мария", "Петрова", "Женский", 22, "+7 (987) 654-32-10"),
        TestUser(3, "user3@test.com", "Алексей", "Сидоров", "Мужской", 30, "+7 (111) 222-33-44")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Люди",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(testUsers) { user ->
                UserCard(user = user)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Временный класс для теста
data class TestUser(
    val id: Int,
    val email: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val age: Int,
    val phone: String
)

@Composable
fun UserCard(user: TestUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Аватар",
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Возраст: ${user.age}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}