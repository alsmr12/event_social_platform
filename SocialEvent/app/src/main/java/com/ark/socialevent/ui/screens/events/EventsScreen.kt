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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(eventRepository: EventRepository) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    fun loadEvents() {
        isLoading = true
        eventRepository.getEvents { eventsList, error ->
            isLoading = false
            if (error != null) {
                errorMessage = error
            } else {
                events = eventsList ?: emptyList()
                errorMessage = null
            }
        }
    }

    LaunchedEffect(Unit) {
        loadEvents()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isLoading) {
                Row {

                    FloatingActionButton(
                        onClick = { showJoinByCodeDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = "Присоединиться по коду")
                    }
                    // Кнопка создания события
                    FloatingActionButton(
                        onClick = { showCreateEventDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Создать событие")
                    }
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
                "События",
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
                        Button(onClick = { loadEvents() }) {
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
                            "Событий пока нет",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                        EventCard(
                            event = event,
                            eventRepository = eventRepository,
                            onShowMessage = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                    loadEvents() // Перезагружаем события после действия
                                }
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
                loadEvents()
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

    if (showJoinByCodeDialog) {
        JoinEventByCodeDialog(
            eventRepository = eventRepository,
            onDismiss = { showJoinByCodeDialog = false },
            onJoinSuccess = {
                showJoinByCodeDialog = false
                loadEvents()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Вы присоединились к событию!")
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
fun EventCard(
    event: Event,
    eventRepository: EventRepository,
    onShowMessage: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок и тип
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
                if (event.isPrivate) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Приватное событие",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
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

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка подписки
            EventSubscriptionButton(
                event = event,
                isLoading = isLoading,
                onSubscribe = {
                    isLoading = true
                    eventRepository.subscribeToEvent(event.id) { success, message ->
                        isLoading = false
                        if (success) {
                            onShowMessage(message ?: "Подписка оформлена")
                        } else {
                            onShowMessage(message ?: "Ошибка подписки")
                        }
                    }
                },
                onUnsubscribe = {
                    isLoading = true
                    eventRepository.unsubscribeFromEvent(event.id) { success, message ->
                        isLoading = false
                        if (success) {
                            onShowMessage(message ?: "Подписка отменена")
                        } else {
                            onShowMessage(message ?: "Ошибка отписки")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun EventInfo(event: Event) {
    Column {
        // Дата и время
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = "Время",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatEventDate(event.dateTime),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Место
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Место",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = event.location,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Организатор и подписчики
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Организатор: ${event.creator.firstName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${event.subscribersCount} участников",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EventSubscriptionButton(
    event: Event,
    isLoading: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    if (event.isPast) {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        ) {
            Text("Событие завершено")
        }
    } else if (event.isSubscribed) {
        OutlinedButton(
            onClick = onUnsubscribe,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Отписаться")
            }
        }
    } else {
        Button(
            onClick = onSubscribe,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Участвовать")
            }
        }
    }
}

// Функция для форматирования даты события
fun formatEventDate(dateString: String): String {
    return try {
        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )

        var parsedDate: Date? = null
        for (format in formats) {
            try {
                parsedDate = format.parse(dateString)
                if (parsedDate != null) break
            } catch (e: Exception) {}
        }

        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        parsedDate?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}