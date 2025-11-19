// PeopleScreen.kt - исправленная версия
package com.ark.socialevent.ui.screens.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.UserProfile
import com.ark.socialevent.network.UserRepository
import com.ark.socialevent.state.FriendshipStateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(userRepository: UserRepository) {
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Следим за глобальным триггером обновления
    val refreshTrigger by remember { mutableStateOf(FriendshipStateManager.refreshPeopleTrigger) }

    // Snackbar для уведомлений
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        userRepository.getAllProfiles { usersList, error ->
            isLoading = false
            if (error != null) {
                errorMessage = error
            } else {
                users = usersList ?: emptyList()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Люди",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage ?: "Ошибка загрузки",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            isLoading = true
                            errorMessage = null
                            FriendshipStateManager.refreshPeople() // Используем глобальный менеджер
                        }) {
                            Text("Повторить")
                        }
                    }
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Пользователи не найдены")
                }
            } else {
                LazyColumn {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            userRepository = userRepository,
                            onShowMessage = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: UserProfile,
    userRepository: UserRepository,
    onShowMessage: (String) -> Unit
) {
    var friendshipStatus by remember { mutableStateOf<String?>(null) }
    var isLoadingStatus by remember { mutableStateOf(true) }
    var showActionButtons by remember { mutableStateOf(false) }
    var isIncomingRequest by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Загружаем статус дружбы при создании карточки
    LaunchedEffect(user.id, FriendshipStateManager.refreshPeopleTrigger) { // Добавляем зависимость от триггера
        userRepository.getFriendshipStatus(user.id) { status, error ->
            isLoadingStatus = false
            if (error == null) {
                friendshipStatus = status

                // ОПРЕДЕЛЯЕМ ТИП ЗАЯВКИ: входящая или исходящая
                if (status == "pending") {
                    // Для определения типа заявки проверяем список входящих запросов
                    userRepository.getPendingRequests { requests, _ ->
                        isIncomingRequest = requests?.any { it.user.id == user.id } == true
                        showActionButtons = status != null && status != "none"
                    }
                } else {
                    showActionButtons = status != null && status != "none"
                }
            } else {
                friendshipStatus = "none"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Верхняя часть с информацией о пользователе
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Аватар",
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Возраст: ${user.age} • ${user.gender}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Нижняя часть с кнопками дружбы
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoadingStatus) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                FriendshipButtons(
                    user = user,
                    friendshipStatus = friendshipStatus,
                    isIncomingRequest = isIncomingRequest,
                    userRepository = userRepository,
                    onShowMessage = onShowMessage,
                    showActionButtons = showActionButtons,
                    onShowActionButtonsChange = { showActionButtons = it }
                )
            }
        }
    }
}

@Composable
fun FriendshipButtons(
    user: UserProfile,
    friendshipStatus: String?,
    isIncomingRequest: Boolean,
    userRepository: UserRepository,
    onShowMessage: (String) -> Unit,
    showActionButtons: Boolean,
    onShowActionButtonsChange: (Boolean) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Функция для обработки операций с друзьями
    fun handleFriendshipOperation(
        operation: (callback: (Boolean, String?) -> Unit) -> Unit,
        successMessage: String
    ) {
        coroutineScope.launch {
            isLoading = true
            operation { success, message ->
                isLoading = false
                if (success) {
                    onShowMessage(successMessage)
                    // ВЫЗЫВАЕМ ГЛОБАЛЬНОЕ ОБНОВЛЕНИЕ ВСЕХ ЭКРАНОВ
                    FriendshipStateManager.refreshAll()
                    onShowActionButtonsChange(false)
                } else {
                    onShowMessage(message ?: "Ошибка операции")
                }
            }
        }
    }

    // Основная кнопка/статус
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Текст статуса
        Text(
            text = when (friendshipStatus) {
                "none" -> "Не в друзьях"
                "pending" -> if (isIncomingRequest) "Прислал(а) заявку" else "Запрос отправлен"
                "accepted" -> "Друзья"
                else -> "Не в друзьях"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = when (friendshipStatus) {
                "accepted" -> MaterialTheme.colorScheme.primary
                "pending" -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // Кнопка действий
        when (friendshipStatus) {
            "none" -> {
                Button(
                    onClick = {
                        handleFriendshipOperation(
                            operation = { callback -> userRepository.sendFriendRequest(user.id, callback) },
                            successMessage = "Запрос в друзья отправлен"
                        )
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Добавить в друзья")
                    }
                }
            }
            "pending" -> {
                if (isIncomingRequest) {
                    // ВХОДЯЩАЯ заявка - показываем кнопку "Действия"
                    Button(
                        onClick = { onShowActionButtonsChange(!showActionButtons) },
                        enabled = !isLoading
                    ) {
                        Text("Действия")
                    }
                } else {
                    // ИСХОДЯЩАЯ заявка - показываем кнопку "Отменить"
                    OutlinedButton(
                        onClick = {
                            handleFriendshipOperation(
                                operation = { callback -> userRepository.rejectFriendRequest(user.id, callback) },
                                successMessage = "Заявка отменена"
                            )
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Отменить")
                        }
                    }
                }
            }
            "accepted" -> {
                // Для друзей показываем кнопку управления
                OutlinedButton(
                    onClick = { onShowActionButtonsChange(!showActionButtons) },
                    enabled = !isLoading
                ) {
                    Text("Управление")
                }
            }
        }
    }

    // Дополнительные кнопки действий (показываются по нажатию)
    if (showActionButtons) {
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            when (friendshipStatus) {
                "pending" -> {
                    // ТОЛЬКО ДЛЯ ВХОДЯЩИХ ЗАЯВОК - принять/отклонить
                    if (isIncomingRequest) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    handleFriendshipOperation(
                                        operation = { callback -> userRepository.acceptFriendRequest(user.id, callback) },
                                        successMessage = "Запрос в друзья принят"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Принять")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = {
                                    handleFriendshipOperation(
                                        operation = { callback -> userRepository.rejectFriendRequest(user.id, callback) },
                                        successMessage = "Запрос в друзья отклонен"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Отклонить")
                            }
                        }
                    }
                }
                "accepted" -> {
                    // Для друзей - удалить из друзей
                    OutlinedButton(
                        onClick = {
                            handleFriendshipOperation(
                                operation = { callback -> userRepository.removeFriend(user.id, callback) },
                                successMessage = "Пользователь удален из друзей"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить из друзей")
                    }
                }
            }
        }
    }
}