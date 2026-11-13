package com.tecnisis.galeria.presentation.tasacion

import com.tecnisis.galeria.domain.model.ObraTasada

/** Pestaña Tasadas del Especialista de Ventas. */
data class TasadasUiState(
    val cargando: Boolean = true,
    val obras: List<ObraTasada> = emptyList(),   // la más reciente primero (lo ordena el caso de uso)
    val mensajeError: String? = null,
    val abriendo: String? = null,                // id de la obra cuyo PDF se está descargando
    val pdfParaAbrir: String? = null,            // ruta del PDF listo para abrir (lo consume la Route)
    val aviso: String? = null
) {
    val vacia: Boolean
        get() = !cargando && mensajeError == null && obras.isEmpty()

    /** "3 obras · la más reciente primero". */
    val resumen: String
        get() = when (obras.size) {
            0 -> "Se actualiza en tiempo real"
            1 -> "1 obra"
            else -> "${obras.size} obras · la más reciente primero"
        }
}