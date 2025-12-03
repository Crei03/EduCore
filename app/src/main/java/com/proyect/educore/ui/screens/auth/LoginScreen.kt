package com.proyect.educore.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.Usuario
import com.proyect.educore.model.repository.AuthRepository
import com.proyect.educore.model.repository.LoginResult
import com.proyect.educore.ui.components.ButtonVariant
import com.proyect.educore.ui.components.EduCoreButton
import com.proyect.educore.ui.components.EduCoreTextField
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.notification.EduCoreNotificationHost
import com.proyect.educore.ui.components.notification.NotificationType
import com.proyect.educore.ui.components.notification.rememberNotificationState
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (Usuario) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notificationState = rememberNotificationState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    fun showError(message: String) {
        notificationState.showError(message)
    }

    fun validateInputs(): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            showError("Ingresa un correo válido.")
            return false
        }
        if (password.length < 6) {
            showError("La contraseña debe tener al menos 6 caracteres.")
            return false
        }
        return true
    }

    fun attemptLogin() {
        focusManager.clearFocus()
        if (!validateInputs() || isLoading) {
            return
        }
        isLoading = true
        coroutineScope.launch {
            val result = AuthRepository.login(email.trim(), password)
            when (result) {
                is LoginResult.Success -> {
                    notificationState.showSuccess(result.message)
                    onLoginSuccess(result.usuario)
                }
                is LoginResult.Error -> showError(result.message)
            }
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(15.dp))

                // Logo con fondo circular moderno
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.School,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 56.dp
                    )
                }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Bienvenido de vuelta",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Inicia sesión para acceder a tus trámites escolares.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(40.dp))

                // Campo de correo con nuevo componente
                EduCoreTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Correo institucional",
                    placeholder = "alumno@universidad.edu",
                    leadingIcon = RemoteIconSpec.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            
            Spacer(modifier = Modifier.height(20.dp))

                // Campo de contraseña con nuevo componente
                EduCoreTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Contraseña",
                    leadingIcon = RemoteIconSpec.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = { attemptLogin() }
                )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = { /* TODO: Agregar lógica para recuperar contraseña */ },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

                // Botón de ingreso con nuevo componente
                EduCoreButton(
                    text = if (isLoading) "Validando..." else "Ingresar",
                    onClick = { attemptLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.PRIMARY,
                    isLoading = isLoading,
                    enabled = !isLoading,
                    fullWidth = true
                )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "¿No tienes cuenta? ")
                Text(
                    text = "Regístrate aquí",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Host de notificaciones superpuesto
        EduCoreNotificationHost(
            state = notificationState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    EduCoreTheme {
        LoginScreen(onLoginSuccess = { _ -> }, onNavigateToRegister = {})
    }
}
