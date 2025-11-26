// com/ark/socialevent/ui/components/DateTimePickerDialog.kt
package com.ark.socialevent.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DateTimePickerDialog(
    initialDateTime: String = "",
    onDismiss: () -> Unit,
    onDateTimeSelected: (String) -> Unit
) {
    val calendar = remember { parseInitialDate(initialDateTime) }
    var step by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header with step indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step indicator
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepIndicator(step = 1, currentStep = step + 1, label = "Дата")
                        Spacer(modifier = Modifier.width(8.dp))
                        StepIndicator(step = 2, currentStep = step + 1, label = "Время")
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Content
                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f)
                ) { currentStep ->
                    when (currentStep) {
                        0 -> EnhancedDatePicker(calendar = calendar)
                        1 -> EnhancedTimePicker(calendar = calendar)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Selected preview
                SelectedDateTimePreview(calendar = calendar)

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (step == 0) onDismiss() else step--
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (step == 0) "Отмена" else "Назад")
                    }

                    Button(
                        onClick = {
                            if (step == 0) {
                                step = 1
                            } else {
                                onDateTimeSelected(formatDateTimeForStorage(calendar))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(if (step == 0) "Продолжить" else "Подтвердить")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, currentStep: Int, label: String) {
    val isActive = step == currentStep
    val isCompleted = step < currentStep

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .wrapContentSize(Alignment.Center)
        ) {
            Text(
                text = if (isCompleted) "✓" else step.toString(),
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                    isActive -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = if (isCompleted) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isActive || isCompleted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedDatePicker(calendar: Calendar) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dateState = rememberDatePickerState(
        yearRange = currentYear..(currentYear + 20), // Увеличил диапазон до 20 лет
        initialSelectedDateMillis = calendar.timeInMillis,
        initialDisplayedMonthMillis = calendar.timeInMillis
    )

    LaunchedEffect(dateState.selectedDateMillis) {
        dateState.selectedDateMillis?.let { millis ->
            calendar.timeInMillis = millis
        }
    }

    DatePicker(
        state = dateState,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp),
        title = null,
        headline = null,
        showModeToggle = true,
        colors = DatePickerDefaults.colors(
            containerColor = Color.Transparent,
            todayDateBorderColor = MaterialTheme.colorScheme.primary,
            selectedDayContainerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun EnhancedTimePicker(calendar: Calendar) {
    var hour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    LaunchedEffect(hour, minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time spinner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeSpinnerUnit(
                value = hour,
                range = 0..23,
                onValueChange = { hour = it },
                label = "часы",
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            TimeSpinnerUnit(
                value = minute,
                range = 0..59,
                onValueChange = { minute = it },
                label = "минуты",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick time presets
        Text(
            text = "Быстрый выбор",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        val timePresets = listOf(
            Triple(9, 0, "Утро"),
            Triple(12, 0, "Обед"),
            Triple(15, 0, "День"),
            Triple(18, 0, "Вечер"),
            Triple(21, 0, "Ночь")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timePresets.forEach { (h, m, label) ->
                PresetTimeChip(
                    hour = h,
                    minute = m,
                    label = label,
                    isSelected = hour == h && minute == m,
                    onClick = {
                        hour = h
                        minute = m
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Manual input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ManualTimeInput(
                value = hour,
                onValueChange = { hour = it },
                label = "Часы",
                range = 0..23,
                modifier = Modifier.weight(1f)
            )

            ManualTimeInput(
                value = minute,
                onValueChange = { minute = it },
                label = "Минуты",
                range = 0..59,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeSpinnerUnit(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Up button
        IconButton(
            onClick = {
                val newValue = if (value < range.last) value + 1 else range.first
                onValueChange(newValue)
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Увеличить",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Value display
        Card(
            modifier = Modifier
                .width(80.dp)
                .height(60.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(value),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Down button
        IconButton(
            onClick = {
                val newValue = if (value > range.first) value - 1 else range.last
                onValueChange(newValue)
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Уменьшить",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PresetTimeChip(
    hour: Int,
    minute: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .height(60.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%02d:%02d".format(hour, minute),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ManualTimeInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = "%02d".format(value),
            onValueChange = { text ->
                text.toIntOrNull()?.takeIf { it in range }?.let { onValueChange(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Composable
private fun SelectedDateTimePreview(calendar: Calendar) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDateTimeForDisplay(calendar),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun parseInitialDate(input: String): Calendar {
    return if (input.isBlank()) {
        Calendar.getInstance()
    } else {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            Calendar.getInstance().apply { time = sdf.parse(input)!! }
        } catch (e: Exception) {
            Calendar.getInstance()
        }
    }
}

private fun formatDateTimeForDisplay(calendar: Calendar): String {
    val dateFormat = SimpleDateFormat("d MMMM yyyy', 'HH:mm", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

private fun formatDateTimeForStorage(calendar: Calendar): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
    return dateFormat.format(calendar.time)
}