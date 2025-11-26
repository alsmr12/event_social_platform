package com.ark.socialevent.ui.screens.events

import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventDetailsDialog(
    event: Event,
    eventRepository: EventRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (event.isPrivate) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Приватное событие",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Полное описание
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                Divider()

                // Детальная информация
                EventDetailedInfo(event = event)

                Divider()

                // Статистика
                EventStats(event = event)

                // Приватная информация (если событие приватное)
                if (event.isPrivate) {
                    Divider()
                    PrivateEventInfo(event = event)
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть")
                }
            }
        }
    )
}

@Composable
fun EventDetailedInfo(event: Event) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Дата и время
        InfoRow(
            icon = Icons.Default.Schedule,
            title = "Дата и время",
            value = formatEventDate(event.dateTime)
        )

        // Место
        InfoRow(
            icon = Icons.Default.LocationOn,
            title = "Место проведения",
            value = event.location
        )

        // Координаты (если есть)
        if (event.latitude != null && event.longitude != null) {
            InfoRow(
                icon = Icons.Default.Place,
                title = "Координаты",
                value = String.format("%.6f, %.6f", event.latitude, event.longitude)
            )
        }

        // Тип события
        InfoRow(
            icon = Icons.Default.Category,
            title = "Тип события",
            value = event.type
        )

        // Организатор
        InfoRow(
            icon = Icons.Default.Person,
            title = "Организатор",
            value = "${event.creator.firstName} ${event.creator.lastName}"
        )

        // Максимальное количество участников
        event.maxParticipants?.let { maxParticipants ->
            if (maxParticipants > 0) {
                InfoRow(
                    icon = Icons.Default.Group,
                    title = "Максимум участников",
                    value = "$maxParticipants человек"
                )
            }
        }

        // Дата создания
        InfoRow(
            icon = Icons.Default.CalendarToday,
            title = "Создано",
            value = formatEventDate(event.createdAt)
        )
    }
}

@Composable
fun PrivateEventInfo(event: Event) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Приватная информация",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // Код приглашения
        if (!event.inviteCode.isNullOrEmpty()) {
            InfoRow(
                icon = Icons.Default.VpnKey,
                title = "Код приглашения",
                value = event.inviteCode
            )
        }

        // Приватный ключ (если есть)
        if (!event.privateKey.isNullOrEmpty()) {
            InfoRow(
                icon = Icons.Default.Security,
                title = "Приватный ключ",
                value = event.privateKey
            )
        }

        Text(
            text = "Это приватное событие. Доступ только по приглашению.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EventStats(event: Event) {
    Column {
        Text(
            text = "Статистика",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                value = event.subscribersCount.toString(),
                label = "участников"
            )

            StatItem(
                value = if (event.isPrivate) "Приватное" else "Публичное",
                label = "тип события"
            )

            StatItem(
                value = if (event.isPast) "Завершено" else "Активно",
                label = "статус",
                valueColor = if (event.isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

