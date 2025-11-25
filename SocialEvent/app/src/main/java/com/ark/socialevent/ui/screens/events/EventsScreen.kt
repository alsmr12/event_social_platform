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
import androidx.compose.foundation.clickable
import kotlinx.coroutines.coroutineScope

// Перечисление для типов вкладок
enum class EventsTab {
    UPCOMING, PAST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(eventRepository: EventRepository) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(EventsTab.UPCOMING) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    // Параметры фильтрации
    var selectedType by remember { mutableStateOf("all") }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("0") }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun loadEvents() {
        isLoading = true
        // Здесь можно добавить параметры фильтрации когда API будет поддерживать
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

    LaunchedEffect(selectedTab) {
        loadEvents()
    }

    // Функция для получения текущего списка событий в зависимости от вкладки
    fun getCurrentEvents(): List<Event> {
        val now = System.currentTimeMillis()
        return when (selectedTab) {
            EventsTab.UPCOMING -> events.filter {
                try {
                    val eventTime = parseDate(it.dateTime)?.time ?: 0
                    eventTime > now
                } catch (e: Exception) {
                    false
                }
            }
            EventsTab.PAST -> events.filter {
                try {
                    val eventTime = parseDate(it.dateTime)?.time ?: 0
                    eventTime <= now
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    // Функция для получения заголовка в зависимости от вкладки
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(getScreenTitle()) },
                actions = {
                    // Кнопка фильтров
                    IconButton(
                        onClick = { showFiltersDialog = true },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
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
            EventsTabs(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое
            EventsContent(
                events = getCurrentEvents(),
                isLoading = isLoading,
                errorMessage = errorMessage,
                emptyMessage = getEmptyMessage(),
                eventRepository = eventRepository,
                onRetry = { loadEvents() },
                onShowMessage = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                        loadEvents()
                    }
                }, onEventClick = { event ->  selectedEvent = event
                }
            )
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

    // Диалог присоединения по коду
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

    // Диалог фильтров
    if (showFiltersDialog) {
        FiltersDialog(
            selectedType = selectedType,
            dateFrom = dateFrom,
            dateTo = dateTo,
            radius = radius,
            onTypeChanged = { selectedType = it },
            onDateFromChanged = { dateFrom = it },
            onDateToChanged = { dateTo = it },
            onRadiusChanged = { radius = it },
            onApplyFilters = {
                showFiltersDialog = false
                loadEvents()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Фильтры применены")
                }
            },
            onClearFilters = {
                selectedType = "all"
                dateFrom = ""
                dateTo = ""
                radius = "0"
                showFiltersDialog = false
                loadEvents()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Фильтры сброшены")
                }
            },
            onDismiss = { showFiltersDialog = false }
        )
    }

    if (selectedEvent != null) {
        EventDetailsDialog(
            event = selectedEvent!!,
            eventRepository = eventRepository,
            onDismiss = { selectedEvent = null },
            onShowMessage = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                    loadEvents() // Перезагружаем события после действия
                    selectedEvent = null // Закрываем диалог
                }
            }
        )
    }

}

@Composable
fun EventsTabs(
    selectedTab: EventsTab,
    onTabSelected: (EventsTab) -> Unit
) {
    TabRow(selectedTabIndex = selectedTab.ordinal) {
        Tab(
            text = { Text("Предстоящие") },
            selected = selectedTab == EventsTab.UPCOMING,
            onClick = { onTabSelected(EventsTab.UPCOMING) }
        )
        Tab(
            text = { Text("Прошедшие") },
            selected = selectedTab == EventsTab.PAST,
            onClick = { onTabSelected(EventsTab.PAST) }
        )
    }
}

@Composable
fun EventsContent(
    events: List<Event>,
    isLoading: Boolean,
    errorMessage: String?,
    emptyMessage: String,
    eventRepository: EventRepository,
    onRetry: () -> Unit,
    onShowMessage: (String) -> Unit,
    onEventClick: (Event) -> Unit  // Добавь этот параметр
) {
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
                Button(onClick = onRetry) {
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
                    emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            items(events) { event ->
                EventCard(
                    event = event,
                    eventRepository = eventRepository,
                    onShowMessage = onShowMessage,
                    onEventClick = { onEventClick(event) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FiltersDialog(
    selectedType: String,
    dateFrom: String,
    dateTo: String,
    radius: String,
    onTypeChanged: (String) -> Unit,
    onDateFromChanged: (String) -> Unit,
    onDateToChanged: (String) -> Unit,
    onRadiusChanged: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтры событий") },
        text = {
            Column {
                // Тип события
                Text("Тип события:", style = MaterialTheme.typography.bodyMedium)
                DropdownMenuBox(
                    selectedValue = selectedType,
                    onValueChange = onTypeChanged,
                    options = listOf("all" to "Все типы", "sport" to "Спорт", "culture" to "Культура", "education" to "Образование")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Дата от
                Text("Дата от:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = dateFrom,
                    onValueChange = onDateFromChanged,
                    placeholder = { Text("ГГГГ-ММ-ДД") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Дата до
                Text("Дата до:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = dateTo,
                    onValueChange = onDateToChanged,
                    placeholder = { Text("ГГГГ-ММ-ДД") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Радиус
                Text("Радиус поиска (км):", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = radius,
                    onValueChange = onRadiusChanged,
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row {
                OutlinedButton(
                    onClick = onClearFilters,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сбросить")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApplyFilters,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Применить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DropdownMenuBox(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    options: List<Pair<String, String>>
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(options.find { it.first == selectedValue }?.second ?: "Выберите тип")
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    eventRepository: EventRepository,
    onShowMessage: (String) -> Unit,
    onEventClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEventClick() },
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

fun parseDate(dateString: String): Date? {
    return try {
        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        for (format in formats) {
            try {
                return format.parse(dateString)
            } catch (e: Exception) {
                continue
            }
        }
        null
    } catch (e: Exception) {
        null
    }


}