// com/ark/socialevent/ui/components/DatePickerDialog.kt
package com.ark.socialevent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.*

@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Date) -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Выберите дату",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Простой выбор даты (можно заменить на более продвинутый)
                // Здесь используем простой текстовый ввод для демонстрации
                // В реальном приложении лучше использовать DatePicker из accompanist
                OutlinedTextField(
                    value = "${selectedDate.get(Calendar.DAY_OF_MONTH)}.${selectedDate.get(Calendar.MONTH) + 1}.${selectedDate.get(Calendar.YEAR)}",
                    onValueChange = { },
                    label = { Text("Дата (ДД.ММ.ГГГГ)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Простые кнопки для навигации по датам
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedDate.add(Calendar.DAY_OF_MONTH, -1)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("−1 день")
                    }

                    OutlinedButton(
                        onClick = {
                            selectedDate.add(Calendar.DAY_OF_MONTH, 1)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1 день")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = { onDateSelected(selectedDate.time) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}