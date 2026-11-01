package com.tecnisis.galeria.domain.model

/** HU06: estados de la solicitud de exhibición. Los dos últimos los pone la HU07. */
enum class EstadoSolicitud(val valorFirestore: String, val etiqueta: String) {
    BORRADOR("borrador", "Borrador"),               // solo en el celular; se puede editar
    POR_ENVIAR("por_enviar", "Pendiente de envío"), // el socio pulsó Enviar; WorkManager espera internet
    ENVIADA("enviada", "En revisión"),              // ya está en Firestore
    APROBADA("aprobada", "Aprobada"),               // HU07
    RECHAZADA("rechazada", "Rechazada");            // HU07

    /** Abierta = mientras exista, el socio no puede armar otra. */
    val estaAbierta: Boolean
        get() = this == BORRADOR || this == POR_ENVIAR || this == ENVIADA

    companion object {
        fun desdeValor(valor: String?): EstadoSolicitud =
            entries.firstOrNull { it.valorFirestore == valor } ?: BORRADOR
    }
}