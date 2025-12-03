package com.ark.socialevent.ui.screens.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ark.socialevent.network.UserAchievement
import com.ark.socialevent.network.UserRating
import com.ark.socialevent.network.UserRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(userRepository: UserRepository) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var myAchievements by remember { mutableStateOf<List<UserAchievement>>(emptyList()) }
    var ratings by remember { mutableStateOf<List<UserRating>>(emptyList()) }
    var totalPoints by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTab) {
        isLoading = true
        errorMessage = null

        when (selectedTab) {
            0 -> {
                // Загружаем мои достижения
                userRepository.getMyAchievements { achievements, error ->
                    achievements?.let { myAchievements = it }
                    error?.let { errorMessage = it }
                    isLoading = false
                }
                // Загружаем общее количество очков
                userRepository.getTotalPoints { points, _ ->
                    totalPoints = points
                }
            }
            1 -> {
                // Загружаем рейтинг
                userRepository.getRatings { ratingsList, error ->
                    ratingsList?.let { ratings = it }
                    error?.let { errorMessage = it }
                    isLoading = false
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Заголовок с очками (только для вкладки "Мои достижения")
            if (selectedTab == 0 && totalPoints != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Очки",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Всего очков: $totalPoints",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Переключатель вкладок
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Мои достижения") },
                    icon = {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = "Мои достижения"
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Рейтинг") },
                    icon = {
                        Icon(
                            Icons.Filled.Leaderboard,
                            contentDescription = "Рейтинг"
                        )
                    }
                )
            }

            // Содержимое вкладок
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Ошибка",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    when (selectedTab) {
                        0 -> MyAchievementsTab(myAchievements)
                        1 -> RatingsTab(ratings)
                    }
                }
            }
        }
    }
}

@Composable
fun MyAchievementsTab(achievements: List<UserAchievement>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        items(achievements) { achievement ->
            AchievementItem(achievement)
        }
    }
}

@Composable
fun AchievementItem(achievement: UserAchievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.completed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.completed) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка достижения
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.achievement.icon,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о достижении
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = achievement.achievement.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${achievement.achievement.points} очков",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = achievement.achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Прогресс-бар
                val progress = achievement.progress.toFloat()
                val maxProgress = achievement.achievement.condition.toFloat()
                val progressPercentage = if (maxProgress > 0) (progress / maxProgress) * 100 else 0f

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Прогресс: $progress/${achievement.achievement.condition}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${progressPercentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    androidx.compose.material3.LinearProgressIndicator(
                        progress = progress / maxProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (achievement.completed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }

                // Статус
                if (achievement.completed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ Выполнено ${achievement.completedAt?.substring(0, 10) ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RatingsTab(ratings: List<UserRating>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        itemsIndexed(ratings) { index, rating ->
            RatingItem(rating, index + 1)
        }
    }
}

@Composable
fun RatingItem(rating: UserRating, position: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Место в рейтинге
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                when (position) {
                    1 -> {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Первое место",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    2 -> {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Второе место",
                            tint = Color(0xFFC0C0C0),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    3 -> {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Третье место",
                            tint = Color(0xFFCD7F32),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "$position",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о пользователе
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${rating.firstName} ${rating.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Очки
            Text(
                text = "${rating.points} очков",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}