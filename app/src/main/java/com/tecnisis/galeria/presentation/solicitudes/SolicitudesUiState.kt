package com.tecnisis.galeria.presentation.solicitudes

import com.tecnisis.galeria.domain.model.SolicitudResumen

enum class PestanaSolicitudes(val titulo: String, val textoVacio: String) {
    POR_REVISAR("Por revisar", "Cuando un socio envíe su solicitud de exhibición, aparecerá aquí al instante."),
    RESUELTAS("Resueltas", "Todavía no has aprobado ni rechazado ninguna solicitud.")
}

data class SolicitudesUiState(
    val cargando: Boolean = true,
    val pestana: PestanaSolicitudes = PestanaSolicitudes.POR_REVISAR,
    val porRevisar: List<SolicitudResumen> = emptyList(),
    val resueltas: List<SolicitudResumen> = emptyList(),
    val mensajeError: String? = null
) {
    val visibles: List<SolicitudResumen>
        get() = if (pestana == PestanaSolicitudes.POR_REVISAR) porRevisar else resueltas
}