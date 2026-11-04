package com.tecnisis.galeria.presentation.obras

import com.tecnisis.galeria.domain.model.Obra
import com.tecnisis.galeria.domain.model.SolicitudExhibicion

data class MisObrasUiState(
    val cargando: Boolean = true,
    val solicitud: SolicitudExhibicion? = null,   // null = nunca armó una
    val hayConexion: Boolean = true,
    val confirmandoEnvio: Boolean = false,
    val enviando: Boolean = false,
    val mensaje: String? = null
) {
    val obras: List<Obra>
        get() = solicitud?.obras.orEmpty()

    val sinObras: Boolean
        get() = obras.isEmpty()
}