package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun iniciarSesion(correo: String, contrasena: String): Result<Usuario>
    suspend fun obtenerUsuarioActual(): Result<Usuario>
    fun haySesionActiva(): Boolean
    fun cerrarSesion()
    suspend fun registrar(nombre: String, correo: String, contrasena: String): Result<Usuario>
    /** Emite el usuario con sesión (o null) y vuelve a emitir si cambia su rol en Firestore */
    fun observarUsuarioActual(): Flow<Usuario?>
}