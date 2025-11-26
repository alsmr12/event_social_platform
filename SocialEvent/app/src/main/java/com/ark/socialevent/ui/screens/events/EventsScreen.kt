package com.ark.socialevent.ui.screens.events

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
import com.ark.socialevent.network.Event
import com.ark.socialevent.network.EventRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Перечисление для типов вкладок
enum class EventsTab {
    UPCOMING, PAST
}

// Модель для фильтров
data class EventFilters(
    val type: String = "all",
    val dateFrom: String = "",
    val dateTo: String = "",
    val radius: String = "0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(eventRepository: EventRepository) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(EventsTab.UPCOMING) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // Текущие активные фильтры
    var currentFilters by remember { mutableStateOf(EventFilters()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Функция загрузки событий
    fun loadEvents() {
        isLoading = true
        errorMessage = null

        // Преобразуем даты в формат для сервера (YYYY-MM-DD)
        val serverDateFrom = if (currentFilters.dateFrom.isNotEmpty()) {
            convertDateToServerFormat(currentFilters.dateFrom)
        } else null

        val serverDateTo = if (currentFilters.dateTo.isNotEmpty()) {
            convertDateToServerFormat(currentFilters.dateTo)
        } else null

        eventRepository.getEventsWithFilters(
            type = if (currentFilters.type != "all") currentFilters.type else null,
            dateFrom = serverDateFrom,
            dateTo = serverDateTo,
            radius = if (currentFilters.radius.isNotEmpty() && currentFilters.radius != "0")
                currentFilters.radius.toDoubleOrNull() else null,
            timeFilter = if (selectedTab == EventsTab.PAST) "past" else "upcoming"
        ) { eventsList, error ->
            isLoading = false
            if (error != null) {
                errorMessage = error
                events = emptyList()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Ошибка загрузки: $error")
                }
            } else {
                events = eventsList ?: emptyList()
            }
        }
    }

    // Загружаем события при первом запуске
    LaunchedEffect(Unit) {
        loadEvents()
    }

    // Перезагружаем при смене вкладки
    LaunchedEffect(selectedTab) {
        loadEvents()
    }

    // Функция для получения заголовка
    fun getScreenTitle(): String {
        return when (selectedTab) {
            EventsTab.UPCOMING -> "Предстоящие события"
            EventsTab.PAST -> "Прошедшие события"
        }
    }

    // Функция для получения сообщения при пустом списке
    fun getEmptyMessage(): String {
        return when (selectedTab) {
            EventsTab.UPCOMING -> "Предстоящих событий пока нет"
            EventsTab.PAST -> "Прошедших событий пока нет"
        }
    }

    // Функция для проверки активных фильтров
    fun hasActiveFilters(): Boolean {
        return currentFilters.type != "all" ||
                currentFilters.dateFrom.isNotEmpty() ||
                currentFilters.dateTo.isNotEmpty() ||
                (currentFilters.radius.isNotEmpty() && currentFilters.radius != "0")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(getScreenTitle())
                        // Показываем активные фильтры
                        if (hasActiveFilters()) {
                            Text(
                                "Фильтры активны",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Кнопка обновления
                    IconButton(
                        onClick = { loadEvents() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить"
                        )
                    }
                    // Кнопка фильтров
                    IconButton(
                        onClick = { showFiltersDialog = true },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Фильтры",
                            tint = if (hasActiveFilters())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                Row {
                    FloatingActionButton(
                        onClick = { showJoinByCodeDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = "Присоединиться по коду")
                    }
                    // Кнопка создания события (только для предстоящих событий)
                    if (selectedTab == EventsTab.UPCOMING) {
                        FloatingActionButton(
                            onClick = { showCreateEventDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Создать событие")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Вкладки
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    text = { Text("Предстоящие") },
                    selected = selectedTab == EventsTab.UPCOMING,
                    onClick = { selectedTab = EventsTab.UPCOMING }
                )
                Tab(
                    text = { Text("Прошедшие") },
                    selected = selectedTab == EventsTab.PAST,
                    onClick = { selectedTab = EventsTab.PAST }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое
            EventsContent(
                events = events,
                isLoading = isLoading,
                errorMessage = errorMessage,
                emptyMessage = getEmptyMessage(),
                onRetry = { loadEvents() },
                onEventClick = { selectedEvent = it }
            )
        }
    }

    // Диалог создания события - используем твой готовый CreateEventDialog
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

    // Диалог присоединения по коду - используем твой готовый JoinEventByCodeDialog
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

    // Диалог фильтров - используем твой готовый FiltersDialog
    if (showFiltersDialog) {
        FiltersDialog(
            selectedType = currentFilters.type,
            dateFrom = currentFilters.dateFrom,
            dateTo = currentFilters.dateTo,
            radius = currentFilters.radius,
            onTypeChanged = { currentFilters = currentFilters.copy(type = it) },
            onDateFromChanged = { currentFilters = currentFilters.copy(dateFrom = it) },
            onDateToChanged = { currentFilters = currentFilters.copy(dateTo = it) },
            onRadiusChanged = { currentFilters = currentFilters.copy(radius = it) },
            onApplyFilters = {
                showFiltersDialog = false
                loadEvents()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Фильтры применены")
                }
            },
            onClearFilters = {
                currentFilters = EventFilters()
                showFiltersDialog = false
                loadEvents()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Фильтры сброшены")
                }
            },
            onDismiss = { showFiltersDialog = false }
        )
    }

    // Диалог деталей события - используем твой готовый EventDetailsDialog
    if (selectedEvent != null) {
        EventDetailsDialog(
            event = selectedEvent!!,
            eventRepository = eventRepository,
            onDismiss = { selectedEvent = null },
            onShowMessage = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                    loadEvents()
                    selectedEvent = null
                }
            }
        )
    }
}

@Composable
fun EventsContent(
    events: List<Event>,
    isLoading: Boolean,
    errorMessage: String?,
    emptyMessage: String,
    onRetry: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    when {
        isLoading -> {
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
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Повторить")
                    }
                }
            }
        }
        events.isEmpty() -> {
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
                        emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onEventClick = { onEventClick(event) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onEventClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEventClick() },
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
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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

// Функция для конвертации даты в формат сервера (DD.MM.YYYY -> YYYY-MM-DD)
fun convertDateToServerFormat(date: String): String? {
    return try {
        val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        parsedDate?.let { outputFormat.format(it) }
    } catch (e: Exception) {
        null
    }
}