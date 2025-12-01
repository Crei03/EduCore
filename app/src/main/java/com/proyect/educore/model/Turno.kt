package com.proyect.educore.model

/**
 * Modelo que representa un Turno en el sistema.
 *
 * @param id Identificador único del turno
 * @param codigoTurno Código único generado para el turno (ej: T-023)
 * @param estudianteId ID del estudiante que solicita el turno
 * @param tipoTramiteId ID del tipo de trámite
 * @param estado Estado del turno (EN_COLA, ATENDIENDO, ATENDIDO, CANCELADO, AUSENTE)
 * @param horaSolicitud Fecha y hora en que se solicitó el turno
 * @param horaInicioAtencion Fecha y hora en que comienza la atención
 * @param horaFinAtencion Fecha y hora en que finaliza la atención
 * @param observaciones Notas adicionales sobre el turno
 * @param creadoEn Timestamp de creación
 * @param actualizadoEn Timestamp de última actualización
 */
data class Turno(
    val id: Long,
    val codigoTurno: String,
    val estudianteId: Long,
    val tipoTramiteId: Int,
    val estado: String, // EN_COLA, ATENDIENDO, ATENDIDO, CANCELADO, AUSENTE
    val horaSolicitud: String,
    val horaInicioAtencion: String? = null,
    val horaFinAtencion: String? = null,
    val observaciones: String? = null,
    val creadoEn: String = "",
    val actualizadoEn: String = "",
    // Campos adicionales para UI (no mapean directamente de BD)
    val tipoTramiteNombre: String? = null,
    val posicionEnFila: Int? = null,
    val tiempoEstimadoMin: Int? = null,
    val estudianteNombre: String? = null,
    val estudianteApellido: String? = null,
    val estudianteEmail: String? = null
)

/**
 * Estados posibles de un turno
 */
enum class EstadoTurno(val valor: String) {
    EN_COLA("EN_COLA"),
    ATENDIENDO("ATENDIENDO"),
    ATENDIDO("ATENDIDO"),
    CANCELADO("CANCELADO"),
    AUSENTE("AUSENTE")
}

