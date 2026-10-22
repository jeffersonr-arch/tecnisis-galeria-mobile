package com.tecnisis.galeria.domain.model

/** Lo que puede decidir el Anfitrión sobre una postulación (HU04). */
enum class DecisionPostulacion(val valorFuncion: String) {
    APROBAR("aprobar"),
    RECHAZAR("rechazar"),
    PEDIR_INFORMACION("pedir_informacion");

    /** Rechazar y pedir información siempre llevan texto; aprobar una observada exige anotar qué se corrigió. */
    fun requiereMensaje(estado: EstadoPostulacion): Boolean = when (this) {
        RECHAZAR, PEDIR_INFORMACION -> true
        APROBAR -> estado == EstadoPostulacion.OBSERVADA
    }
}

/** Observadas: las tres decisiones. Verificadas: aprobar o rechazar. Las demás ya no se resuelven. */
fun EstadoPostulacion.decisionesPermitidas(): List<DecisionPostulacion> = when (this) {
    EstadoPostulacion.OBSERVADA -> listOf(
        DecisionPostulacion.APROBAR,
        DecisionPostulacion.PEDIR_INFORMACION,
        DecisionPostulacion.RECHAZAR
    )
    EstadoPostulacion.VERIFICADA -> listOf(DecisionPostulacion.APROBAR, DecisionPostulacion.RECHAZAR)
    else -> emptyList()
}