package com.tecnisis.galeria.domain.model

const val MAX_OBRAS_POR_SOLICITUD = 10

/** HU06: el grupo de obras que el socio envía junto para que el Anfitrión lo apruebe (HU07). */
data class SolicitudExhibicion(
    val id: String,
    val estado: EstadoSolicitud,
    val creadaEn: Long,
    val enviadaEn: Long?,
    val obras: List<Obra>,
    val motivoRechazo: String? = null, // HU07: el que escribió el Anfitrión al rechazar
    val resueltaEn: Long? = null       // HU07: cuándo se aprobó o rechazó
) {
    /** HU07: ya la aprobaron o rechazaron (el socio puede ver su documento). */
    val estaResuelta: Boolean
        get() = estado == EstadoSolicitud.APROBADA || estado == EstadoSolicitud.RECHAZADA
    /** Solo el borrador se edita: agregar, cambiar o borrar obras. */
    val esEditable: Boolean
        get() = estado == EstadoSolicitud.BORRADOR

    /** HU08: obras que ya tienen veredicto del Especialista de Arte. */
    val cantidadEvaluadas: Int
        get() = obras.count { it.estado.tieneVeredicto }

    /** HU08: aceptadas (también las que ya pasaron a "Lista para exhibición"). */
    val cantidadAceptadas: Int
        get() = obras.count { it.estado == EstadoObra.ACEPTADA || it.estado == EstadoObra.LISTA_EXHIBICION }

    val cantidadRechazadas: Int
        get() = obras.count { it.estado == EstadoObra.RECHAZADA }

    /**
     * HU08: la evaluación artística terminó cuando la solicitud está aprobada y ninguna obra sigue "en_evaluacion".
     * La solicitud no cambia de estado: esto se calcula con sus obras.
     */
    val evaluacionTerminada: Boolean
        get() = estado == EstadoSolicitud.APROBADA && obras.isNotEmpty() &&
                obras.none { it.estado == EstadoObra.EN_EVALUACION }

    /** Sí en un borrador con menos de 10 obras, o si esta ya se cerró (se arma una nueva). */
    val admiteNuevaObra: Boolean
        get() = if (estado.estaAbierta) esEditable && obras.size < MAX_OBRAS_POR_SOLICITUD else true
}