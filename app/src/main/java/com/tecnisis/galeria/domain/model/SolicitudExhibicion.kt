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

    /** Sí en un borrador con menos de 10 obras, o si esta ya se cerró (se arma una nueva). */
    val admiteNuevaObra: Boolean
        get() = if (estado.estaAbierta) esEditable && obras.size < MAX_OBRAS_POR_SOLICITUD else true
}