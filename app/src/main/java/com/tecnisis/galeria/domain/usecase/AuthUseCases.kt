package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.repository.AuthRepository
import com.tecnisis.galeria.domain.repository.SocioRepository
import com.tecnisis.galeria.domain.repository.AvisosRepository

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

/**
 * HU05: antes de cerrar la sesión borra la ficha del celular, mientras todavía sabe de quién es.
 * HU07: también invalida el token de notificaciones, para que no lleguen avisos de esta cuenta.
 */
class CerrarSesionUseCase(
    private val repo: AuthRepository,
    private val socios: SocioRepository,
    private val avisos: AvisosRepository
) {
    operator fun invoke() {
        avisos.olvidarToken()
        socios.borrarPerfilLocal()
        repo.cerrarSesion()
    }
}

class RegistrarCuentaUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(nombre: String, correo: String, contrasena: String) =
        repo.registrar(nombre.trim(), correo.trim(), contrasena)
}