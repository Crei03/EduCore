package com.proyect.educore.data.api

import com.proyect.educore.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ApiResponse(
    val code: Int,
    val body: String
)

object ApiService {

    suspend fun login(email: String, password: String): ApiResponse = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
        performJsonRequest(
            url = BuildConfig.LOGIN_URL,
            method = "POST",
            payload = payload
        )
    }

    suspend fun register(
        nombre: String,
        apellido: String,
        email: String,
        password: String
    ): ApiResponse = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("nombre", nombre)
            .put("apellido", apellido)
            .put("email", email)
            .put("password", password)
        performJsonRequest(
            url = BuildConfig.REGISTER_URL,
            method = "POST",
            payload = payload
        )
    }

    suspend fun fetchTiposTramite(includeDeleted: Boolean = false): ApiResponse = withContext(Dispatchers.IO) {
        val separator = if (BuildConfig.TIPOS_TRAMITE_URL.contains("?")) "&" else "?"
        val url = BuildConfig.TIPOS_TRAMITE_URL + "${separator}action=list&includeDeleted=${includeDeleted}"
        performJsonRequest(url = url, method = "GET")
    }

    suspend fun createTipoTramite(
        nombre: String,
        descripcion: String,
        duracionMin: Int
    ): ApiResponse = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("nombre", nombre)
            .put("descripcion", descripcion)
            .put("duracion_estimada_min", duracionMin)
        val url = BuildConfig.TIPOS_TRAMITE_URL + "?action=create"
        performJsonRequest(url = url, method = "POST", payload = payload)
    }

    suspend fun updateTipoTramite(
        id: Int,
        nombre: String,
        descripcion: String,
        duracionMin: Int
    ): ApiResponse = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("id", id)
            .put("nombre", nombre)
            .put("descripcion", descripcion)
            .put("duracion_estimada_min", duracionMin)
        val url = BuildConfig.TIPOS_TRAMITE_URL + "?action=update"
        performJsonRequest(url = url, method = "POST", payload = payload)
    }

    suspend fun changeEstadoTipoTramite(
        id: Int,
        estado: Int
    ): ApiResponse = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("id", id)
            .put("estado", estado)
        val url = BuildConfig.TIPOS_TRAMITE_URL + "?action=status"
        performJsonRequest(url = url, method = "POST", payload = payload)
    }

    private fun HttpURLConnection.readResponseBody(): String {
        val stream = if (responseCode in 200..299) {
            inputStream
        } else {
            errorStream
        }
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun performJsonRequest(
        url: String,
        method: String,
        payload: JSONObject? = null
    ): ApiResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.setRequestProperty("Accept", "application/json")
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { output ->
                    output.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val responseText = connection.readResponseBody()
            return ApiResponse(connection.responseCode, responseText)
        } finally {
            connection.disconnect()
        }
    }
}
