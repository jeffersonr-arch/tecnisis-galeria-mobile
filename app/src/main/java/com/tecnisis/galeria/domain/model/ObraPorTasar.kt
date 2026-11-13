package com.tecnisis.galeria.domain.model

/**
 * HU10: una obra aceptada que el Especialista de Ventas todavía no tasó (obras con estado "aceptada").
 * nombreSocio y numeroSocio los copia decidirSolicitud (HU08): Ventas no lee la ficha del socio.
 */
data class ObraPorTasar(
    val id: String,
    val solicitudId: String,
    val uid: String,                   // el socio dueño de la obra
    val titulo: String,
    val tecnica: Tecnica,
    val altoCm: Double,
    val anchoCm: Double,
    val profundidadCm: Double?,
    val fotoRuta: String,              // obras/{uid}/{idObra}.jpg en Storage
    val nombreSocio: String?,
    val numeroSocio: String?,
    val comentarioVeredicto: String?,  // el del Especialista de Arte al aceptar (opcional)
    val fechaVeredicto: Long?,
    val estado: EstadoObra
) {
    /** Solo una obra "aceptada" se puede tasar. */
    val porTasar: Boolean
        get() = estado == EstadoObra.ACEPTADA

    val nombreSocioVisible: String
        get() = nombreSocio?.takeIf { it.isNotBlank() } ?: SIN_DATO

    val numeroSocioVisible: String
        get() = numeroSocio?.takeIf { it.isNotBlank() } ?: SIN_DATO

    /** "María Elena López Díaz · SOC-0002"; "—" si no tiene ninguno de los dos. */
    val socioVisible: String
        get() = listOfNotNull(nombreSocio?.takeIf { it.isNotBlank() }, numeroSocio?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
            .ifBlank { SIN_DATO }

    /** "Óleo · 60 × 80 cm". */
    val detalle: String
        get() = "${tecnica.etiqueta} · ${medidasTexto(altoCm, anchoCm, profundidadCm)} cm"
}

/** Pestaña "Por tasar": solo las aceptadas, la del veredicto más antiguo primero (sin fecha, al final). */
fun obrasPorTasar(obras: List<ObraPorTasar>): List<ObraPorTasar> =
    obras.filter { it.porTasar }.sortedBy { it.fechaVeredicto ?: Long.MAX_VALUE }