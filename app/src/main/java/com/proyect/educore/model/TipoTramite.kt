package com.proyect.educore.model

data class TipoTramite(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val duracionEstimadaMin: Int,
    val activo: Int,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)
