package com.ark.socialevent.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
    val dateFromDialogState = rememberMaterialDialogState()
    val dateToDialogState = rememberMaterialDialogState()
    var selectedDateFrom by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDateTo by remember { mutableStateOf<LocalDate?>(null) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Фильтры событий",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 500.dp)
            ) {
                // Тип события
                Text(
                    text = "Тип события",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DropdownMenuBox(
                    selectedValue = selectedType,
                    onValueChange = onTypeChanged,
                    options = listOf(
                        "all" to "Все типы",
                        "concert" to "Концерт",
                        "sport" to "Спорт",
                        "lecture" to "Лекция",
                        "meeting" to "Встреча",
                        "party" to "Вечеринка",
                        "conference" to "Конференция",
                        "exhibition" to "Выставка",
                        "other" to "Другое"
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Даты
                Text(
                    text = "Период событий",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Дата от
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "С",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DatePickerField(
                            value = dateFrom,
                            onValueChange = onDateFromChanged,
                            dialogState = dateFromDialogState,
                            selectedDate = selectedDateFrom,
                            onDateSelected = { selectedDateFrom = it },
                            placeholder = "дд.мм.гггг"
                        )
                    }

                    // Дата до
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "По",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DatePickerField(
                            value = dateTo,
                            onValueChange = onDateToChanged,
                            dialogState = dateToDialogState,
                            selectedDate = selectedDateTo,
                            onDateSelected = { selectedDateTo = it },
                            placeholder = "дд.мм.гггг"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Радиус поиска
                Text(
                    text = "Радиус поиска",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                RadiusSelector(
                    radius = radius,
                    onRadiusChanged = onRadiusChanged
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onClearFilters,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сбросить")
                }
                Button(
                    onClick = onApplyFilters
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Применить")
                }
            }
        }
    )

    // Диалоги выбора даты
    DatePickerDialog(
        dialogState = dateFromDialogState,
        selectedDate = selectedDateFrom,
        onDateSelected = { date ->
            selectedDateFrom = date
            onDateFromChanged(date.format(dateFormatter))
        }
    )

    DatePickerDialog(
        dialogState = dateToDialogState,
        selectedDate = selectedDateTo,
        onDateSelected = { date ->
            selectedDateTo = date
            onDateToChanged(date.format(dateFormatter))
        }
    )
}

@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    dialogState: com.vanpra.composematerialdialogs.MaterialDialogState,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    placeholder: String
) {
    OutlinedButton(
        onClick = { dialogState.show() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isNotEmpty()) value else placeholder,
                color = if (value.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                Icons.Default.Event,
                contentDescription = "Выбрать дату",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DatePickerDialog(
    dialogState: com.vanpra.composematerialdialogs.MaterialDialogState,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    com.vanpra.composematerialdialogs.MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Выбрать")
            negativeButton("Отмена")
        }
    ) {
        datepicker(
            initialDate = selectedDate ?: LocalDate.now(),
            title = "Выберите дату",
            onDateChange = onDateSelected
        )
    }
}

@Composable
fun RadiusSelector(
    radius: String,
    onRadiusChanged: (String) -> Unit
) {
    Column {
        // Поле для ручного ввода
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Расстояние в км:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = radius,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                        onRadiusChanged(newValue)
                    }
                },
                placeholder = { Text("0") },
                modifier = Modifier.width(120.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                enabled = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Подсказка
        Text(
            text = "• 0 км = поиск по всему городу\n• Укажите нужное расстояние в километрах",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    options: List<Pair<String, String>>
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedValue }?.second ?: "Выберите тип"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = MaterialTheme.shapes.small,
            enabled = true
        )

        ExposedDropdownMenu(
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