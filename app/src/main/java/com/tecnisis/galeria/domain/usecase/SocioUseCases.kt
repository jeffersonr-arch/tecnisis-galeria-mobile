package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.repository.SocioRepository

class ObservarMiPerfilUseCase(private val repo: SocioRepository) {
    operator fun invoke() = repo.observarMiPerfil()
}

class SincronizarPerfilUseCase(private val repo: SocioRepository) {
    suspend operator fun invoke() = repo.sincronizar()
}

class ProgramarSincronizacionUseCase(private val repo: SocioRepository) {
    operator fun invoke() = repo.programarSincronizacion()
}