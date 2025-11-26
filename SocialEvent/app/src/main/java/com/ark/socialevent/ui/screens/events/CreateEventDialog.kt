// com/ark/socialevent/ui/screens/events/CreateEventDialog.kt
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ark.socialevent.data.EventTypes
import com.ark.socialevent.network.EventRepository
import com.ark.socialevent.ui.components.DateTimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

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
    var selectedType by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var maxParticipants by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Состояния для выбора даты и времени
    var showDateTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

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

                // Название
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название события*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Описание
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Тип события (выпадающий список)
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = { },
                        label = { Text("Тип события*") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    )
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.matchParentSize()
                    ) {
                        Spacer(modifier = Modifier)
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EventTypes.eventTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(EventTypes.getDisplayName(type)) },
                            onClick = {
                                selectedType = type
                                expanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Дата и время
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDateTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (dateTime.isNotEmpty()) {
                                try {
                                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                                    val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                    val date = inputFormat.parse(dateTime)
                                    outputFormat.format(date!!)
                                } catch (e: Exception) {
                                    dateTime
                                }
                            } else {
                                "Выбрать дату и время"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Место проведения
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Место проведения*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Координаты
                Text(
                    "Координаты (необязательно):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                                latitude = it
                            }
                        },
                        label = { Text("Широта") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                                longitude = it
                            }
                        },
                        label = { Text("Долгота") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Максимальное количество участников
                OutlinedTextField(
                    value = maxParticipants,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                            maxParticipants = it
                        }
                    },
                    label = { Text("Макс. участников") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Приватное событие
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Приватное событие")
                        if (isPrivate) {
                            Text(
                                "Доступ только по приглашению",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
                            // Валидация
                            if (title.isBlank()) {
                                onShowMessage("Введите название события")
                                return@Button
                            }
                            if (description.isBlank()) {
                                onShowMessage("Введите описание события")
                                return@Button
                            }
                            if (selectedType.isBlank()) {
                                onShowMessage("Выберите тип события")
                                return@Button
                            }
                            if (dateTime.isBlank()) {
                                onShowMessage("Выберите дату и время")
                                return@Button
                            }
                            if (location.isBlank()) {
                                onShowMessage("Введите место проведения")
                                return@Button
                            }

                            isLoading = true
                            eventRepository.createEvent(
                                title = title,
                                description = description,
                                type = selectedType,
                                dateTime = dateTime,
                                location = location,
                                latitude = if (latitude.isNotBlank()) latitude else null,
                                longitude = if (longitude.isNotBlank()) longitude else null,
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
                        enabled = !isLoading &&
                                title.isNotBlank() &&
                                description.isNotBlank() &&
                                selectedType.isNotBlank() &&
                                dateTime.isNotBlank() &&
                                location.isNotBlank()
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
            }
        }
    }

    // Диалог выбора даты и времени
    if (showDateTimePicker) {
        DateTimePickerDialog(
            onDismiss = { showDateTimePicker = false },
            onDateTimeSelected = { dateTimeString ->
                dateTime = dateTimeString
                showDateTimePicker = false
            }
        )
    }
}