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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(userRepository: UserRepository) {
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

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
                            refreshTrigger++
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
                            onFriendshipChanged = { refreshTrigger++ },
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
    onFriendshipChanged: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var friendshipStatus by remember { mutableStateOf<String?>(null) }
    var isLoadingStatus by remember { mutableStateOf(true) }
    var showActionButtons by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Загружаем статус дружбы при создании карточки
    LaunchedEffect(user.id) {
        userRepository.getFriendshipStatus(user.id) { status, error ->
            isLoadingStatus = false
            if (error == null) {
                friendshipStatus = status
                showActionButtons = status != null && status != "none"
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
                    userRepository = userRepository,
                    onFriendshipChanged = onFriendshipChanged,
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
    userRepository: UserRepository,
    onFriendshipChanged: () -> Unit,
    onShowMessage: (String) -> Unit,
    showActionButtons: Boolean,
    onShowActionButtonsChange: (Boolean) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                "pending" -> "Запрос отправлен"
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
        if (friendshipStatus != "accepted") {
            Button(
                onClick = {
                    when (friendshipStatus) {
                        "none" -> {
                            coroutineScope.launch {
                                isLoading = true
                                userRepository.sendFriendRequest(user.id) { success, message ->
                                    isLoading = false
                                    if (success) {
                                        onShowMessage("Запрос в друзья отправлен")
                                        onFriendshipChanged()
                                    } else {
                                        onShowMessage(message ?: "Ошибка отправки запроса")
                                    }
                                }
                            }
                        }
                        "pending" -> {
                            onShowActionButtonsChange(!showActionButtons)
                        }
                        else -> {
                            coroutineScope.launch {
                                isLoading = true
                                userRepository.sendFriendRequest(user.id) { success, message ->
                                    isLoading = false
                                    if (success) {
                                        onShowMessage("Запрос в друзья отправлен")
                                        onFriendshipChanged()
                                    } else {
                                        onShowMessage(message ?: "Ошибка отправки запроса")
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        when (friendshipStatus) {
                            "none" -> "Добавить в друзья"
                            "pending" -> "Действия"
                            else -> "Добавить в друзья"
                        }
                    )
                }
            }
        } else {
            // Для друзей показываем кнопку управления
            OutlinedButton(
                onClick = { onShowActionButtonsChange(!showActionButtons) },
                enabled = !isLoading
            ) {
                Text("Управление")
            }
        }
    }

    // Дополнительные кнопки действий (показываются по нажатию)
    if (showActionButtons) {
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            when (friendshipStatus) {
                "pending" -> {
                    // Для ожидающих запросов - принять/отклонить
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    userRepository.acceptFriendRequest(user.id) { success, message ->
                                        isLoading = false
                                        if (success) {
                                            onShowMessage("Запрос в друзья принят")
                                            onFriendshipChanged()
                                            onShowActionButtonsChange(false)
                                        } else {
                                            onShowMessage(message ?: "Ошибка принятия запроса")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("Принять")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    userRepository.rejectFriendRequest(user.id) { success, message ->
                                        isLoading = false
                                        if (success) {
                                            onShowMessage("Запрос в друзья отклонен")
                                            onFriendshipChanged()
                                            onShowActionButtonsChange(false)
                                        } else {
                                            onShowMessage(message ?: "Ошибка отклонения запроса")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("Отклонить")
                        }
                    }
                }
                "accepted" -> {
                    // Для друзей - удалить из друзей
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                userRepository.removeFriend(user.id) { success, message ->
                                    isLoading = false
                                    if (success) {
                                        onShowMessage("Пользователь удален из друзей")
                                        onFriendshipChanged()
                                        onShowActionButtonsChange(false)
                                    } else {
                                        onShowMessage(message ?: "Ошибка удаления из друзей")
                                    }
                                }
                            }
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