package com.tecnisis.galeria.presentation.proyeccion

import com.tecnisis.galeria.domain.model.AgruparPor
import com.tecnisis.galeria.domain.model.ArchivoReporte
import com.tecnisis.galeria.domain.model.BarraProyeccion
import com.tecnisis.galeria.domain.model.ProyeccionEconomica
import com.tecnisis.galeria.domain.model.textoSoles

/** Proyección económica del Gerente. */
data class ProyeccionUiState(
    val cargando: Boolean = true,
    val proyeccion: ProyeccionEconomica? = null,
    val mensajeError: String? = null,
    val agrupar: AgruparPor = AgruparPor.OBRA,
    val exportando: Boolean = false,
    val archivoParaCompartir: ArchivoReporte? = null, // la Route abre el menú Compartir y avisa
    val avisoExportar: String? = null
) {
    val hayObras: Boolean
        get() = (proyeccion?.cantidad ?: 0) > 0

    /** Los botones se apagan sin obras tasadas y mientras se genera un archivo. */
    val puedeExportar: Boolean
        get() = hayObras && !exportando

    val barras: List<BarraProyeccion>
        get() = proyeccion?.barras(agrupar).orEmpty()

    val textoTotal: String
        get() = textoSoles(proyeccion?.totalGaleriaCentimos ?: 0L)

    val subtituloGrafico: String
        get() = when (agrupar) {
            AgruparPor.OBRA -> "Por obra · de mayor a menor (máximo 10 barras)"
            AgruparPor.SOCIO -> "Por socio · de mayor a menor"
        }

    /** Lo que lee TalkBack en lugar del gráfico. */
    val descripcionGrafico: String
        get() = "Ganancia de la galería " + (if (agrupar == AgruparPor.OBRA) "por obra" else "por socio") + ": " +
                barras.joinToString("; ") { "${it.etiqueta}, ${textoSoles(it.gananciaCentimos)}" }
}