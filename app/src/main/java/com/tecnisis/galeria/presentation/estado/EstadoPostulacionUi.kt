package com.tecnisis.galeria.presentation.estado

import com.tecnisis.galeria.domain.model.Postulacion

/** Lo que puede mostrar la pantalla en cada momento */
sealed interface EstadoPostulacionUi {
    data object Cargando : EstadoPostulacionUi
    data object SinPostulacion : EstadoPostulacionUi
    data class ConPostulacion(val postulacion: Postulacion) : EstadoPostulacionUi
    data class Error(val mensaje: String) : EstadoPostulacionUi
}