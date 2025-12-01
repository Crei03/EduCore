package com.proyect.educore.model.repository

import com.proyect.educore.data.api.ApiService
import com.proyect.educore.model.TipoTramite
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface TipoTramiteListResult {
    data class Success(val tramites: List<TipoTramite>) : TipoTramiteListResult
    data class Error(val message: String) : TipoTramiteListResult
}

sealed interface TipoTramiteResult {
    data class Success(val tramite: TipoTramite, val message: String) : TipoTramiteResult
    data class Error(val message: String) : TipoTramiteResult
}

sealed interface EstadoOperacionResult {
    data class Success(val id: Int, val estado: Int, val message: String) : EstadoOperacionResult
    data class Error(val message: String) : EstadoOperacionResult
}

object TipoTramiteRepository {

    suspend fun obtenerTipos(includeDeleted: Boolean = false): TipoTramiteListResult {
        return try {

            // http://127.0.0.1/EduCore/backend/TiposTramite.php?action=list
            val response = ApiService.fetchTiposTramite(includeDeleted)
            if (response.code !in 200..299) {
                return TipoTramiteListResult.Error(response.body.extractMessage("No se pudieron obtener los trámites."))
            }
            val json = JSONObject(response.body)
            val data = json.optJSONArray("data") ?: JSONArray()
            val parsed = mutableListOf<TipoTramite>()
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                parseTipoTramite(item)?.let { parsed.add(it) }
            }
            TipoTramiteListResult.Success(parsed)
        } catch (e: Exception) {
            TipoTramiteListResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    /**
     * Obtiene la lista de tipos de trámite disponibles.
     * Este método retorna un List nullable para compatibilidad con versiones anteriores.
     * @return Lista de tipos de trámite o null si hay error
     */
    suspend fun getTiposTramite(includeDeleted: Boolean = false): List<TipoTramite>? {
        return when (val result = obtenerTipos(includeDeleted)) {
            is TipoTramiteListResult.Success -> result.tramites
            is TipoTramiteListResult.Error -> null
        }
    }

    suspend fun crearTipo(
        nombre: String,
        descripcion: String,
        duracionMin: Int
    ): TipoTramiteResult {
        return try {
            val response = ApiService.createTipoTramite(nombre, descripcion, duracionMin)
            if (response.code !in 200..299) {
                return TipoTramiteResult.Error(response.body.extractMessage("No se pudo registrar el trámite."))
            }
            val json = JSONObject(response.body)
            val tramite = json.optJSONObject("data")?.let { parseTipoTramite(it) }
            if (tramite != null) {
                TipoTramiteResult.Success(tramite, json.optString("message", "Trámite creado."))
            } else {
                TipoTramiteResult.Error("Respuesta inválida del servidor.")
            }
        } catch (e: Exception) {
            TipoTramiteResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    suspend fun actualizarTipo(
        id: Int,
        nombre: String,
        descripcion: String,
        duracionMin: Int
    ): TipoTramiteResult {
        return try {
            val response = ApiService.updateTipoTramite(id, nombre, descripcion, duracionMin)
            if (response.code !in 200..299) {
                return TipoTramiteResult.Error(response.body.extractMessage("No se pudo actualizar el trámite."))
            }
            val json = JSONObject(response.body)
            val tramite = json.optJSONObject("data")?.let { parseTipoTramite(it) }
            if (tramite != null) {
                TipoTramiteResult.Success(tramite, json.optString("message", "Trámite actualizado."))
            } else {
                TipoTramiteResult.Error("Respuesta inválida del servidor.")
            }
        } catch (e: Exception) {
            TipoTramiteResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    suspend fun cambiarEstado(id: Int, estado: Int): EstadoOperacionResult {
        return try {
            val response = ApiService.changeEstadoTipoTramite(id, estado)
            if (response.code !in 200..299) {
                return EstadoOperacionResult.Error(response.body.extractMessage("No se pudo actualizar el estado."))
            }
            val json = JSONObject(response.body)
            val updatedEstado = json.optJSONObject("data")?.optInt("activo", estado) ?: estado
            EstadoOperacionResult.Success(
                id = id,
                estado = updatedEstado,
                message = json.optString("message", "Estado actualizado.")
            )
        } catch (e: Exception) {
            EstadoOperacionResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    private fun parseTipoTramite(json: JSONObject): TipoTramite? {
        val id = json.optInt("id", -1)
        val nombre = json.optString("nombre")
        val descripcion = json.optString("descripcion", "")
        val duracion = json.optInt("duracion_estimada_min", -1)
        val activo = json.optInt("activo", -1)
        if (id == -1 || nombre.isBlank() || duracion <= 0 || activo !in 0..2) {
            return null
        }
        val creadoEn = json.optString("creado_en", "").toTimestampOrNow()
        val actualizadoEn = json.optString("actualizado_en", "").toTimestampOrNow()
        return TipoTramite(
            id = id,
            nombre = nombre,
            descripcion = descripcion,
            duracionEstimadaMin = duracion,
            activo = activo,
            creadoEn = creadoEn,
            actualizadoEn = actualizadoEn
        )
    }

    private fun String.toTimestampOrNow(): Long {
        if (isBlank()) return System.currentTimeMillis()
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            formatter.parse(this)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun String.extractMessage(fallback: String): String {
        if (isBlank()) return fallback
        return try {
            JSONObject(this).optString("message").ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }
}
