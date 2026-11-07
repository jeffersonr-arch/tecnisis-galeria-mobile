package com.tecnisis.galeria.domain.model

/** HU07: minutos tras los cuales un análisis "en curso" se da por abandonado (la función dura como máximo 2). */
const val MINUTOS_BLOQUEO_ANALISIS = 3L

/** HU07: estado del análisis con IA de una solicitud (campo "analisis"; si no existe, PENDIENTE). */
enum class EstadoAnalisis(val valorFirestore: String?) {
    PENDIENTE(null),
    EN_CURSO("en_curso"),
    LISTO("listo"),
    ERROR("error");

    companion object {
        fun desdeValor(valor: String?): EstadoAnalisis =
            entries.firstOrNull { it.valorFirestore == valor } ?: PENDIENTE
    }
}

/** Un Experto que domina la técnica de la obra y su afinidad con ella (0 a 100), calculada por la función. */
data class CandidatoExperto(val uid: String, val afinidad: Int)

/** Lo que ve el Anfitrión de cada obra al revisar la solicitud. */
data class ObraEnRevision(
    val id: String,
    val titulo: String,
    val tecnica: Tecnica,
    val altoCm: Double,
    val anchoCm: Double,
    val profundidadCm: Double?,
    val nota: String,
    val fotoRuta: String,                      // obras/{uid}/{idObra}.jpg en Storage
    val descripcionIa: String?,                // null hasta que Gemini la describe
    val candidatos: List<CandidatoExperto>,    // de mayor a menor afinidad
    val recomendadoUid: String?,               // null = ningún Experto disponible
    val expertoAsignadoUid: String? = null,    // Parte 2: el que quedó al aprobar (puede no ser el recomendado)
    val estado: EstadoObra = EstadoObra.ENVIADA // HU08: en evaluación, aceptada o rechazada (el Anfitrión ve el veredicto)
)

data class SolicitudEnRevision(
    val id: String,
    val uidSocio: String,
    val nombreSocio: String,
    val numeroSocio: String,
    val estado: EstadoSolicitud,
    val analisis: EstadoAnalisis,
    val analisisDesde: Long?,                  // hora del servidor en que empezó el análisis
    val fechaEnvio: Long?,
    val obras: List<ObraEnRevision>,
    val motivoRechazo: String? = null,         // Parte 2: solo si se rechazó
    val fechaResolucion: Long? = null          // Parte 2: cuándo se aprobó o rechazó
)

/** Una obra dentro de la tarjeta de la lista (solo lo necesario para la miniatura). */
data class ObraResumen(val id: String, val tecnica: Tecnica, val fotoRuta: String)

data class SolicitudResumen(
    val id: String,
    val nombreSocio: String,
    val numeroSocio: String,
    val estado: EstadoSolicitud,
    val analisis: EstadoAnalisis,
    val obrasSinExperto: Int?,
    val fechaEnvio: Long?,
    val obras: List<ObraResumen>
)

/** Chip de cada tarjeta de la lista del Anfitrión. */
enum class EtapaRevision(val etiqueta: String) {
    POR_ANALIZAR("Por analizar"),
    ANALIZANDO("Analizando"),
    LISTA("Lista para decidir"),
    FALTA_EXPERTO("Falta Experto"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada")
}

fun SolicitudResumen.etapa(): EtapaRevision = when {
    estado == EstadoSolicitud.APROBADA -> EtapaRevision.APROBADA
    estado == EstadoSolicitud.RECHAZADA -> EtapaRevision.RECHAZADA
    analisis == EstadoAnalisis.EN_CURSO -> EtapaRevision.ANALIZANDO
    analisis == EstadoAnalisis.LISTO ->
        if ((obrasSinExperto ?: 0) > 0) EtapaRevision.FALTA_EXPERTO else EtapaRevision.LISTA
    else -> EtapaRevision.POR_ANALIZAR // pendiente o con error: hay que (volver a) analizar
}

/**
 * Solo decide qué muestra la pantalla: con más de 3 minutos "en curso" aparece "Reintentar".
 * Quien desbloquea de verdad es la función, con la hora del servidor.
 */
fun analisisVencido(desde: Long?, ahora: Long): Boolean =
    desde == null || ahora - desde > MINUTOS_BLOQUEO_ANALISIS * 60_000