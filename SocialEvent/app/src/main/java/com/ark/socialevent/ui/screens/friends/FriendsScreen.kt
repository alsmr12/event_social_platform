package com.ark.socialevent.ui.screens.friends

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

@Composable
fun FriendsScreen(userRepository: UserRepository) {
    var currentTab by remember { mutableStateOf(0) }
    val tabs = listOf("Друзья", "Входящие заявки", "Исходящие заявки")

    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Загружаем данные при первом открытии
    LaunchedEffect(currentTab) {
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
                    pendingRequests = result ?: emptyList()
                    errorMessage = error
                    loading = false
                }
            }
            2 -> {
                userRepository.getSentRequests { result, error ->
                    sentRequests = result ?: emptyList()
                    errorMessage = error
                    loading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Друзья",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Табы
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

        // Сообщение об ошибке
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

        // Контент в зависимости от выбранной вкладки
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
                        onFriendRemoved = {
                            coroutineScope.launch {
                                userRepository.getFriends { result, error ->
                                    friends = result ?: emptyList()
                                }
                            }
                        }
                    )
                    1 -> PendingRequestsList(
                        requests = pendingRequests,
                        userRepository = userRepository,
                        onRequestProcessed = {
                            coroutineScope.launch {
                                userRepository.getPendingRequests { result, error ->
                                    pendingRequests = result ?: emptyList()
                                }
                            }
                        }
                    )
                    2 -> SentRequestsList(requests = sentRequests)
                }
            }
        }
    }
}

@Composable
fun FriendsList(
    friends: List<Friend>,
    userRepository: UserRepository,
    onFriendRemoved: () -> Unit
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
                    onFriendRemoved = onFriendRemoved
                )
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: Friend,
    userRepository: UserRepository,
    onFriendRemoved: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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

            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Действия")
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
                        userRepository.removeFriend(friend.id) { success, message ->
                            if (success) {
                                onFriendRemoved()
                            }
                        }
                        showDialog = false
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
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
    onRequestProcessed: () -> Unit
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
                    onRequestProcessed = onRequestProcessed
                )
            }
        }
    }
}

@Composable
fun PendingRequestItem(
    request: FriendRequest,
    userRepository: UserRepository,
    onRequestProcessed: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
                "Хочет добавить вас в друзья",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    onClick = {
                        userRepository.acceptFriendRequest(request.user.id) { success, message ->
                            if (success) {
                                onRequestProcessed()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Принять")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        userRepository.rejectFriendRequest(request.user.id) { success, message ->
                            if (success) {
                                onRequestProcessed()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отклонить")
                }
            }
        }
    }
}

@Composable
fun SentRequestsList(requests: List<FriendRequest>) {
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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

                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}