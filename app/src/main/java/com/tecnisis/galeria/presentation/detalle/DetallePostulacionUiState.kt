package com.tecnisis.galeria.presentation.detalle

import com.tecnisis.galeria.domain.model.DecisionPostulacion
import com.tecnisis.galeria.domain.model.Postulacion

data class DetallePostulacionUiState(
    val cargando: Boolean = true,
    val postulacion: Postulacion? = null,
    val errorCarga: String? = null,
    val decisionAbierta: DecisionPostulacion? = null,
    val mensaje: String = "",
    val errorMensaje: String? = null,
    val enviando: Boolean = false,
    val aviso: String? = null,
    val resuelta: Boolean = false
)