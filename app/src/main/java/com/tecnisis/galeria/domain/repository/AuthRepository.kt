package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.Usuario

interface AuthRepository {
    suspend fun iniciarSesion(correo: String, contrasena: String): Result<Usuario>
    suspend fun obtenerUsuarioActual(): Result<Usuario>
    fun haySesionActiva(): Boolean
    fun cerrarSesion()
}