package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.repository.AuthRepository

class IniciarSesionUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(correo: String, contrasena: String) =
        repo.iniciarSesion(correo.trim(), contrasena)
}

class ObtenerUsuarioActualUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.obtenerUsuarioActual()
}

class HaySesionActivaUseCase(private val repo: AuthRepository) {
    operator fun invoke() = repo.haySesionActiva()
}

class CerrarSesionUseCase(private val repo: AuthRepository) {
    operator fun invoke() = repo.cerrarSesion()
}