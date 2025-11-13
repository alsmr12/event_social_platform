package com.ark.socialevent.ui.screens
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ark.socialevent.ui.screens.home.HomeScreen
import com.ark.socialevent.ui.screens.events.EventsScreen
import com.ark.socialevent.ui.screens.people.PeopleScreen
import com.ark.socialevent.ui.screens.profile.ProfileScreen
import com.ark.socialevent.ui.screens.friends.FriendsScreen
import com.ark.socialevent.ui.screens.subscriptions.SubscriptionsScreen
import kotlinx.coroutines.launch
import com.ark.socialevent.network.UserRepository
import com.ark.socialevent.ui.screens.profile.EditProfileScreen
import com.ark.socialevent.network.UserProfile

sealed class DrawerScreens(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : DrawerScreens("home", "Главная", Icons.Filled.Home)
    object Events : DrawerScreens("events", "События", Icons.Filled.CalendarToday)
    object People : DrawerScreens("people", "Люди", Icons.Filled.People)
    object Profile : DrawerScreens("profile", "Профиль", Icons.Filled.Person)
    object Friends : DrawerScreens("friends", "Друзья", Icons.Filled.Group)
    object Subscriptions : DrawerScreens("subscriptions", "Подписки", Icons.Filled.AccountCircle)
    object Logout : DrawerScreens("logout", "Выйти", Icons.Filled.ExitToApp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    userRepository: UserRepository

) {
    var currentScreen by remember { mutableStateOf<DrawerScreens>(DrawerScreens.Home) }
    val drawerState = remember { DrawerState(DrawerValue.Closed) }
    val coroutineScope = rememberCoroutineScope()

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
                    DrawerScreens.Home,
                    DrawerScreens.Events,
                    DrawerScreens.People,
                    DrawerScreens.Profile,
                    DrawerScreens.Friends,
                    DrawerScreens.Subscriptions
                ).forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
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
                        onLogout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
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
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    DrawerScreens.Home -> HomeScreen()
                    DrawerScreens.Events -> EventsScreen()
                    DrawerScreens.People -> PeopleScreen(userRepository = userRepository)
                    DrawerScreens.Profile -> {
                        var showEditProfile by remember { mutableStateOf(false) }

                        if (showEditProfile) {
                            // Получаем текущий профиль для редактирования
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
                    DrawerScreens.Friends -> FriendsScreen(userRepository = userRepository)
                    DrawerScreens.Subscriptions -> SubscriptionsScreen()
                    DrawerScreens.Logout -> HomeScreen() // fallback
                }
            }
        }
    }
}