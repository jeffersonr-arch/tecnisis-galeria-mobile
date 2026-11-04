package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.repository.EnvioSolicitudScheduler
import com.tecnisis.galeria.domain.repository.SocioRepository

class ObservarMiPerfilUseCase(private val repo: SocioRepository) {
    operator fun invoke() = repo.observarMiPerfil()
}

class SincronizarPerfilUseCase(private val repo: SocioRepository) {
    suspend operator fun invoke() = repo.sincronizar()
}

/**
 * HU05 + HU06: se llama al ser Socio Activo y cada vez que abre la app.
 * Sincroniza la ficha y vuelve a intentar las solicitudes que quedaron pendientes de envío.
 */
class ProgramarSincronizacionUseCase(
    private val repo: SocioRepository,
    private val envio: EnvioSolicitudScheduler
) {
    operator fun invoke() {
        repo.programarSincronizacion()
        envio.programar()
    }
}