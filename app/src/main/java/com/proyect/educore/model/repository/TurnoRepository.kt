package com.proyect.educore.model.repository

import com.proyect.educore.data.api.ApiService
import com.proyect.educore.model.Turno
import org.json.JSONObject

sealed interface TurnoPanelResult {
    data class Success(val turnos: List<Turno>) : TurnoPanelResult
    data class Error(val message: String) : TurnoPanelResult
}

sealed interface TurnoOperacionResult {
    data class Success(val turno: Turno, val message: String) : TurnoOperacionResult
    data class Error(val message: String) : TurnoOperacionResult
}

/**
 * Repositorio para gestionar las operaciones de Turnos.
 * Maneja la comunicación con el backend y la transformación de datos.
 */
class TurnoRepository {

    /**
     * Obtiene la lista de turnos del estudiante.
     * @param estudianteId ID del estudiante
     * @param estado Estado opcional para filtrar (EN_COLA, ATENDIDO, etc.)
     * @return Lista de turnos o null si hay error
     */
    suspend fun getTurnosEstudiante(
        estudianteId: Long,
        estado: String? = null
    ): List<Turno>? {
        return try {
            val response = ApiService.fetchTurnosEstudiante(estudianteId, estado)
            if (response.code in 200..299) {
                val jsonObject = JSONObject(response.body)
                if (jsonObject.optBoolean("success", false)) {
                    val dataArray = jsonObject.optJSONArray("data")
                    if (dataArray != null) {
                        (0 until dataArray.length()).map { index ->
                            parseTurnoFromJson(dataArray.getJSONObject(index))
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene el turno actual del estudiante (en cola o siendo atendido).
     * @param estudianteId ID del estudiante
     * @return El turno actual o null si no hay
     */
    suspend fun getTurnoActual(estudianteId: Long): Turno? {
        return try {
            val response = ApiService.fetchTurnoActual(estudianteId)
            if (response.code in 200..299) {
                val jsonObject = JSONObject(response.body)
                if (jsonObject.optBoolean("success", false)) {
                    val dataObj = jsonObject.optJSONObject("data")
                    if (dataObj != null) {
                        parseTurnoFromJson(dataObj)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calcula el tiempo estimado de espera para un tipo de trámite.
     * @param tipoTramiteId ID del tipo de trámite
     * @return Minutos estimados de espera
     */
    suspend fun getTiempoEstimado(tipoTramiteId: Int): Int {
        return try {
            val response = ApiService.fetchTiempoEstimado(tipoTramiteId)
            if (response.code in 200..299) {
                val jsonObject = JSONObject(response.body)
                jsonObject.optInt("data", 0)
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Crea un nuevo turno para el estudiante.
     * @param estudianteId ID del estudiante
     * @param tipoTramiteId ID del tipo de trámite
     * @param observaciones Observaciones opcionales del turno
     * @return Resultado con el turno creado o el mensaje de error
     */
    suspend fun crearTurno(
        estudianteId: Long,
        tipoTramiteId: Int,
        observaciones: String = ""
    ): TurnoOperacionResult {
        return try {
            println("🟢 [TurnoRepository] Creando turno - estudianteId: $estudianteId, tipoTramiteId: $tipoTramiteId")
            println("🟢 [TurnoRepository] Observaciones: $observaciones")

            val response = ApiService.createTurno(
                estudianteId = estudianteId,
                tipoTramiteId = tipoTramiteId,
                estado = "EN_COLA",
                observaciones = observaciones
            )

            println("🟢 [TurnoRepository] Response code: ${response.code}")
            println("🟢 [TurnoRepository] Response body: ${response.body}")

            val jsonObject = try {
                JSONObject(response.body)
            } catch (e: Exception) {
                null
            }

            if (response.code in 200..299) {
                if (jsonObject?.optBoolean("success", false) == true) {
                    val dataObj = jsonObject.optJSONObject("data")
                    println("🟢 [TurnoRepository] Data object: $dataObj")

                    if (dataObj != null) {
                        val turno = parseTurnoFromJson(dataObj)
                        println("🟢 [TurnoRepository] Turno creado exitosamente: ${turno.id}")
                        return TurnoOperacionResult.Success(
                            turno,
                            jsonObject.optString("message", "Turno creado exitosamente.")
                        )
                    }
                    println("🔴 [TurnoRepository] Data object es null")
                    return TurnoOperacionResult.Error("Respuesta inválida del servidor.")
                }
                val message = jsonObject?.optString("message", "No se pudo crear el turno.")
                    ?: "No se pudo crear el turno."
                println("🔴 [TurnoRepository] Success es false. Mensaje: $message")
                return TurnoOperacionResult.Error(message)
            }

            println("🔴 [TurnoRepository] Código de respuesta inválido: ${response.code}")
            TurnoOperacionResult.Error(response.body.extractMessage("No se pudo crear el turno."))
        } catch (e: Exception) {
            println("🔴 [TurnoRepository] Exception: ${e.message}")
            e.printStackTrace()
            TurnoOperacionResult.Error("Error inesperado: " + (e.message ?: "intenta más tarde."))
        }
    }

    /**
     * Cancela un turno existente.
     * @param turnoId ID del turno a cancelar
     * @return true si se canceló exitosamente
     */
    suspend fun cancelarTurno(turnoId: Long): Boolean {
        return try {
            val response = ApiService.cancelarTurno(turnoId)
            if (response.code !in 200..299) return false
            val json = JSONObject(response.body)
            json.optBoolean("success", true)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Obtiene la posición del turno en la fila.
     * @param turnoId ID del turno
     * @return Número de posición en la fila
     */
    suspend fun getPosicionEnFila(turnoId: Long): Int {
        return try {
            val response = ApiService.fetchPosicionEnFila(turnoId)
            if (response.code in 200..299) {
                val jsonObject = JSONObject(response.body)
                jsonObject.optInt("position", 0)
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Lista los turnos del día para el panel de secretaría.
     */
    suspend fun obtenerTurnosPanel(): TurnoPanelResult {
        return try {
            val response = ApiService.fetchTurnosDelDiaEnCola()
            if (response.code !in 200..299) {
                return TurnoPanelResult.Error(response.body.extractMessage("No se pudo cargar la cola de turnos."))
            }
            val jsonObject = JSONObject(response.body)
            if (!jsonObject.optBoolean("success", false)) {
                return TurnoPanelResult.Error(jsonObject.optString("message", "No se pudo cargar la cola de turnos."))
            }
            val dataArray = jsonObject.optJSONArray("data")
            if (dataArray == null) {
                TurnoPanelResult.Success(emptyList())
            } else {
                val turnos = mutableListOf<Turno>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.optJSONObject(i) ?: continue
                    turnos.add(parseTurnoFromJson(item))
                }
                TurnoPanelResult.Success(turnos)
            }
        } catch (e: Exception) {
            TurnoPanelResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    /**
     * Llama al siguiente turno en cola y lo marca como ATENDIENDO.
     */
    suspend fun llamarSiguienteTurno(): TurnoOperacionResult {
        return try {
            val response = ApiService.callNextTurno()
            if (response.code !in 200..299) {
                return TurnoOperacionResult.Error(response.body.extractMessage("No se pudo llamar al siguiente turno."))
            }
            val jsonObject = JSONObject(response.body)
            if (!jsonObject.optBoolean("success", false)) {
                return TurnoOperacionResult.Error(jsonObject.optString("message", "No se pudo llamar al siguiente turno."))
            }
            val turno = jsonObject.optJSONObject("data")?.let { parseTurnoFromJson(it) }
                ?: return TurnoOperacionResult.Error("Respuesta inválida del servidor.")
            TurnoOperacionResult.Success(turno, jsonObject.optString("message", "Turno llamado."))
        } catch (e: Exception) {
            TurnoOperacionResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    /**
     * Finaliza la atención y marca el turno como ATENDIDO.
     */
    suspend fun finalizarAtencion(turnoId: Long): TurnoOperacionResult {
        return try {
            val response = ApiService.finalizarAtencion(turnoId)
            if (response.code !in 200..299) {
                return TurnoOperacionResult.Error(response.body.extractMessage("No se pudo finalizar la atención."))
            }
            val jsonObject = JSONObject(response.body)
            if (!jsonObject.optBoolean("success", false)) {
                return TurnoOperacionResult.Error(jsonObject.optString("message", "No se pudo finalizar la atención."))
            }
            val turno = jsonObject.optJSONObject("data")?.let { parseTurnoFromJson(it) }
                ?: return TurnoOperacionResult.Error("Respuesta inválida del servidor.")
            TurnoOperacionResult.Success(turno, jsonObject.optString("message", "Atención finalizada."))
        } catch (e: Exception) {
            TurnoOperacionResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    /**
     * Marca un turno como cancelado/ausente.
     */
    suspend fun marcarAusente(turnoId: Long, motivo: String = ""): TurnoOperacionResult {
        return try {
            val response = ApiService.marcarAusente(turnoId, motivo)
            if (response.code !in 200..299) {
                return TurnoOperacionResult.Error(response.body.extractMessage("No se pudo marcar el turno como cancelado."))
            }
            val jsonObject = JSONObject(response.body)
            if (!jsonObject.optBoolean("success", false)) {
                return TurnoOperacionResult.Error(jsonObject.optString("message", "No se pudo marcar el turno como cancelado."))
            }
            val turno = jsonObject.optJSONObject("data")?.let { parseTurnoFromJson(it) }
                ?: return TurnoOperacionResult.Error("Respuesta inválida del servidor.")
            TurnoOperacionResult.Success(turno, jsonObject.optString("message", "Turno cancelado."))
        } catch (e: Exception) {
            TurnoOperacionResult.Error("Error de red: " + (e.localizedMessage ?: "intenta más tarde."))
        }
    }

    /**
     * Parsea un objeto JSON a una entidad Turno.
     */
    private fun parseTurnoFromJson(json: JSONObject): Turno {
        return Turno(
            id = json.optLong("id", 0),
            codigoTurno = json.optString("codigo_turno", ""),
            estudianteId = json.optLong("estudiante_id", 0),
            tipoTramiteId = json.optInt("tipo_tramite_id", 0),
            estado = json.optString("estado", "EN_COLA"),
            horaSolicitud = json.optString("hora_solicitud", ""),
            horaInicioAtencion = json.optStringOrNull("hora_inicio_atencion"),
            horaFinAtencion = json.optStringOrNull("hora_fin_atencion"),
            observaciones = json.optStringOrNull("observaciones"),
            creadoEn = json.optString("creado_en", ""),
            actualizadoEn = json.optString("actualizado_en", ""),
            tipoTramiteNombre = json.optStringOrNull("tipo_tramite_nombre"),
            posicionEnFila = json.optInt("posicion_en_fila", 0).takeIf { json.has("posicion_en_fila") },
            tiempoEstimadoMin = json.optInt("tiempo_estimado_min", 0).takeIf { json.has("tiempo_estimado_min") },
            estudianteNombre = json.optStringOrNull("estudiante_nombre"),
            estudianteApellido = json.optStringOrNull("estudiante_apellido"),
            estudianteEmail = json.optStringOrNull("estudiante_email")
        )
    }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    val value = optString(key, "")
    return value.takeIf { it.isNotBlank() }
}

private fun String.extractMessage(fallback: String): String {
    if (isBlank()) return fallback
    return try {
        JSONObject(this).optString("message", fallback).ifBlank { fallback }
    } catch (_: Exception) {
        fallback
    }
}

