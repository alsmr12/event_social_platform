package com.ark.socialevent.navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ark.socialevent.ui.auth.LoginScreen
import com.ark.socialevent.ui.auth.RegistrationScreen
import com.ark.socialevent.ui.home.HomeScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController() // It's good practice to provide the NavController as a parameter
) {
    // The start destination should reference the 'route' property of the 'Login' object
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) { LoginScreen(navController = navController) }
        composable(Screen.Register.route) { RegistrationScreen(navController = navController) }
        composable(Screen.Home.route) { HomeScreen(navController = navController) }
    }
}
