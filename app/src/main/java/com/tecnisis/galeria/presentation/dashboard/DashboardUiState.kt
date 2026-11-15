package com.tecnisis.galeria.presentation.dashboard

import com.tecnisis.galeria.domain.model.DashboardSolicitudes
import com.tecnisis.galeria.domain.model.ExpertoDashboard
import com.tecnisis.galeria.domain.model.FiltroFecha
import com.tecnisis.galeria.domain.model.FiltrosDashboard
import com.tecnisis.galeria.domain.model.texto

/** Dashboard de solicitudes del Gerente. */
data class DashboardUiState(
    val cargando: Boolean = true,
    val filtros: FiltrosDashboard = FiltrosDashboard(),
    val dashboard: DashboardSolicitudes? = null,
    val mensajeError: String? = null,
    val eligiendoRango: Boolean = false
) {
    /** El chip "Rango…" muestra el rango elegido: "2 nov – 13 nov". */
    val textoChipRango: String
        get() = (filtros.fecha as? FiltroFecha.Rango)?.texto() ?: "Rango…"

    val textoTecnica: String
        get() = filtros.tecnica?.etiqueta ?: TODOS

    val textoSocio: String
        get() = filtros.socio ?: TODOS

    val textoExperto: String
        get() = filtros.expertoUid?.let { uid ->
            dashboard?.expertos?.firstOrNull { it.uid == uid }?.let(::etiquetaExperto)
        } ?: TODOS

    companion object {
        const val TODOS = "Todos"
    }
}

/** "Elena Torres Ugarte (inactivo)" en el desplegable de Experto. */
fun etiquetaExperto(experto: ExpertoDashboard): String =
    if (experto.activo) experto.nombre else "${experto.nombre} (inactivo)"