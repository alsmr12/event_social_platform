
package com.ark.socialevent.ui.screens.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    var selectedUserId by remember { mutableStateOf<Int?>(null) } // Для навигации на профиль

    val refreshTrigger by remember { mutableStateOf(FriendshipStateManager.refreshPeopleTrigger) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun loadUsers() {
        isLoading = true
        userRepository.getAllProfiles { usersList, error ->
            isLoading = false
            if (error != null) {
                errorMessage = error
            } else {
                users = usersList ?: emptyList()
            }
        }
    }

    LaunchedEffect(refreshTrigger) {
        loadUsers()
    }

    // Если выбран пользователь - показываем его профиль
    selectedUserId?.let { userId ->
        UserProfileScreen(
            userId = userId,
            userRepository = userRepository,
            onBack = { selectedUserId = null }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = { loadUsers() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                }
            }
        }
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Загрузка пользователей...")
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                errorMessage ?: "Ошибка загрузки",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadUsers() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить")
                        }
                    }
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = "Нет пользователей",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Пользователи не найдены",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            },
                            onViewProfile = { userId -> selectedUserId = userId } // Передаем callback
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
    onShowMessage: (String) -> Unit,
    onViewProfile: (Int) -> Unit // Добавляем callback для навигации
) {
    var friendshipStatus by remember { mutableStateOf<String?>(null) }
    var isSubscribed by remember { mutableStateOf(false) }
    var isLoadingStatus by remember { mutableStateOf(true) }
    var showFriendshipActions by remember { mutableStateOf(false) }
    var isIncomingRequest by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Загружаем статус дружбы и подписки
    LaunchedEffect(user.id, FriendshipStateManager.refreshPeopleTrigger) {
        // Загружаем статус дружбы
        userRepository.getFriendshipStatus(user.id) { status, error ->
            friendshipStatus = if (error == null) status else "none"

            // Определяем тип заявки
            if (status == "pending") {
                userRepository.getPendingRequests { requests, _ ->
                    isIncomingRequest = requests?.any { it.user.id == user.id } == true
                }
            }

            // Загружаем статус подписки
            userRepository.checkSubscription(user.id) { subscribed, _ ->
                isSubscribed = subscribed
                isLoadingStatus = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile(user.id) }, // Делаем карточку кликабельной
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Информация о пользователе
            UserInfo(user = user)

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoadingStatus) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                // Кнопки дружбы
                FriendshipSection(
                    user = user,
                    friendshipStatus = friendshipStatus,
                    isIncomingRequest = isIncomingRequest,
                    showActions = showFriendshipActions,
                    userRepository = userRepository,
                    onShowMessage = onShowMessage,
                    onToggleActions = { showFriendshipActions = !showFriendshipActions }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопки подписки
                SubscriptionSection(
                    user = user,
                    isSubscribed = isSubscribed,
                    userRepository = userRepository,
                    onShowMessage = onShowMessage
                )
            }
        }
    }
}

@Composable
fun UserInfo(user: UserProfile) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Аватар",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${user.firstName} ${user.lastName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                Text(
                    text = "${user.age} лет",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Добавляем отображение даты рождения, если она есть
                user.birthDate?.let { birthDate ->
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBirthDate(birthDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user.gender,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = user.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Иконка для навигации на профиль
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = "Перейти к профилю",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FriendshipSection(
    user: UserProfile,
    friendshipStatus: String?,
    isIncomingRequest: Boolean,
    showActions: Boolean,
    userRepository: UserRepository,
    onShowMessage: (String) -> Unit,
    onToggleActions: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                    FriendshipStateManager.refreshAll()
                } else {
                    onShowMessage(message ?: "Ошибка операции")
                }
            }
        }
    }

    Column {
        // Статус и основная кнопка
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Статус дружбы
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
                    "pending" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Основная кнопка действия
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
                        Button(
                            onClick = onToggleActions,
                            enabled = !isLoading
                        ) {
                            Text("Ответить")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                handleFriendshipOperation(
                                    operation = { callback -> userRepository.cancelFriendRequest(user.id, callback) },
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
                                Text("Отменить заявку")
                            }
                        }
                    }
                }
                "accepted" -> {
                    OutlinedButton(
                        onClick = onToggleActions,
                        enabled = !isLoading
                    ) {
                        Text("Друзья")
                    }
                }
            }
        }

        // Дополнительные действия
        if (showActions) {
            Spacer(modifier = Modifier.height(8.dp))
            when (friendshipStatus) {
                "pending" -> {
                    if (isIncomingRequest) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            OutlinedButton(
                                onClick = {
                                    handleFriendshipOperation(
                                        operation = { callback -> userRepository.rejectFriendRequest(user.id, callback) },
                                        successMessage = "Запрос отклонен"
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
                    OutlinedButton(
                        onClick = {
                            handleFriendshipOperation(
                                operation = { callback -> userRepository.removeFriend(user.id, callback) },
                                successMessage = "Удален из друзей"
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

@Composable
fun SubscriptionSection(
    user: UserProfile,
    isSubscribed: Boolean,
    userRepository: UserRepository,
    onShowMessage: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun handleSubscription(
        operation: (callback: (Boolean, String?) -> Unit) -> Unit,
        successMessage: String,
        targetUserId: Int
    ) {
        coroutineScope.launch {
            isLoading = true
            operation { success, message ->
                isLoading = false
                if (success) {
                    // ПРИНУДИТЕЛЬНО ОБНОВЛЯЕМ СТАТУС ПОДПИСКИ
                    userRepository.checkSubscription(targetUserId) { subscribed, _ ->
                        // Обновляем локальное состояние
                        // Note: В реальном приложении это состояние будет обновлено через recomposition
                    }
                    onShowMessage(successMessage)
                    FriendshipStateManager.refreshAll()
                } else {
                    onShowMessage(message ?: "Ошибка подписки")
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Статус подписки
        Text(
            text = if (isSubscribed) "Вы подписаны" else "Не подписаны",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSubscribed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Кнопка подписки/отписки
        if (isSubscribed) {
            OutlinedButton(
                onClick = {
                    handleSubscription(
                        operation = { callback -> userRepository.unsubscribeFromUser(user.id, callback) },
                        successMessage = "Вы отписались",
                        targetUserId = user.id
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
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Отписаться")
                }
            }
        } else {
            Button(
                onClick = {
                    handleSubscription(
                        operation = { callback -> userRepository.subscribeToUser(user.id, callback) },
                        successMessage = "Вы подписались",
                        targetUserId = user.id
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
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Подписаться")
                }
            }
        }
    }


}

private fun formatBirthDate(birthDate: String): String {
    return try {
        // Преобразуем из "2006-01-02" в "02.01.2006"
        val parts = birthDate.split("-")
        if (parts.size == 3) {
            "${parts[2]}.${parts[1]}.${parts[0]}"
        } else {
            birthDate
        }
    } catch (e: Exception) {
        birthDate
    }
}