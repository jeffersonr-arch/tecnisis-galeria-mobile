package com.tecnisis.galeria.domain.model

/**
 * HU09: una obra rechazada, tal como la ve el Gerente en el histórico.
 * nombreSocio y numeroSocio los copia decidirSolicitud (HU08); evaluadaPorNombre lo guarda registrarVeredicto (HU09).
 * Las obras antiguas pueden no tenerlos: se muestra "—".
 */
data class ObraRechazada(
    val id: String,
    val titulo: String,
    val tecnica: Tecnica,
    val altoCm: Double,
    val anchoCm: Double,
    val profundidadCm: Double?,
    val nombreSocio: String?,
    val numeroSocio: String?,
    val evaluadaPorNombre: String?,
    val motivo: String,
    val fechaVeredicto: Long?,
    val documentoRechazo: List<String>
) {
    /** "María Elena López Díaz · SOC-0002"; "—" si no tiene ninguno de los dos. */
    val socioVisible: String
        get() = listOfNotNull(nombreSocio?.takeIf { it.isNotBlank() }, numeroSocio?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
            .ifBlank { SIN_DATO }

    val especialistaVisible: String
        get() = evaluadaPorNombre?.takeIf { it.isNotBlank() } ?: SIN_DATO

    /** "Textil · 120 × 90 cm". */
    val detalle: String
        get() = "${tecnica.etiqueta} · ${medidasTexto(altoCm, anchoCm, profundidadCm)} cm"

    val tieneDocumento: Boolean
        get() = documentoRechazo.isNotEmpty()
}

/** HU09: el histórico del Gerente, la del veredicto más reciente primero (sin fecha, al final). */
fun historicoRechazos(obras: List<ObraRechazada>): List<ObraRechazada> =
    obras.sortedByDescending { it.fechaVeredicto ?: 0L }