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
        val connection = (URL(BuildConfig.LOGIN_URL).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val payload = JSONObject()
                .put("email", email)
                .put("password", password)
                .toString()
            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()

            ApiResponse(responseCode, responseText)
        } finally {
            connection.disconnect()
        }
    }
}
