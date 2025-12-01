package com.proyect.educore.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.proyect.educore.model.Usuario
import com.proyect.educore.ui.screens.home.secretary.SecretaryHomeScreen
import com.proyect.educore.ui.screens.home.secretary.SecretaryProceduresScreen
import com.proyect.educore.ui.screens.home.secretary.SecretaryQueueScreen
import com.proyect.educore.ui.screens.home.student.StudentHomeScreen
import java.util.Locale

@Composable
fun HomeRoute(
    usuario: Usuario,
    onLogout: () -> Unit,
    onNavigateToSolicitarTurno: () -> Unit = {},
    onNavigateToHistorial: () -> Unit = {}
) {
    when {
        usuario.isSecretary() -> {
            var section by rememberSaveable { mutableStateOf(SecretarySection.Home) }
            when (section) {
                SecretarySection.Home -> SecretaryHomeScreen(
                    usuario = usuario,
                    onNavigateToProcedures = { section = SecretarySection.Procedures },
                    onNavigateToQueue = { section = SecretarySection.Queue },
                    onLogout = onLogout
                )
                SecretarySection.Queue -> SecretaryQueueScreen(
                    onNavigateBack = { section = SecretarySection.Home },
                    onLogout = onLogout
                )
                SecretarySection.Procedures -> SecretaryProceduresScreen(
                    usuario = usuario,
                    onNavigateBack = { section = SecretarySection.Home },
                    onLogout = onLogout
                )
            }
        }
        usuario.isStudent() -> StudentHomeScreen(
            onLogout = onLogout,
            onNavigateToSolicitarTurno = onNavigateToSolicitarTurno,
            onNavigateToHistorial = onNavigateToHistorial
        )
        else -> StudentHomeScreen(
            onLogout = onLogout,
            onNavigateToSolicitarTurno = onNavigateToSolicitarTurno,
            onNavigateToHistorial = onNavigateToHistorial
        )
    }
}

private fun Usuario.isSecretary(): Boolean {
    val normalizedRole = rol.trim().uppercase(Locale.ROOT)
    if (normalizedRole.contains("SECRETARIA")) {
        return true
    }
    return email.contains("SECRETARIA", ignoreCase = true)
}

private fun Usuario.isStudent(): Boolean {
    val normalizedRole = rol.trim().uppercase(Locale.ROOT)
    if (normalizedRole.contains("ESTUDIANTE")) return true
    return email.contains("ESTUDIANTE", ignoreCase = true)
}

private enum class SecretarySection {
    Home,
    Queue,
    Procedures
}
