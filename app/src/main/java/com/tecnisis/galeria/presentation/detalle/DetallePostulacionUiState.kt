package com.tecnisis.galeria.presentation.detalle

import com.tecnisis.galeria.domain.model.Postulacion

data class DetallePostulacionUiState(
    val cargando: Boolean = true,
    val postulacion: Postulacion? = null,
    val errorCarga: String? = null
)