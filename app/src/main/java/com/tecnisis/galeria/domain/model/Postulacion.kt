package com.tecnisis.galeria.domain.model

data class Postulacion(
    val uid: String,
    val dni: String,
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val direccion: String,
    val sexo: String,
    val telefono: String,
    val correo: String,
    val estado: EstadoPostulacion = EstadoPostulacion.PENDIENTE_VERIFICACION,
    val motivo: String? = null,        // por qué quedó observada (HU03)
    val motivoRechazo: String? = null, // por qué la rechazó el Anfitrión (HU04)
    val fechaRegistro: Long? = null
) {
    val nombreCompleto: String
        get() = "$nombres $apellidoPaterno $apellidoMaterno".trim()
}