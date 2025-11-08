package com.proyect.educore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proyect.educore.ui.screens.auth.LoginScreen
import com.proyect.educore.ui.screens.auth.RegisterScreen
import com.proyect.educore.ui.screens.home.HomeScreen

enum class AppDestination(val route: String) {
    Login("login"),
    Register("register"),
    Home("home")
}

@Composable
fun EduCoreApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    AppNavGraph(
        modifier = modifier,
        navController = navController
    )
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Login.route,
        modifier = modifier
    ) {
        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppDestination.Register.route)
                }
            )
        }
        composable(AppDestination.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Home.route) {
            HomeScreen(
                onLogout = {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
