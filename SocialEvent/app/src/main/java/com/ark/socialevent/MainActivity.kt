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
import com.yandex.mapkit.MapKitFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Инициализация MapKit ДО super.onCreate()
        android.util.Log.d("YandexMaps", "Setting API key: 0d77df46-fbc9-4e7b-9733-960a20b4a389")

        try {
            MapKitFactory.setApiKey("0d77df46-fbc9-4e7b-9733-960a20b4a389")
            MapKitFactory.initialize(this)
            android.util.Log.d("YandexMaps", "MapKit initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("YandexMaps", "MapKit init error: ${e.message}", e)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. Проверка что MapKit работает
        try {
            val mapKit = MapKitFactory.getInstance()
            android.util.Log.d("YandexMaps", "MapKit instance created: $mapKit")
        } catch (e: Exception) {
            android.util.Log.e("YandexMaps", "Failed to get MapKit instance: ${e.message}")
        }

        ApiClient.initialize(this)
        ApiClient.setBaseUrl("http://10.0.2.2:8080")

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

    override fun onStart() {
        super.onStart()
        try {
            MapKitFactory.getInstance().onStart()
        } catch (e: Exception) {
            android.util.Log.e("YandexMaps", "Error in onStart: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            MapKitFactory.getInstance().onStop()
        } catch (e: Exception) {
            android.util.Log.e("YandexMaps", "Error in onStop: ${e.message}")
        }
    }
}