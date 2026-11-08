package com.tecnisis.galeria.presentation.evaluacion

import com.tecnisis.galeria.domain.model.EtiquetaDetectada
import com.tecnisis.galeria.domain.model.ObraAsignada
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.TipoVeredicto

/** Lo que muestra la tarjeta de ML Kit. */
sealed interface EstadoEtiquetas {
    data object Analizando : EstadoEtiquetas
    data class Listas(val etiquetas: List<EtiquetaDetectada>) : EstadoEtiquetas
    data object SinResultados : EstadoEtiquetas
    data object Error : EstadoEtiquetas
}

data class DetalleObraAsignadaUiState(
    val cargando: Boolean = true,
    val obra: ObraAsignada? = null,
    val ficha: PerfilEspecialista? = null,
    val etiquetas: EstadoEtiquetas = EstadoEtiquetas.Analizando,
    val errorCarga: String? = null,
    // Veredicto
    val dialogo: TipoVeredicto? = null,
    val comentario: String = "",
    val mostrarErrorComentario: Boolean = false,
    val guardando: Boolean = false,
    val errorDialogo: String? = null,
    val aviso: String? = null
) {
    /** Solo se evalúa una obra "en_evaluacion"; cuando llega el veredicto por Firestore, los botones desaparecen. */
    val puedeEvaluar: Boolean
        get() = obra?.porEvaluar == true
}