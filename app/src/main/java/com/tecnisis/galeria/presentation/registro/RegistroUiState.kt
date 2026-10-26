package com.tecnisis.galeria.presentation.registro

data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val errorNombre: String? = null,
    val errorCorreo: String? = null,
    val errorContrasena: String? = null,
    val errorConfirmacion: String? = null,
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    val registrado: Boolean = false
)