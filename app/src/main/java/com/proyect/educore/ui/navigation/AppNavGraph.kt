package com.proyect.educore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyect.educore.model.Usuario
import com.proyect.educore.ui.screens.auth.LoginScreen
import com.proyect.educore.ui.screens.auth.RegisterScreen
import com.proyect.educore.ui.screens.home.HomeRoute
import com.proyect.educore.ui.screens.home.student.turnos.SolicitarTurnoScreen
import com.proyect.educore.ui.screens.home.student.turnos.DetalleTurnoScreen
import com.proyect.educore.ui.screens.home.student.turnos.HistorialTurnosScreen

enum class AppDestination(val route: String) {
    Login("login"),
    Register("register"),
    Home("home"),
    SolicitarTurno("solicitarTurno"),
    DetalleTurno("detalleTurno/{turnoId}"),
    HistorialTurnos("historialTurnos")
}

private val usuarioSaver: Saver<Usuario?, List<Any?>> = Saver(
    save = { usuario ->
        usuario?.let { listOf(it.id, it.nombre, it.apellido, it.email, it.rol) }
    },
    restore = { saved ->
        if (saved.size < 5) {
            null
        } else {
            Usuario(
                id = (saved[0] as Number).toInt(),
                nombre = saved[1] as String,
                apellido = saved[2] as String,
                email = saved[3] as String,
                rol = saved[4] as String
            )
        }
    }
)

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
    var currentUser by rememberSaveable(stateSaver = usuarioSaver) { mutableStateOf<Usuario?>(null) }
    val startDestination = if (currentUser != null) {
        AppDestination.Home.route
    } else {
        AppDestination.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = { usuario ->
                    currentUser = usuario
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
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Home.route) {
            val usuario = currentUser
            if (usuario == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                }
            } else {
                HomeRoute(
                    usuario = usuario,
                    onLogout = {
                        currentUser = null
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(AppDestination.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToSolicitarTurno = {
                        navController.navigate(AppDestination.SolicitarTurno.route)
                    },
                    onNavigateToHistorial = {
                        navController.navigate(AppDestination.HistorialTurnos.route)
                    }
                )
            }
        }
        composable(AppDestination.SolicitarTurno.route) {
            val usuario = currentUser
            if (usuario != null) {
                SolicitarTurnoScreen(
                    estudianteId = usuario.id.toLong(),
                    onTurnoCreated = { _ ->
                        navController.popBackStack(AppDestination.Home.route, inclusive = false)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(
            AppDestination.DetalleTurno.route,
            arguments = listOf(navArgument("turnoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val turnoId = backStackEntry.arguments?.getLong("turnoId") ?: 0L
            DetalleTurnoScreen(
                turnoId = turnoId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(AppDestination.HistorialTurnos.route) {
            val usuario = currentUser
            if (usuario != null) {
                HistorialTurnosScreen(
                    estudianteId = usuario.id.toLong(),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
