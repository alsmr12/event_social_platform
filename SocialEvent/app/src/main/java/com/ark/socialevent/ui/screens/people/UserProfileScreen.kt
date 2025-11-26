// UserProfileScreen.kt - ИСПРАВЛЕННАЯ ВЕРСИЯ
package com.ark.socialevent.ui.screens.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.ark.socialevent.state.FriendshipStateManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Int,
    userRepository: UserRepository,
    onBack: () -> Unit
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userStats by remember { mutableStateOf<UserStats?>(null) }
    var subscriptionStats by remember { mutableStateOf<SubscriptionStats?>(null) }
    var wallPosts by remember { mutableStateOf<List<WallPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Загружаем данные пользователя
    LaunchedEffect(userId) {
        isLoading = true

        // Загружаем профиль из списка всех пользователей
        userRepository.getAllProfiles { users, error ->
            if (error == null && users != null) {
                userProfile = users.find { it.id == userId }
                if (userProfile == null) {
                    errorMessage = "Пользователь не найден"
                    isLoading = false
                    return@getAllProfiles
                }

                // Загружаем статистику текущего пользователя (не того, чей профиль смотрим)
                userRepository.getUserStats { stats, _ ->
                    userStats = stats
                }

                // Загружаем статистику подписок просматриваемого пользователя
                userRepository.getSubscriptionStats(userId) { stats, _ ->
                    subscriptionStats = stats
                }

                // Загружаем записи на стене просматриваемого пользователя - ИСПРАВЛЕНО!
                userRepository.getUserWallPosts(userId) { posts, error ->
                    wallPosts = if (error == null) posts ?: emptyList() else emptyList()
                    isLoading = false
                }
            } else {
                errorMessage = error ?: "Ошибка загрузки профиля"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        userProfile?.let { "${it.firstName} ${it.lastName}" } ?: "Профиль"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Загрузка профиля...")
                }
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Ошибка",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        errorMessage ?: "Ошибка загрузки",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Назад")
                    }
                }
            }
        } else if (userProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = "Пользователь не найден",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Пользователь не найден")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Назад")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    // Основная информация
                    UserProfileHeader(
                        user = userProfile!!,
                        userStats = userStats,
                        subscriptionStats = subscriptionStats,
                        userRepository = userRepository,
                        onShowMessage = { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                }

                // Записи на стене
                // Записи на стене
                if (wallPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Записи на стене",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    wallPosts.forEach { post ->
                        item {
                            WallPostItem(post = post)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Нет записей",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "На стене пока нет записей",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileHeader(
    user: UserProfile,
    userStats: UserStats?,
    subscriptionStats: SubscriptionStats?,
    userRepository: UserRepository,
    onShowMessage: (String) -> Unit
) {
    var friendshipStatus by remember { mutableStateOf<String?>(null) }
    var isSubscribed by remember { mutableStateOf(false) }
    var isLoadingStatus by remember { mutableStateOf(true) }
    var showFriendshipActions by remember { mutableStateOf(false) }
    var isIncomingRequest by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Загружаем статус дружбы и подписки
    LaunchedEffect(user.id, FriendshipStateManager.refreshPeopleTrigger) {
        userRepository.getFriendshipStatus(user.id) { status, error ->
            friendshipStatus = if (error == null) status else "none"

            if (status == "pending") {
                userRepository.getPendingRequests { requests, _ ->
                    isIncomingRequest = requests?.any { it.user.id == user.id } == true
                }
            }

            userRepository.checkSubscription(user.id) { subscribed, _ ->
                isSubscribed = subscribed
                isLoadingStatus = false
            }
        }
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Аватар и основная информация
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Аватар
            Icon(
                Icons.Default.Person,
                contentDescription = "Аватар",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${user.age} лет • ${user.gender}",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Статистика
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = userStats?.friendsCount?.toString() ?: "0",
                label = "Друзей"
            )
            StatItem(
                value = subscriptionStats?.followersCount?.toString() ?: "0",
                label = "Подписчиков"
            )
            StatItem(
                value = subscriptionStats?.followingCount?.toString() ?: "0",
                label = "Подписок"
            )
            StatItem(
                value = userStats?.eventsCount?.toString() ?: "0",
                label = "Событий"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isLoadingStatus) {
            // Кнопки действий
            ActionButtons(
                user = user,
                friendshipStatus = friendshipStatus,
                isIncomingRequest = isIncomingRequest,
                isSubscribed = isSubscribed,
                showFriendshipActions = showFriendshipActions,
                userRepository = userRepository,
                onShowMessage = onShowMessage,
                onToggleFriendshipActions = { showFriendshipActions = !showFriendshipActions }
            )
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActionButtons(
    user: UserProfile,
    friendshipStatus: String?,
    isIncomingRequest: Boolean,
    isSubscribed: Boolean,
    showFriendshipActions: Boolean,
    userRepository: UserRepository,
    onShowMessage: (String) -> Unit,
    onToggleFriendshipActions: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun handleOperation(
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
                    userRepository.getFriendshipStatus(user.id) { status, _ ->
                        // Обновление состояния
                    }
                    userRepository.checkSubscription(user.id) { subscribed, _ ->
                        // Обновление состояния
                    }
                } else {
                    onShowMessage(message ?: "Ошибка операции")
                }
            }
        }
    }

    Column {
        // Дружба
        FriendshipActionButton(
            friendshipStatus = friendshipStatus,
            isIncomingRequest = isIncomingRequest,
            showActions = showFriendshipActions,
            isLoading = isLoading,
            onToggleActions = onToggleFriendshipActions,
            onSendRequest = {
                handleOperation(
                    { callback -> userRepository.sendFriendRequest(user.id, callback) },
                    "Запрос в друзья отправлен"
                )
            },
            onAcceptRequest = {
                handleOperation(
                    { callback -> userRepository.acceptFriendRequest(user.id, callback) },
                    "Запрос принят"
                )
            },
            onRejectRequest = {
                handleOperation(
                    { callback -> userRepository.rejectFriendRequest(user.id, callback) },
                    "Запрос отклонен"
                )
            },
            onCancelRequest = {
                handleOperation(
                    { callback -> userRepository.cancelFriendRequest(user.id, callback) },
                    "Заявка отменена"
                )
            },
            onRemoveFriend = {
                handleOperation(
                    { callback -> userRepository.removeFriend(user.id, callback) },
                    "Удален из друзей"
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Подписка
        SubscriptionActionButton(
            isSubscribed = isSubscribed,
            isLoading = isLoading,
            onSubscribe = {
                handleOperation(
                    { callback -> userRepository.subscribeToUser(user.id, callback) },
                    "Вы подписались"
                )
            },
            onUnsubscribe = {
                handleOperation(
                    { callback -> userRepository.unsubscribeFromUser(user.id, callback) },
                    "Вы отписались"
                )
            }
        )
    }
}

@Composable
fun FriendshipActionButton(
    friendshipStatus: String?,
    isIncomingRequest: Boolean,
    showActions: Boolean,
    isLoading: Boolean,
    onToggleActions: () -> Unit,
    onSendRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onCancelRequest: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    Column {
        when (friendshipStatus) {
            "none" -> {
                Button(
                    onClick = onSendRequest,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить в друзья")
                    }
                }
            }
            "pending" -> {
                if (isIncomingRequest) {
                    // Входящая заявка
                    Button(
                        onClick = onToggleActions,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("Ответить на заявку")
                    }

                    if (showActions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAcceptRequest,
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Принять")
                            }
                            OutlinedButton(
                                onClick = onRejectRequest,
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Отклонить")
                            }
                        }
                    }
                } else {
                    // Исходящая заявка
                    OutlinedButton(
                        onClick = onCancelRequest,
                        modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Друзья")
                }

                if (showActions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRemoveFriend,
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
fun SubscriptionActionButton(
    isSubscribed: Boolean,
    isLoading: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    if (isSubscribed) {
        OutlinedButton(
            onClick = onUnsubscribe,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отписаться")
            }
        }
    } else {
        Button(
            onClick = onSubscribe,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Подписаться")
            }
        }
    }
}

@Composable
fun WallPostItem(post: WallPost) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Автор и дата
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Автор",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${post.author.firstName} ${post.author.lastName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое поста
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Функция для форматирования даты
fun formatDate(dateString: String): String {
    return try {
        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        var parsedDate: Date? = null
        for (format in formats) {
            try {
                parsedDate = format.parse(dateString)
                if (parsedDate != null) break
            } catch (e: Exception) {}
        }

        val outputFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
        parsedDate?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}