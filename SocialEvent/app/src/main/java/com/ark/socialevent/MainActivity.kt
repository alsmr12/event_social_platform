package com.ark.socialevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.ark.socialevent.network.UserRepository
import com.ark.socialevent.ui.screens.LoginScreen
import com.ark.socialevent.ui.screens.MainContent
import com.ark.socialevent.ui.screens.RegisterScreen
import com.ark.socialevent.ui.theme.SocialEventTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val userRepo = UserRepository()

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
                        onRegisterSuccess = { currentScreen = "login" },
                        onNavigateToLogin = { currentScreen = "login" }
                    )
                    "main" -> MainContent()
                }
            }
        }
    }
}
