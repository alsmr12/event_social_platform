// com/ark/socialevent/ui/screens/events/MyEventsScreen.kt
package com.ark.socialevent.ui.screens.events

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
import com.ark.socialevent.network.Event
import com.ark.socialevent.network.EventRepository
import com.ark.socialevent.network.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(
    userRepository: UserRepository,
    eventRepository: EventRepository
) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<Int?>(null) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showEditEventDialog by remember { mutableStateOf(false) } // ← ДОБАВЬ ЭТО
    var selectedEventForEdit by remember { mutableStateOf<Event?>(null) } // ← ДОБАВЬ ЭТО

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun loadMyEvents(userId: Int) {
        isLoading = true
        eventRepository.getUserEvents(userId) { eventsList, error ->
            isLoading = false
            if (error != null) {
                errorMessage = error
            } else {
                events = eventsList ?: emptyList()
                errorMessage = null
            }
        }
    }

    // Получаем ID текущего пользователя
    LaunchedEffect(Unit) {
        userRepository.getProfile { profile ->
            currentUserId = profile?.id
            if (currentUserId != null) {
                loadMyEvents(currentUserId!!)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = { showCreateEventDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать событие")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Мои события",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Загрузка событий...")
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                errorMessage ?: "Ошибка загрузки",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            currentUserId?.let { loadMyEvents(it) }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить")
                        }
                    }
                }
            } else if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = "Нет событий",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "У вас пока нет событий",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Создайте первое событие!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(events) { event ->
                        MyEventCard(
                            event = event,
                            eventRepository = eventRepository,
                            onShowMessage = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                    currentUserId?.let { loadMyEvents(it) }
                                }
                            },
                            onEditEvent = {
                                selectedEventForEdit = event // ← ЗАПОМИНАЕМ СОБЫТИЕ
                                showEditEventDialog = true   // ← ПОКАЗЫВАЕМ ДИАЛОГ
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Диалог создания события
    if (showCreateEventDialog) {
        CreateEventDialog(
            eventRepository = eventRepository,
            onDismiss = { showCreateEventDialog = false },
            onEventCreated = {
                showCreateEventDialog = false
                currentUserId?.let { loadMyEvents(it) }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Событие создано!")
                }
            },
            onShowMessage = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    // Диалог редактирования события ← ДОБАВЬ ЭТОТ БЛОК
    if (showEditEventDialog && selectedEventForEdit != null) {
        EditEventDialog(
            event = selectedEventForEdit!!,
            eventRepository = eventRepository,
            onDismiss = {
                showEditEventDialog = false
                selectedEventForEdit = null
            },
            onEventUpdated = {
                showEditEventDialog = false
                selectedEventForEdit = null
                currentUserId?.let { loadMyEvents(it) }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Событие обновлено!")
                }
            },
            onShowMessage = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }
}

@Composable
fun MyEventCard(
    event: Event,
    eventRepository: EventRepository,
    onShowMessage: (String) -> Unit,
    onEditEvent: (Event) -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок и кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Иконки статуса
                Row {
                    if (event.isPrivate) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Приватное",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = { showActions = !showActions }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Действия")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Описание
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Информация о событии
            EventInfo(event = event)

            Spacer(modifier = Modifier.height(8.dp))

            // Статистика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${event.subscribersCount} участников",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (event.isPrivate && event.inviteCode != null) {
                    Text(
                        text = "Код: ${event.inviteCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Действия
            if (showActions) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditEvent(event) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Редактировать")
                    }

                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            eventRepository.deleteEvent(event.id) { success, message ->
                                isLoading = false
                                if (success) {
                                    onShowMessage("Событие удалено")
                                } else {
                                    onShowMessage(message ?: "Ошибка удаления")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }
}