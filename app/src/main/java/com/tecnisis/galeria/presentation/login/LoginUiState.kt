package com.tecnisis.galeria.presentation.login

import com.tecnisis.galeria.domain.model.Usuario

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val errorCorreo: String? = null,
    val errorContrasena: String? = null,
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    val puedeUsarHuella: Boolean = false,
    val usuarioAutenticado: Usuario? = null
)