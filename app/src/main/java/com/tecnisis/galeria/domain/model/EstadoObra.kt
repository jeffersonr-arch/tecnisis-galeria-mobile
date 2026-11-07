package com.tecnisis.galeria.domain.model

/** Estados de cada obra en todo el proceso de la galería (HU06 a HU10). */
enum class EstadoObra(val valorFirestore: String, val etiqueta: String) {
    BORRADOR("borrador", "Borrador"),                         // HU06
    ENVIADA("enviada", "Enviada"),                            // HU06
    EN_EVALUACION("en_evaluacion", "En evaluación"),          // HU07: la solicitud se aprobó (ya tiene Experto)
    NO_ADMITIDA("no_admitida", "No admitida"),                // HU07: la solicitud se rechazó
    ACEPTADA("aceptada", "Aceptada"),                         // HU08
    RECHAZADA("rechazada", "Rechazada"),                      // HU08
    LISTA_EXHIBICION("lista_exhibicion", "Lista para exhibición"); // HU10

    companion object {
        fun desdeValor(valor: String?): EstadoObra =
            entries.firstOrNull { it.valorFirestore == valor } ?: BORRADOR
    }
}