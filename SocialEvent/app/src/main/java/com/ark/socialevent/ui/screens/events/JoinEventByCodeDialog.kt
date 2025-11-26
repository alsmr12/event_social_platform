package com.ark.socialevent.ui.screens.events

import androidx.compose.foundation.layout.*
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
fun JoinEventByCodeDialog(
    eventRepository: EventRepository,
    onDismiss: () -> Unit,
    onJoinSuccess: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Присоединиться к событию",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Введите код приглашения от организатора события",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Код приглашения") },
                    placeholder = { Text("ABCD1234") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            if (code.isBlank()) {
                                onShowMessage("Введите код приглашения")
                                return@Button
                            }

                            isLoading = true
                            eventRepository.joinEventByCode(code) { success, message, event ->
                                isLoading = false
                                if (success) {
                                    onJoinSuccess()
                                } else {
                                    onShowMessage(message ?: "Ошибка присоединения")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && code.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Присоединиться")
                        }
                    }
                }
            }
        }
    }
}