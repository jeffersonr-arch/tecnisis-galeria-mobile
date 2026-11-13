package com.tecnisis.galeria.presentation.tasacion

import com.tecnisis.galeria.domain.model.ObraPorTasar

/** Pestaña Por tasar del Especialista de Ventas. */
data class PorTasarUiState(
    val cargando: Boolean = true,
    val obras: List<ObraPorTasar> = emptyList(),   // la del veredicto más antiguo primero
    val mensajeError: String? = null
) {
    val vacia: Boolean
        get() = !cargando && mensajeError == null && obras.isEmpty()

    /** "3 obras aceptadas · la de veredicto más antiguo primero". */
    val resumen: String
        get() = when (obras.size) {
            0 -> "Obras aceptadas · se actualiza en tiempo real"
            1 -> "1 obra aceptada"
            else -> "${obras.size} obras aceptadas · la de veredicto más antiguo primero"
        }
}