package com.tecnisis.galeria.presentation.asignadas

import com.tecnisis.galeria.domain.model.ObraAsignada

enum class PestanaAsignadas(val titulo: String, val textoVacio: String) {
    POR_EVALUAR(
        "Por evaluar",
        "Cuando el Anfitrión apruebe una solicitud y te asigne una obra, aparecerá aquí al instante."
    ),
    EVALUADAS("Evaluadas", "Todavía no registraste ningún veredicto.")
}

data class ObrasAsignadasUiState(
    val cargando: Boolean = true,
    val pestana: PestanaAsignadas = PestanaAsignadas.POR_EVALUAR,
    val porEvaluar: List<ObraAsignada> = emptyList(),
    val evaluadas: List<ObraAsignada> = emptyList(),
    val carga: Int? = null,          // obras sin veredicto según expertos/{uid}; null si no se pudo leer
    val mensajeError: String? = null
) {
    val visibles: List<ObraAsignada>
        get() = if (pestana == PestanaAsignadas.POR_EVALUAR) porEvaluar else evaluadas
}