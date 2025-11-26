// SubscriptionsScreen.kt
package com.ark.socialevent.ui.screens.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.Subscription
import com.ark.socialevent.network.UserRepository
import kotlinx.coroutines.launch

@Composable
fun SubscriptionsScreen(
    userRepository: UserRepository,
    onOpenProfile: (Int) -> Unit
) {
    var subscriptions by remember { mutableStateOf<List<Subscription>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadSubscriptions() {
        loading = true
        userRepository.getSubscriptions { subs, error ->
            subscriptions = subs ?: emptyList()
            loading = false
            errorMessage = error
        }
    }

    LaunchedEffect(Unit) {
        loadSubscriptions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Заголовок
        Text(
            text = "Мои подписки",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(errorMessage ?: "Ошибка", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { loadSubscriptions() }) {
                        Text("Повторить")
                    }
                }
            }
            subscriptions.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = "Нет подписок",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "У вас пока нет подписок",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Подпишитесь на интересных людей",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(subscriptions) { subscription ->
                        SubscriptionItem(
                            subscription = subscription,
                            onUnsubscribe = { userId ->
                                coroutineScope.launch {
                                    userRepository.unsubscribeFromUser(userId) { success, message ->
                                        if (success) {
                                            loadSubscriptions()
                                        } else {
                                            errorMessage = message
                                        }
                                    }
                                }
                            },
                            onOpenProfile = onOpenProfile
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionItem(
    subscription: Subscription,
    onUnsubscribe: (Int) -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    var showUnsubscribeDialog by remember { mutableStateOf(false) }
    var isUnsubscribing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onOpenProfile(subscription.following.id)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватар
            Icon(
                Icons.Default.Person,
                contentDescription = "Пользователь",
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onOpenProfile(subscription.following.id) },
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о пользователе
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenProfile(subscription.following.id) }
            ) {
                Text(
                    text = "${subscription.following.firstName} ${subscription.following.lastName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subscription.following.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Подписан(а)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Красивая кнопка отписки
            if (isUnsubscribing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                OutlinedButton(
                    onClick = { showUnsubscribeDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Отписаться",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отписаться")
                }
            }
        }
    }

    // Диалог подтверждения отписки
    if (showUnsubscribeDialog) {
        AlertDialog(
            onDismissRequest = { showUnsubscribeDialog = false },
            title = {
                Text(
                    "Отписаться?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "Вы действительно хотите отписаться от ${subscription.following.firstName} ${subscription.following.lastName}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isUnsubscribing = true
                        showUnsubscribeDialog = false
                        onUnsubscribe(subscription.following.id)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Да, отписаться")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnsubscribeDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}