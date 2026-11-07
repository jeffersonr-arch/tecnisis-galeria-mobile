package com.tecnisis.galeria.domain.model

/** HU07: dato que lleva la notificación para que la app abra Mis obras al tocarla. */
const val EXTRA_ABRIR_MIS_OBRAS = "abrir_mis_obras"

/** Título y texto de la notificación que recibe el socio cuando el Anfitrión decide. */
data class AvisoDecision(val titulo: String, val texto: String)

fun avisoDecision(aprobada: Boolean, cantidadObras: Int): AvisoDecision =
    if (aprobada) {
        AvisoDecision(
            "Tu solicitud fue aprobada",
            if (cantidadObras == 1) "Tu obra pasó a evaluación artística. Toca para ver tu documento."
            else "Tus $cantidadObras obras pasaron a evaluación artística. Toca para ver tu documento."
        )
    } else {
        AvisoDecision(
            "Tu solicitud fue rechazada",
            "El Anfitrión indicó el motivo. Toca para verlo y armar una nueva solicitud."
        )
    }