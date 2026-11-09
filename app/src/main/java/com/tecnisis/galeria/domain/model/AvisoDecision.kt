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

/** HU08: título y texto de la notificación que recibe el socio cuando el Especialista evalúa una obra. */
fun avisoVeredicto(aceptada: Boolean, tituloObra: String): AvisoDecision {
    val obra = if (tituloObra.isBlank()) "Tu obra" else "Tu obra «$tituloObra»"
    return if (aceptada) {
        AvisoDecision("$obra fue aceptada", "Pasó a la evaluación económica. Toca para ver tus obras.")
    } else {
        AvisoDecision("$obra fue rechazada", "Toca para ver el motivo y tu documento.")
    }
}

/**
 * HU08: un id de notificación distinto por obra, para que el aviso de una obra no reemplace al de otra.
 * Siempre es 3000 o más: no choca con el aviso de la decisión de la solicitud (2001).
 */
fun idAvisoVeredicto(obraId: String): Int = 3_000 + obraId.hashCode().mod(1_000_000)