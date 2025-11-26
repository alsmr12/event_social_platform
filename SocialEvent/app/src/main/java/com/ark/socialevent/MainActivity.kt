package com.ark.socialevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.ark.socialevent.network.ApiClient
import com.ark.socialevent.network.UserRepository
import com.ark.socialevent.network.EventRepository
import com.ark.socialevent.ui.screens.LoginScreen
import com.ark.socialevent.ui.screens.MainScreen
import com.ark.socialevent.ui.screens.RegisterScreen
import com.ark.socialevent.ui.theme.SocialEventTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализируем ApiClient с правильным baseUrl для локального сервера
        ApiClient.initialize(this)
        ApiClient.setBaseUrl("http://10.0.2.2:8080") // для эмулятора

        // Инициализируем репозитории
        val userRepo = UserRepository(this)
        val eventRepo = EventRepository(this)

        setContent {
            SocialEventTheme {
                var currentScreen by remember { mutableStateOf("login") }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        userRepo = userRepo,
                        onLoginSuccess = { currentScreen = "main" },
                        onNavigateToRegister = { currentScreen = "register" }
                    )
                    "register" -> RegisterScreen(
                        userRepo = userRepo,
                        onRegisterSuccess = { currentScreen = "main" },
                        onNavigateToLogin = { currentScreen = "login" }
                    )
                    "main" -> MainScreen(
                        userRepository = userRepo,
                        eventRepository = eventRepo,
                        onLogout = {
                            userRepo.logout()
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}