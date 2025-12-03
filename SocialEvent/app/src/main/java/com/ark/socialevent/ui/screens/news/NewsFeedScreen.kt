// NewsFeedScreen.kt
package com.ark.socialevent.ui.screens.news

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
import com.ark.socialevent.network.EventRepository
import com.ark.socialevent.network.NewsFeedItem
import com.ark.socialevent.network.UserRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NewsFeedScreen(
    userRepository: UserRepository,
    eventRepository: EventRepository,
    onNavigateToPeople: () -> Unit,
    onOpenProfile: (userId: Int) -> Unit,
    onOpenEvent: (event: com.ark.socialevent.network.Event) -> Unit
) {
    var newsFeed by remember { mutableStateOf<List<NewsFeedItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadingEventId by remember { mutableStateOf<Int?>(null) }

    fun loadNewsFeed() {
        loading = true
        userRepository.getNewsFeed { items, error ->
            newsFeed = items ?: emptyList()
            loading = false
            errorMessage = error
        }
    }


    val loadFullEvent = { newsEvent: com.ark.socialevent.network.NewsEvent ->
        loadingEventId = newsEvent.id
        eventRepository.getEventById(newsEvent.id) { event, error ->
            loadingEventId = null
            if (event != null) {
                onOpenEvent(event)
            } else {
                errorMessage = "Не удалось загрузить событие: $error"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNewsFeed()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Заголовок и кнопка обновления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Новости",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(
                onClick = { loadNewsFeed() },
                enabled = !loading
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Обновить",
                    tint = if (loading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Загрузка новостей...")
                    }
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = errorMessage ?: "Ошибка",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { loadNewsFeed() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Повторить")
                    }
                }
            }
            newsFeed.isEmpty() -> {
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
                        "В ленте пока пусто",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Подпишитесь на людей, чтобы видеть их записи",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToPeople
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Найти людей")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(newsFeed) { item ->
                        val isEvent = item.type == "event"
                        val isEventLoading = isEvent && loadingEventId == item.event?.id

                        NewsFeedItem(
                            item = item,
                            isEvent = isEvent,
                            isEventLoading = isEventLoading,
                            onAuthorClick = {
                                onOpenProfile(item.author.id)
                            },
                            onEventClick = {
                                // Кликабельно только для событий и не во время загрузки
                                if (isEvent && !isEventLoading) {
                                    item.event?.let { newsEvent ->
                                        loadFullEvent(newsEvent)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NewsFeedItem(
    item: NewsFeedItem,
    isEvent: Boolean = false,
    isEventLoading: Boolean = false,
    onAuthorClick: () -> Unit,
    onEventClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isEvent && !isEventLoading,
                onClick = onEventClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок с автором - только имя кликабельное
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Автор",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Только имя автора кликабельно
                    Text(
                        text = "${item.author.firstName} ${item.author.lastName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onAuthorClick() }
                    )
                    Text(
                        text = formatDate(item.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Иконка типа контента (БЕЗ индикатора загрузки)
                Icon(
                    imageVector = when (item.type) {
                        "event" -> Icons.Default.Event
                        else -> Icons.Default.Edit
                    },
                    contentDescription = when (item.type) {
                        "event" -> "Событие"
                        else -> "Запись"
                    },
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Содержимое
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            // Дополнительная информация для событий
            if (isEvent) {
                item.event?.let { newsEvent ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎯 ${newsEvent.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Место",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = newsEvent.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Время",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(newsEvent.dateTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Подсказка для клика (только для событий)
                    if (isEventLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Загрузка события...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите для просмотра события →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Дополнительная информация для постов (не кликабельно)
            if (item.type == "post") {
                item.post?.let { post ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📝 Запись на стене",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Функция для форматирования даты
fun formatDate(dateString: String): String {
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
            } catch (e: Exception) {
                // Пробуем следующий формат
            }
        }

        val outputFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
        if (parsedDate != null) {
            outputFormat.format(parsedDate)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}