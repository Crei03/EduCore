package com.proyect.educore.ui.screens.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.Usuario
import com.proyect.educore.model.repository.AuthRepository
import com.proyect.educore.model.repository.LoginResult
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (Usuario) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    fun showError(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
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
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    onLoginSuccess(result.usuario)
                }
                is LoginResult.Error -> showError(result.message)
            }
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Bienvenido de vuelta",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Inicia sesión para acceder a tus trámites escolares.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo institucional") },
                    placeholder = { Text("alumno@universidad.edu") },
                    leadingIcon = {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.Email,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    leadingIcon = {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.Lock,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            RemoteIcon(
                                iconSpec = if (passwordVisible) {
                                    RemoteIconSpec.VisibilityOff
                                } else {
                                    RemoteIconSpec.Visibility
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        attemptLogin()
                    }),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { attemptLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = if (isLoading) "Validando..." else "Ingresar")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNavigateToRegister) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.PersonAdd,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Crear cuenta")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    EduCoreTheme {
        LoginScreen(onLoginSuccess = { _ -> }, onNavigateToRegister = {})
    }
}
