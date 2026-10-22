package com.tecnisis.galeria.presentation.bandeja

import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion

enum class PestanaBandeja(
    val titulo: String,
    val estado: EstadoPostulacion,
    val ayuda: String,
    val textoVacio: String
) {
    OBSERVADAS(
        titulo = "Observadas",
        estado = EstadoPostulacion.OBSERVADA,
        ayuda = "Requieren tu intervención: revisa el motivo antes de decidir.",
        textoVacio = "No hay postulaciones observadas por resolver."
    ),
    VERIFICADAS(
        titulo = "Verificadas",
        estado = EstadoPostulacion.VERIFICADA,
        ayuda = "Sus datos coinciden con RENIEC; solo falta tu aprobación final.",
        textoVacio = "No hay postulaciones verificadas pendientes."
    )
}

data class BandejaUiState(
    val cargando: Boolean = true,
    val pestana: PestanaBandeja = PestanaBandeja.OBSERVADAS,
    val observadas: List<Postulacion> = emptyList(),
    val verificadas: List<Postulacion> = emptyList(),
    val mensajeError: String? = null
) {
    val visibles: List<Postulacion>
        get() = listaDe(pestana)

    fun cantidad(p: PestanaBandeja): Int = listaDe(p).size

    private fun listaDe(p: PestanaBandeja): List<Postulacion> = when (p) {
        PestanaBandeja.OBSERVADAS -> observadas
        PestanaBandeja.VERIFICADAS -> verificadas
    }
}