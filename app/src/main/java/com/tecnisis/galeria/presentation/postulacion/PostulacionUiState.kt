package com.tecnisis.galeria.presentation.postulacion

enum class Campo { DNI, NOMBRES, APELLIDO_PATERNO, APELLIDO_MATERNO, DIRECCION, SEXO, TELEFONO, CORREO }

data class PostulacionUiState(
    val dni: String = "",
    val nombres: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val direccion: String = "",
    val sexo: String = "",
    val telefono: String = "",
    val correo: String = "",
    val errores: Map<Campo, String> = emptyMap(),
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    val enviada: Boolean = false
)