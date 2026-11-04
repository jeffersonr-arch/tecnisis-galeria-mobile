package com.tecnisis.galeria.domain.model

/** HU06: una obra del socio, tal como está guardada en el celular. */
data class Obra(
    val id: String,
    val solicitudId: String,
    val titulo: String,
    val tecnica: Tecnica,
    val altoCm: Double,
    val anchoCm: Double,
    val profundidadCm: Double?,
    val fotoLocal: String,   // ruta del JPG reducido dentro de la app
    val fotoUrl: String?,    // null hasta que WorkManager la sube a Storage
    val nota: String,        // concepto o inspiración; puede estar vacía
    val estado: EstadoObra,
    val creadaEn: Long
) {
    val fotoSubida: Boolean
        get() = fotoUrl != null
}