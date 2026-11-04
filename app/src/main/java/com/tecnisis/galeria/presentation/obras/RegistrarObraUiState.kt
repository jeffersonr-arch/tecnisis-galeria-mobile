package com.tecnisis.galeria.presentation.obras

import com.tecnisis.galeria.domain.model.CampoObra
import com.tecnisis.galeria.domain.model.Tecnica

data class RegistrarObraUiState(
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val titulo: String = "",
    val tecnica: Tecnica? = null,
    val alto: String = "",
    val ancho: String = "",
    val profundidad: String = "",
    val fotoLocal: String? = null,
    val preparandoFoto: Boolean = false,
    val nota: String = "",
    val dictando: Boolean = false,
    val avisoVoz: AvisoVoz? = null,
    val errores: Set<CampoObra> = emptySet(),
    val guardando: Boolean = false,
    val confirmandoEliminar: Boolean = false,
    val mensaje: String? = null,
    val terminado: Boolean = false   // guardó o eliminó: hay que volver a Mis obras
) {
    fun tieneError(campo: CampoObra) = campo in errores
}

/** Todo lo que el formulario puede pedir. En la Preview se quedan vacías. */
data class AccionesRegistrarObra(
    val onVolver: () -> Unit = {},
    val onTituloChange: (String) -> Unit = {},
    val onTecnicaChange: (Tecnica) -> Unit = {},
    val onAltoChange: (String) -> Unit = {},
    val onAnchoChange: (String) -> Unit = {},
    val onProfundidadChange: (String) -> Unit = {},
    val onNotaChange: (String) -> Unit = {},
    val onTomarFoto: () -> Unit = {},
    val onElegirGaleria: () -> Unit = {},
    val onDictar: () -> Unit = {},
    val onDetenerDictado: () -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onPedirEliminar: () -> Unit = {},
    val onCancelarEliminar: () -> Unit = {},
    val onConfirmarEliminar: () -> Unit = {}
)