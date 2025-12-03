package com.ark.socialevent.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.ark.socialevent.network.ApiClient
import com.ark.socialevent.network.EventRepository
import com.ark.socialevent.network.UserProfile
import com.ark.socialevent.network.UserRepository
import com.ark.socialevent.ui.screens.achievements.AchievementsScreen
import com.ark.socialevent.ui.screens.events.EventDetailsDialog
import com.ark.socialevent.ui.screens.events.EventsScreen
import com.ark.socialevent.ui.screens.events.MyEventsScreen
import com.ark.socialevent.ui.screens.friends.FriendsScreen
import com.ark.socialevent.ui.screens.map.MapScreen
import com.ark.socialevent.ui.screens.news.NewsFeedScreen
import com.ark.socialevent.ui.screens.people.PeopleScreen
import com.ark.socialevent.ui.screens.people.UserProfileScreen
import com.ark.socialevent.ui.screens.profile.EditProfileScreen
import com.ark.socialevent.ui.screens.profile.ProfileScreen
import com.ark.socialevent.ui.screens.subscriptions.SubscriptionsScreen
import kotlinx.coroutines.launch

sealed class DrawerScreens(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Map : DrawerScreens("map", "Карта", Icons.Filled.Map)
    object News : DrawerScreens("news", "Новости", Icons.Filled.Newspaper)
    object Events : DrawerScreens("events", "События", Icons.Filled.CalendarToday)
    object People : DrawerScreens("people", "Люди", Icons.Filled.People)
    object Profile : DrawerScreens("profile", "Профиль", Icons.Filled.Person)
    object Friends : DrawerScreens("friends", "Друзья", Icons.Filled.Group)
    object Subscriptions : DrawerScreens("subscriptions", "Подписки", Icons.Filled.AccountCircle)
    object Achievements : DrawerScreens("achievements", "Достижения", Icons.Filled.Star)
    object MyEvents : DrawerScreens("my_events", "Мои события", Icons.AutoMirrored.Filled.EventNote)
    object Logout : DrawerScreens("logout", "Выйти", Icons.Filled.ExitToApp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    userRepository: UserRepository,
    eventRepository: EventRepository,
) {
    var currentScreen by remember { mutableStateOf<DrawerScreens>(DrawerScreens.Map) }
    var selectedUserId by remember { mutableStateOf<Int?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val drawerState = remember { DrawerState(DrawerValue.Closed) }
    val coroutineScope = rememberCoroutineScope()

    // Функция для открытия профиля пользователя
    val openUserProfile = { userId: Int ->
        selectedUserId = userId
    }

    // Функция для возврата к основному экрану
    val onBackFromProfile = {
        selectedUserId = null
    }

    var selectedEvent by remember { mutableStateOf<com.ark.socialevent.network.Event?>(null) }

    val openEvent = { event: com.ark.socialevent.network.Event ->
        selectedEvent = event
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Social Event",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Список экранов кроме Logout
                listOf(
                    DrawerScreens.Map,
                    DrawerScreens.Events,
                    DrawerScreens.People,
                    DrawerScreens.News,
                    DrawerScreens.Profile,
                    DrawerScreens.MyEvents,
                    DrawerScreens.Friends,
                    DrawerScreens.Subscriptions,
                    DrawerScreens.Achievements
                ).forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            selectedUserId = null // Сбрасываем выбранный профиль при смене экрана
                            coroutineScope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(DrawerScreens.Logout.icon, contentDescription = null) },
                    label = { Text(DrawerScreens.Logout.title) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                        showLogoutDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                when {
                    selectedUserId != null -> {
                        // Показываем кнопку "Назад" когда открыт профиль пользователя
                        CenterAlignedTopAppBar(
                            title = { Text("Профиль") },
                            navigationIcon = {
                                IconButton(onClick = onBackFromProfile) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                        )
                    }
                    else -> {
                        CenterAlignedTopAppBar(
                            title = { Text(currentScreen.title) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Меню")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    selectedUserId != null -> {
                        // Показываем профиль пользователя
                        UserProfileScreen(
                            userId = selectedUserId!!,
                            userRepository = userRepository,
                            onBack = onBackFromProfile
                        )
                    }
                    else -> {
                        // Показываем обычные экраны
                        when (currentScreen) {
                            DrawerScreens.Map -> MapScreen(
                                userRepository = userRepository,
                                eventRepository = eventRepository,
                                onOpenEvent = openEvent,
                                onOpenProfile = openUserProfile
                            )
                            DrawerScreens.Events -> EventsScreen(eventRepository = eventRepository)
                            DrawerScreens.People -> PeopleScreen(
                                userRepository = userRepository
                            )
                            DrawerScreens.Profile -> {
                                var showEditProfile by remember { mutableStateOf(false) }

                                if (showEditProfile) {
                                    var currentProfile by remember { mutableStateOf<UserProfile?>(null) }

                                    LaunchedEffect(Unit) {
                                        userRepository.getProfile { profile ->
                                            currentProfile = profile
                                        }
                                    }

                                    EditProfileScreen(
                                        userRepository = userRepository,
                                        currentProfile = currentProfile,
                                        onBack = { showEditProfile = false },
                                        onSaveSuccess = { showEditProfile = false }
                                    )
                                } else {
                                    ProfileScreen(
                                        userRepository = userRepository,
                                        onEditProfile = { showEditProfile = true }
                                    )
                                }
                            }
                            DrawerScreens.Friends -> FriendsScreen(
                                userRepository = userRepository,
                                onOpenProfile = openUserProfile
                            )
                            DrawerScreens.Subscriptions -> SubscriptionsScreen(
                                userRepository = userRepository,
                                onOpenProfile = openUserProfile
                            )
                            DrawerScreens.News -> NewsFeedScreen(
                                userRepository = userRepository,
                                eventRepository = eventRepository,
                                onNavigateToPeople = {
                                    currentScreen = DrawerScreens.People
                                },
                                onOpenProfile = openUserProfile,
                                onOpenEvent = openEvent
                            )
                            DrawerScreens.MyEvents -> MyEventsScreen(
                                userRepository = userRepository,
                                eventRepository = eventRepository
                            )
                            DrawerScreens.Achievements -> AchievementsScreen(userRepository = userRepository)
                            DrawerScreens.Logout -> {
                                // Просто показываем карту вместо выхода

                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        Dialog(
            onDismissRequest = { showLogoutDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = "Выход",
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Выход из аккаунта",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Вы уверены, что хотите выйти?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }

                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Выйти")
                        }
                    }
                }
            }
        }
    }

    if (selectedEvent != null) {
        EventDetailsDialog(
            event = selectedEvent!!,
            eventRepository = eventRepository,
            onDismiss = { selectedEvent = null },
            onShowMessage = { message ->
                coroutineScope.launch {
                    // Можно показать снекбар если нужно
                    selectedEvent = null // Закрываем диалог после действия
                }
            }
        )
    }
}

