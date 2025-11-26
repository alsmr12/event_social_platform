package com.ark.socialevent.ui.screens.friends

import android.util.Log
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
import com.ark.socialevent.network.Friend
import com.ark.socialevent.network.FriendRequest
import com.ark.socialevent.network.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun FriendsScreen(
    userRepository: UserRepository,
    onOpenProfile: (Int) -> Unit // Добавляем callback для открытия профиля
) {
    var currentTab by remember { mutableStateOf(0) }
    val tabs = listOf("Друзья", "Входящие заявки", "Исходящие заявки")

    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Следим за глобальным триггером обновления
    val refreshTrigger by remember { mutableStateOf(com.ark.socialevent.state.FriendshipStateManager.refreshFriendsTrigger) }

    // Функция для принудительного обновления данных текущей вкладки
    val refreshCurrentTab = {
        loading = true
        errorMessage = null

        when (currentTab) {
            0 -> {
                userRepository.getFriends { result, error ->
                    friends = result ?: emptyList()
                    errorMessage = error
                    loading = false
                }
            }
            1 -> {
                userRepository.getPendingRequests { result, error ->
                    val incomingRequests = result?.map { it.copy(isIncoming = true) } ?: emptyList()
                    pendingRequests = incomingRequests
                    errorMessage = error
                    loading = false
                }
            }
            2 -> {
                userRepository.getSentRequests { result, error ->
                    val outgoingRequests = result?.map { it.copy(isIncoming = false) } ?: emptyList()
                    sentRequests = outgoingRequests
                    errorMessage = error
                    loading = false
                }
            }
        }
    }

    // Загружаем данные при первом открытии, при смене вкладки И при глобальном обновлении
    LaunchedEffect(currentTab, refreshTrigger) {
        refreshCurrentTab()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Друзья",
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(
                onClick = { refreshCurrentTab() },
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = currentTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = currentTab == index,
                    onClick = { currentTab = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                when (currentTab) {
                    0 -> FriendsList(
                        friends = friends,
                        userRepository = userRepository,
                        onFriendRemoved = refreshCurrentTab,
                        onOpenProfile = onOpenProfile
                    )
                    1 -> PendingRequestsList(
                        requests = pendingRequests,
                        userRepository = userRepository,
                        onRequestProcessed = refreshCurrentTab,
                        onOpenProfile = onOpenProfile
                    )
                    2 -> SentRequestsList(
                        requests = sentRequests,
                        userRepository = userRepository,
                        onRequestCancelled = refreshCurrentTab,
                        onOpenProfile = onOpenProfile
                    )
                }
            }
        }
    }
}

@Composable
fun FriendsList(
    friends: List<Friend>,
    userRepository: UserRepository,
    onFriendRemoved: () -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    if (friends.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("У вас пока нет друзей")
        }
    } else {
        LazyColumn {
            items(friends) { friend ->
                FriendItem(
                    friend = friend,
                    userRepository = userRepository,
                    onFriendRemoved = onFriendRemoved,
                    onOpenProfile = onOpenProfile
                )
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: Friend,
    userRepository: UserRepository,
    onFriendRemoved: () -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var isRemoving by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = {
            // При клике на карточку открываем профиль
            onOpenProfile(friend.id)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${friend.firstName} ${friend.lastName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${friend.age} лет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isRemoving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                IconButton(
                    onClick = {
                        // Останавливаем распространение события, чтобы не открывался профиль
                        showDialog = true
                    },
                    enabled = !isRemoving
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Действия")
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Действия с другом") },
            text = { Text("Вы действительно хотите удалить ${friend.firstName} из друзей?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isRemoving = true
                        userRepository.removeFriend(friend.id) { success, message ->
                            isRemoving = false
                            showDialog = false
                            if (success) {
                                onFriendRemoved()
                            }
                        }
                    },
                    enabled = !isRemoving
                ) {
                    if (isRemoving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Удалить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    enabled = !isRemoving
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PendingRequestsList(
    requests: List<FriendRequest>,
    userRepository: UserRepository,
    onRequestProcessed: () -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет входящих заявок")
        }
    } else {
        LazyColumn {
            items(requests) { request ->
                PendingRequestItem(
                    request = request,
                    userRepository = userRepository,
                    onRequestProcessed = onRequestProcessed,
                    onOpenProfile = onOpenProfile
                )
            }
        }
    }
}

@Composable
fun PendingRequestItem(
    request: FriendRequest,
    userRepository: UserRepository,
    onRequestProcessed: () -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = {
            // При клике на карточку открываем профиль пользователя
            onOpenProfile(request.user.id)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "${request.user.firstName} ${request.user.lastName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Text(
                if (request.isIncoming) {
                    "Хочет добавить вас в друзья"
                } else {
                    "Вы отправили заявку в друзья"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (request.isIncoming) {
                Row {
                    Button(
                        onClick = {
                            if (!isProcessing) {
                                isProcessing = true
                                userRepository.acceptFriendRequest(request.user.id) { success, message ->
                                    isProcessing = false
                                    if (success) {
                                        onRequestProcessed()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Принять")
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            if (!isProcessing) {
                                isProcessing = true
                                userRepository.rejectFriendRequest(request.user.id) { success, message ->
                                    isProcessing = false
                                    if (success) {
                                        onRequestProcessed()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text("Отклонить")
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Ожидание",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Ожидает ответа",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = {
                            if (!isProcessing) {
                                isProcessing = true
                                userRepository.rejectFriendRequest(request.user.id) { success, message ->
                                    isProcessing = false
                                    if (success) {
                                        onRequestProcessed()
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Отменить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestsList(
    requests: List<FriendRequest>,
    userRepository: UserRepository,
    onRequestCancelled: () -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет отправленных заявок")
        }
    } else {
        LazyColumn {
            items(requests) { request ->
                var isCancelling by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = {
                        // При клике на карточку открываем профиль пользователя
                        onOpenProfile(request.friend.id)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${request.friend.firstName} ${request.friend.lastName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Ожидает ответа",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isCancelling) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            TextButton(
                                onClick = {
                                    isCancelling = true
                                    userRepository.rejectFriendRequest(request.friend.id) { success, message ->
                                        coroutineScope.launch {
                                            isCancelling = false
                                            if (success) {
                                                Log.d("FriendsScreen", "✅ Request cancelled successfully")
                                                delay(500)
                                                onRequestCancelled()
                                            } else {
                                                Log.e("FriendsScreen", "❌ Failed to cancel request: $message")
                                            }
                                        }
                                    }
                                },
                                enabled = !isCancelling
                            ) {
                                if (isCancelling) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Отменить")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}