package com.tecnisis.galeria.presentation.tasacion

import com.tecnisis.galeria.domain.model.LecturaPorcentaje
import com.tecnisis.galeria.domain.model.LecturaPvp
import com.tecnisis.galeria.domain.model.MENSAJE_PORCENTAJE
import com.tecnisis.galeria.domain.model.MontosTasacion
import com.tecnisis.galeria.domain.model.ObraPorTasar
import com.tecnisis.galeria.domain.model.leerPorcentaje
import com.tecnisis.galeria.domain.model.leerPvp
import com.tecnisis.galeria.domain.model.montosDe

/** Pantalla Tasar obra (los campos, el cálculo en vivo y el diálogo de confirmación). */
data class TasarObraUiState(
    val cargando: Boolean = true,
    val obra: ObraPorTasar? = null,
    val errorCarga: String? = null,
    val pvp: String = "",
    val porcentaje: String = "",
    // Diálogo
    val confirmando: Boolean = false,
    val guardando: Boolean = false,
    val errorDialogo: String? = null,
    val yaTasada: Boolean = false,          // la función respondió que ya se tasó: el diálogo solo deja volver
    val tituloGuardada: String? = null      // se guardó: la Route vuelve a Por tasar con el aviso
) {
    /** El mensaje debajo del PVP; null si está vacío o es válido (vacío no marca error). */
    val errorPvp: String?
        get() = (leerPvp(pvp) as? LecturaPvp.Invalido)?.error?.mensaje

    val errorPorcentaje: String?
        get() = if (leerPorcentaje(porcentaje) is LecturaPorcentaje.Invalido) MENSAJE_PORCENTAJE else null

    /** null mientras algún campo esté vacío o sea inválido: el cálculo muestra "—" y el botón se apaga. */
    val montos: MontosTasacion?
        get() = montosDe(pvp, porcentaje)

    val puedeGuardar: Boolean
        get() = obra != null && montos != null && !guardando
}