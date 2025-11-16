package com.proyect.educore.model.repository

import com.proyect.educore.data.api.ApiService
import com.proyect.educore.model.Usuario
import org.json.JSONObject

sealed interface LoginResult {
    data class Success(val message: String, val usuario: Usuario) : LoginResult
    data class Error(val message: String) : LoginResult
}

sealed interface RegisterResult {
    data class Success(val message: String) : RegisterResult
    data class Error(val message: String) : RegisterResult
}

object AuthRepository {

    suspend fun login(email: String, password: String): LoginResult {
        return try {
            val response = ApiService.login(email, password)
            if (response.code in 200..299) {
                parseSuccessResponse(response.body)
            } else {
                LoginResult.Error(response.body.extractMessage("No se pudo iniciar sesión."))
            }
        } catch (e: Exception) {
            LoginResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    suspend fun register(firstName: String, lastName: String, email: String, password: String): RegisterResult {
        return try {
            val response = ApiService.register(
                nombre = firstName,
                apellido = lastName,
                email = email,
                password = password
            )

            if (response.code in 200..299) {
                RegisterResult.Success(
                    response.body.extractMessage("Registro exitoso. Inicia sesión.")
                )
            } else {
                RegisterResult.Error(
                    response.body.extractMessage("No se pudo registrar la cuenta.")
                )
            }
        } catch (e: Exception) {
            RegisterResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    suspend fun register(fullName: String, email: String, password: String): RegisterResult {
        val (firstName, lastName) = splitFullName(fullName)
        return register(firstName, lastName, email, password)
    }

    private fun parseSuccessResponse(responseText: String): LoginResult {
        val json = runCatching { JSONObject(responseText) }.getOrNull()
            ?: return LoginResult.Error("Respuesta inválida del servidor.")

        val userJson = json.optJSONObject("user")
            ?: return LoginResult.Error("No se recibió información del usuario.")
        val usuario = userJson.toUsuario()
            ?: return LoginResult.Error("La respuesta del servidor no contiene datos válidos.")

        val message = json.optString("message").ifBlank { "Inicio de sesión exitoso." }
        return LoginResult.Success(message, usuario)
    }

    private fun JSONObject.toUsuario(): Usuario? {
        val id = optInt("id", -1)
        val nombre = optString("nombre")
        val apellido = optString("apellido")
        val email = optString("email")
        val rol = optString("rol")

        if (id == -1 || nombre.isBlank() || apellido.isBlank() || email.isBlank() || rol.isBlank()) {
            return null
        }
        return Usuario(
            id = id,
            nombre = nombre,
            apellido = apellido,
            email = email,
            rol = rol
        )
    }

    private fun String.extractMessage(fallback: String): String {
        if (isBlank()) return fallback
        return try {
            val json = JSONObject(this)
            json.optString("message").ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun splitFullName(fullName: String): Pair<String, String> {
        val normalized = fullName.trim().replace("\\s+".toRegex(), " ")
        if (normalized.isBlank()) {
            return "Estudiante" to "EduCore"
        }
        val parts = normalized.split(" ")
        val firstName = parts.first()
        val lastName = if (parts.size > 1) {
            parts.drop(1).joinToString(" ")
        } else {
            "Estudiante"
        }
        return firstName to lastName
    }
}
