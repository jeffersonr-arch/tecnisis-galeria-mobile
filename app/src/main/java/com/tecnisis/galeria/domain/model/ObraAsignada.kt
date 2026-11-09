package com.tecnisis.galeria.domain.model

/** HU08: lo que se muestra cuando un dato no existe (por ejemplo, el socio en una obra aprobada antes de la HU08). */
const val SIN_DATO = "—"

/**
 * HU08: una obra que el Anfitrión asignó al Especialista de Arte (obras con expertoUid == su uid).
 * nombreSocio y numeroSocio los copia decidirSolicitud en la obra al aprobar: el Especialista no lee la ficha
 * del socio. Las obras aprobadas antes de la HU08 no los tienen (null).
 */
data class ObraAsignada(
    val id: String,
    val solicitudId: String,
    val titulo: String,
    val tecnica: Tecnica,
    val altoCm: Double,
    val anchoCm: Double,
    val profundidadCm: Double?,
    val nota: String,
    val fotoRuta: String,              // obras/{uid}/{idObra}.jpg en Storage
    val descripcionIa: String?,        // la de Gemini (HU07)
    val nombreSocio: String?,
    val numeroSocio: String?,
    val estado: EstadoObra,
    val fechaAsignacion: Long?,        // cuándo se aprobó la solicitud (decidirSolicitud)
    val fechaEnvio: Long?,
    val comentarioVeredicto: String?,  // null si no hay veredicto o si se aceptó sin comentario
    val fechaVeredicto: Long?
) {
    /** Solo una obra "en_evaluacion" se puede aceptar o rechazar. */
    val porEvaluar: Boolean
        get() = estado == EstadoObra.EN_EVALUACION

    /** Para ordenar: la fecha de asignación; si no la tiene, la de envío. */
    val fechaOrden: Long
        get() = fechaAsignacion ?: fechaEnvio ?: 0L

    val socioVisible: String
        get() = nombreSocio?.takeIf { it.isNotBlank() } ?: SIN_DATO

    val numeroSocioVisible: String
        get() = numeroSocio?.takeIf { it.isNotBlank() } ?: SIN_DATO
}

/** Pestaña "Por evaluar": solo "en_evaluacion", la asignada hace más tiempo primero (orden de llegada). */
fun obrasPorEvaluar(obras: List<ObraAsignada>): List<ObraAsignada> =
    obras.filter { it.porEvaluar }.sortedBy { it.fechaOrden }

/** Pestaña "Evaluadas": las que ya tienen veredicto, la del veredicto más reciente primero. */
fun obrasEvaluadas(obras: List<ObraAsignada>): List<ObraAsignada> =
    obras.filter { it.estado.tieneVeredicto }.sortedByDescending { it.fechaVeredicto ?: it.fechaOrden }