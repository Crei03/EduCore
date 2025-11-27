package com.proyect.educore.model.repository

import com.proyect.educore.data.api.ApiService
import com.proyect.educore.model.Turno
import org.json.JSONObject

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
     * @return El turno creado o null si hay error
     */
    suspend fun crearTurno(
        estudianteId: Long,
        tipoTramiteId: Int,
        observaciones: String = ""
    ): Turno? {
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

            if (response.code in 200..299) {
                val jsonObject = JSONObject(response.body)
                val success = jsonObject.optBoolean("success", false)
                println("🟢 [TurnoRepository] Success flag: $success")

                if (success) {
                    val dataObj = jsonObject.optJSONObject("data")
                    println("🟢 [TurnoRepository] Data object: $dataObj")

                    if (dataObj != null) {
                        val turno = parseTurnoFromJson(dataObj)
                        println("🟢 [TurnoRepository] Turno creado exitosamente: ${turno.id}")
                        turno
                    } else {
                        println("🔴 [TurnoRepository] Data object es null")
                        null
                    }
                } else {
                    val message = jsonObject.optString("message", "Sin mensaje")
                    println("🔴 [TurnoRepository] Success es false. Mensaje: $message")
                    null
                }
            } else {
                println("🔴 [TurnoRepository] Código de respuesta inválido: ${response.code}")
                null
            }
        } catch (e: Exception) {
            println("🔴 [TurnoRepository] Exception: ${e.message}")
            e.printStackTrace()
            null
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
            response.code in 200..299
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
            horaInicioAtencion = json.optString("hora_inicio_atencion", ""),
            horaFinAtencion = json.optString("hora_fin_atencion", ""),
            observaciones = json.optString("observaciones", ""),
            creadoEn = json.optString("creado_en", ""),
            actualizadoEn = json.optString("actualizado_en", ""),
            tipoTramiteNombre = if (json.has("tipo_tramite_nombre")) json.optString("tipo_tramite_nombre") else null,
            posicionEnFila = json.optInt("posicion_en_fila", 0),
            tiempoEstimadoMin = json.optInt("tiempo_estimado_min", 0)
        )
    }
}

