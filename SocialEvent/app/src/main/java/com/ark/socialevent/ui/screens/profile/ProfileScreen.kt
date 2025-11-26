package com.ark.socialevent.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.*
import com.ark.socialevent.utils.DateUtils // Импортируем утилиты для дат
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    userRepository: UserRepository,
    onEditProfile: () -> Unit
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userStats by remember { mutableStateOf<UserStats?>(null) }
    var wallPosts by remember { mutableStateOf<List<WallPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddPostDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<WallPost?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Функция для загрузки всех данных профиля
    fun loadProfileData() {
        loading = true

        // Загружаем профиль
        userRepository.getProfile { profile ->
            userProfile = profile
            if (profile != null) {
                // Загружаем статистику
                userRepository.getUserStats { stats, error ->
                    userStats = stats
                    if (error != null) {
                        Log.e("ProfileScreen", "Error loading stats: $error")
                    }
                }

                // Загружаем посты стены
                userRepository.getUserWallPosts(profile.id) { posts, error ->
                    wallPosts = posts ?: emptyList()
                    loading = false
                    if (error != null) {
                        errorMessage = "Ошибка загрузки стены: $error"
                    }
                }
            } else {
                loading = false
                errorMessage = "Не удалось загрузить профиль"
            }
        }
    }

    // Загружаем данные при открытии экрана
    LaunchedEffect(Unit) {
        loadProfileData()
    }

    Scaffold(
        floatingActionButton = {
            if (userProfile != null && !loading) {
                FloatingActionButton(
                    onClick = { showAddPostDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить запись")
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
            // Заголовок и кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Мой профиль",
                    style = MaterialTheme.typography.headlineMedium
                )
                Row {
                    IconButton(
                        onClick = { loadProfileData() },
                        enabled = !loading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = if (loading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onEditProfile,
                        enabled = userProfile != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать профиль")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Загрузка профиля...")
                        }
                    }
                }
                errorMessage != null -> {
                    Column {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Ошибка",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "Ошибка",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        // Кнопка повторной загрузки
                        Button(
                            onClick = {
                                errorMessage = null
                                loadProfileData()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить")
                        }
                    }
                }
                userProfile != null -> {
                    ProfileContent(
                        userProfile = userProfile!!,
                        userStats = userStats,
                        wallPosts = wallPosts,
                        onEditPost = { post -> editingPost = post },
                        onDeletePost = { post ->
                            coroutineScope.launch {
                                userRepository.deleteWallPost(post.id) { success, message ->
                                    if (success) {
                                        // Обновляем список постов
                                        wallPosts = wallPosts.filter { it.id != post.id }
                                    } else {
                                        errorMessage = message ?: "Ошибка удаления записи"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        }

        // Диалог добавления новой записи
        if (showAddPostDialog) {
            AddPostDialog(
                onDismiss = { showAddPostDialog = false },
                onAddPost = { content ->
                    coroutineScope.launch {
                        userProfile?.let { profile ->
                            userRepository.createWallPost(content, profile.id) { success, message ->
                                if (success) {
                                    // Перезагружаем посты
                                    userRepository.getUserWallPosts(profile.id) { posts, error ->
                                        wallPosts = posts ?: emptyList()
                                        showAddPostDialog = false
                                        if (error != null) {
                                            errorMessage = "Запись добавлена, но ошибка обновления: $error"
                                        }
                                    }
                                } else {
                                    errorMessage = message ?: "Ошибка создания записи"
                                }
                            }
                        }
                    }
                }
            )
        }

        // Диалог редактирования записи
        editingPost?.let { post ->
            EditPostDialog(
                post = post,
                onDismiss = { editingPost = null },
                onEditPost = { content ->
                    coroutineScope.launch {
                        userRepository.updateWallPost(post.id, content) { success, message ->
                            if (success) {
                                // Обновляем список постов
                                wallPosts = wallPosts.map {
                                    if (it.id == post.id) it.copy(content = content) else it
                                }
                                editingPost = null
                            } else {
                                errorMessage = message ?: "Ошибка обновления записи"
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileContent(
    userProfile: UserProfile,
    userStats: UserStats?,
    wallPosts: List<WallPost>,
    onEditPost: (WallPost) -> Unit,
    onDeletePost: (WallPost) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Основная информация - ОБНОВЛЕННАЯ
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${userProfile.firstName} ${userProfile.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                InfoRow(icon = Icons.Default.Email, text = userProfile.email)
                InfoRow(icon = Icons.Default.Person, text = userProfile.gender)

                // ОБНОВЛЕННАЯ СЕКЦИЯ ВОЗРАСТА И ДАТЫ РОЖДЕНИЯ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${userProfile.age} лет",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        // Добавляем дату рождения, если она есть
                        userProfile.birthDate?.let { birthDate ->
                            Text(
                                text = "Родился(ась): ${DateUtils.formatBirthDate(birthDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                InfoRow(icon = Icons.Default.Phone, text = userProfile.phone)
            }
        }

        // Статистика
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Статистика",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        count = userStats?.friendsCount?.toString() ?: "0",
                        label = "Друзей"
                    )
                    StatItem(
                        count = userStats?.eventsCount?.toString() ?: "0",
                        label = "Событий"
                    )
                    StatItem(
                        count = userStats?.followersCount?.toString() ?: "0",
                        label = "Подписчиков"
                    )
                    StatItem(
                        count = userStats?.followingCount?.toString() ?: "0",
                        label = "Подписок"
                    )
                }
            }
        }

        // Моя стена
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Моя стена",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${wallPosts.size} зап.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (wallPosts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = "Нет записей",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "На вашей стене пока нет записей",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Нажмите + чтобы добавить первую запись",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    wallPosts.forEach { post ->
                        WallPostItem(
                            post = post,
                            onEdit = { onEditPost(post) },
                            onDelete = { onDeletePost(post) },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        if (post != wallPosts.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallPostItem(
    post: WallPost,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок с информацией об авторе и времени
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Вы",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatDate(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое поста
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Функция для форматирования даты
fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString // возвращаем оригинальную строку в случае ошибки
    }
}

@Composable
fun AddPostDialog(
    onDismiss: () -> Unit,
    onAddPost: (String) -> Unit
) {
    var postContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая запись") },
        text = {
            Column {
                Text(
                    "Что у вас нового?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Напишите вашу запись...") },
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (postContent.isNotBlank()) {
                        onAddPost(postContent)
                    }
                },
                enabled = postContent.isNotBlank()
            ) {
                Text("Опубликовать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditPostDialog(
    post: WallPost,
    onDismiss: () -> Unit,
    onEditPost: (String) -> Unit
) {
    var postContent by remember { mutableStateOf(post.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать запись") },
        text = {
            Column {
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Текст записи...") },
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (postContent.isNotBlank()) {
                        onEditPost(postContent)
                    }
                },
                enabled = postContent.isNotBlank() && postContent != post.content
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge,
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