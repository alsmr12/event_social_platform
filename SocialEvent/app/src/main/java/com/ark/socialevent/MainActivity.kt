package com.ark.socialevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.ark.socialevent.network.UserRepository
import com.ark.socialevent.ui.screens.LoginScreen
import com.ark.socialevent.ui.screens.MainScreen
import com.ark.socialevent.ui.screens.RegisterScreen
import com.ark.socialevent.ui.theme.SocialEventTheme

// app/src/main/java/com/ark/socialevent/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ПЕРЕДАЕМ CONTEXT в репозиторий!
        val userRepo = UserRepository(this)

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
                        onLogout = {
                            userRepo.logout() // Вызываем логаут
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}