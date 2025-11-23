// com/ark/socialevent/ui/components/DateTimePickerDialog.kt
package com.ark.socialevent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialDateTime: String = "",
    onDismiss: () -> Unit,
    onDateTimeSelected: (String) -> Unit
) {
    val calendar = remember {
        if (initialDateTime.isNotEmpty()) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val date = format.parse(initialDateTime)
                Calendar.getInstance().apply { time = date!! }
            } catch (e: Exception) {
                Calendar.getInstance()
            }
        } else {
            Calendar.getInstance()
        }
    }

    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    // Списки доступных значений
    val years = (2020..2030).toList()
    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val daysInMonth = getDaysInMonth(selectedMonth, selectedYear)
    val hours = (0..23).toList()
    val minutes = listOf(0, 15, 30, 45)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Календарь",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Выберите дату и время",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Выбор даты
                Text(
                    "Дата",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Год
                Text(
                    "Год",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(years.size) { index ->
                        val year = years[index]
                        val isSelected = year == selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedYear = year }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                year.toString(),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Месяц
                Text(
                    "Месяц",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(months.size) { index ->
                        val monthNumber = index + 1
                        val isSelected = monthNumber == selectedMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedMonth = monthNumber
                                    // Обновляем количество дней в месяце
                                    val newDaysInMonth = getDaysInMonth(monthNumber, selectedYear)
                                    if (selectedDay > newDaysInMonth) {
                                        selectedDay = newDaysInMonth
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                months[index],
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // День
                Text(
                    "День",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(daysInMonth) { index ->
                        val day = index + 1
                        val isSelected = day == selectedDay
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedDay = day }
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                day.toString(),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Выбор времени
                Text(
                    "Время",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Часы и минуты в одной строке
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Часы
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Часы",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(hours.size) { index ->
                                val hour = hours[index]
                                val isSelected = hour == selectedHour
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedHour = hour }
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(
                                        String.format("%02d", hour),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Минуты
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Минуты",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(minutes.size) { index ->
                                val minute = minutes[index]
                                val isSelected = minute == selectedMinute
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedMinute = minute }
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(
                                        String.format("%02d", minute),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Быстрый выбор времени
                Text(
                    "Популярное время",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val popularTimes = listOf(
                    "09:00" to "Утро",
                    "12:00" to "Обед",
                    "15:00" to "День",
                    "18:00" to "Вечер",
                    "20:00" to "Ночь"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(popularTimes.size) { index ->
                        val (time, label) = popularTimes[index]
                        val parts = time.split(":")
                        val hour = parts[0].toInt()
                        val minute = parts[1].toInt()

                        val isSelected = hour == selectedHour && minute == selectedMinute

                        OutlinedButton(
                            onClick = {
                                selectedHour = hour
                                selectedMinute = minute
                            },
                            modifier = Modifier,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            ),
                            border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
                        ) {
                            Column {
                                Text(time, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(label, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Выбранная дата и время
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            buildSelectedDateTimeString(selectedDay, selectedMonth, selectedYear, selectedHour, selectedMinute),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            val dateTimeString = String.format(
                                "%04d-%02d-%02dT%02d:%02d",
                                selectedYear,
                                selectedMonth,
                                selectedDay,
                                selectedHour,
                                selectedMinute
                            )
                            onDateTimeSelected(dateTimeString)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}

private fun getDaysInMonth(month: Int, year: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun buildSelectedDateTimeString(day: Int, month: Int, year: Int, hour: Int, minute: Int): String {
    val monthNames = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )
    return "$day ${monthNames[month - 1]} $year, ${String.format("%02d:%02d", hour, minute)}"
}