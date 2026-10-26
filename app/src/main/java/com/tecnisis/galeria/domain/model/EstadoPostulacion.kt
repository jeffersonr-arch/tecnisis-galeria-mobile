package com.tecnisis.galeria.domain.model

enum class EstadoPostulacion(val valorFirestore: String, val etiqueta: String) {
    PENDIENTE_VERIFICACION("pendiente_verificacion", "Pendiente de verificación"),
    VERIFICADA("verificada", "Verificada"),
    OBSERVADA("observada", "Observada"),
    APROBADA("aprobada", "Aprobada"),
    RECHAZADA("rechazada", "Rechazada");

    companion object {
        fun desdeFirestore(valor: String?): EstadoPostulacion =
            entries.firstOrNull { it.valorFirestore == valor } ?: PENDIENTE_VERIFICACION
    }
}