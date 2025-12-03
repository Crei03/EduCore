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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.repository.AuthRepository
import com.proyect.educore.model.repository.RegisterResult
import com.proyect.educore.ui.components.ButtonVariant
import com.proyect.educore.ui.components.EduCoreButton
import com.proyect.educore.ui.components.EduCoreTextField
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.notification.EduCoreNotificationHost
import com.proyect.educore.ui.components.notification.rememberNotificationState
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notificationState = rememberNotificationState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    fun showError(message: String) {
        notificationState.showError(message)
    }

    fun validateInputs(): Boolean {
        if (firstName.trim().length < 2) {
            showError("Ingresa tu nombre.")
            return false
        }
        if (lastName.trim().length < 2) {
            showError("Ingresa tu apellido.")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            showError("Tu correo institucional no es válido.")
            return false
        }
        if (password.length < 8) {
            showError("La contraseña debe tener al menos 8 caracteres.")
            return false
        }
        if (password != confirmPassword) {
            showError("Las contraseñas no coinciden.")
            return false
        }
        return true
    }

    fun attemptRegister() {
        focusManager.clearFocus()
        if (!validateInputs() || isLoading) {
            return
        }
        isLoading = true
        coroutineScope.launch {
            when (val result = AuthRepository.register(firstName.trim(), lastName.trim(), email.trim(), password)) {
                is RegisterResult.Success -> {
                    notificationState.showSuccess(result.message)
                    onRegisterSuccess()
                }
                is RegisterResult.Error -> showError(result.message)
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
                Spacer(modifier = Modifier.height(24.dp))

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
                    text = "Crear cuenta",
                    style = MaterialTheme.typography.headlineLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Regístrate para iniciar tus trámites digitales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(40.dp))

                    // Campos con nuevos componentes
                    EduCoreTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Nombre",
                        leadingIcon = RemoteIconSpec.Person,
                        imeAction = ImeAction.Next
                    )
                
                Spacer(modifier = Modifier.height(16.dp))

                    EduCoreTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Apellido",
                        leadingIcon = RemoteIconSpec.Person,
                        imeAction = ImeAction.Next
                    )

                Spacer(modifier = Modifier.height(16.dp))

                    EduCoreTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Correo institucional",
                        leadingIcon = RemoteIconSpec.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )

                Spacer(modifier = Modifier.height(16.dp))

                    EduCoreTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Contraseña",
                        leadingIcon = RemoteIconSpec.Lock,
                        isPassword = true,
                        imeAction = ImeAction.Next
                    )

                Spacer(modifier = Modifier.height(16.dp))

                    EduCoreTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Confirmar contraseña",
                        leadingIcon = RemoteIconSpec.Lock,
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        onImeAction = { attemptRegister() }
                    )

                Spacer(modifier = Modifier.height(32.dp))

                    EduCoreButton(
                        text = if (isLoading) "Registrando..." else "Registrarme",
                        onClick = { attemptRegister() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.PRIMARY,
                        isLoading = isLoading,
                        enabled = !isLoading,
                        fullWidth = true
                    )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onNavigateBack,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "¿Ya tienes cuenta? ")
                    Text(
                        text = "Inicia sesión",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Overlay de carga
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Host de notificaciones
        EduCoreNotificationHost(
            state = notificationState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    EduCoreTheme {
        RegisterScreen(onNavigateBack = {}, onRegisterSuccess = {})
    }
}
