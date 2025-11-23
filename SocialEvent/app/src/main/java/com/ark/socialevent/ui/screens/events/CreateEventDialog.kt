package com.ark.socialevent.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ark.socialevent.network.EventRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    eventRepository: EventRepository,
    onDismiss: () -> Unit,
    onEventCreated: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var maxParticipants by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Создать событие",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Поля формы
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название события") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Тип события") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    label = { Text("Дата и время (ГГГГ-ММ-ДДTЧЧ:ММ)") },
                    placeholder = { Text("2024-01-15T19:30") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Место проведения") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = maxParticipants,
                    onValueChange = { maxParticipants = it },
                    label = { Text("Макс. участников (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Приватное событие")
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank() || description.isBlank() ||
                                type.isBlank() || dateTime.isBlank() || location.isBlank()) {
                                onShowMessage("Заполните все обязательные поля")
                                return@Button
                            }

                            // Проверяем формат даты
                            if (!isValidDateTimeFormat(dateTime)) {
                                onShowMessage("Неверный формат даты. Используйте: ГГГГ-ММ-ДДTЧЧ:ММ")
                                return@Button
                            }

                            isLoading = true
                            eventRepository.createEvent(
                                title = title,
                                description = description,
                                type = type,
                                dateTime = dateTime,
                                location = location,
                                isPrivate = isPrivate,
                                maxParticipants = maxParticipants.toIntOrNull(),
                                callback = { success, message, event ->
                                    isLoading = false
                                    if (success) {
                                        onEventCreated()
                                    } else {
                                        onShowMessage(message ?: "Ошибка создания события")
                                    }
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && title.isNotBlank() &&
                                description.isNotBlank() && type.isNotBlank() &&
                                dateTime.isNotBlank() && location.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Создать")
                        }
                    }
                }

                // Подсказка по формату даты
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Формат даты: ГГГГ-ММ-ДДTЧЧ:ММ (например: 2024-01-15T19:30)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Функция для проверки формата даты
private fun isValidDateTimeFormat(dateTime: String): Boolean {
    return try {
        // Простая проверка формата ГГГГ-ММ-ДДTЧЧ:ММ
        val pattern = """^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$""".toRegex()
        pattern.matches(dateTime)
    } catch (e: Exception) {
        false
    }
}