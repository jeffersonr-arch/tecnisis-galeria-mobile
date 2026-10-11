package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.repository.PostulacionRepository
import com.tecnisis.galeria.domain.repository.VerificacionScheduler

class EnviarPostulacionUseCase(
    private val repo: PostulacionRepository,
    private val scheduler: VerificacionScheduler
) {
    /** Guarda la postulación y deja programada su verificación automática (HU03). */
    suspend operator fun invoke(postulacion: Postulacion): Result<Unit> =
        repo.enviar(postulacion).map { uid -> scheduler.programar(uid) }
}

class ObservarMiPostulacionUseCase(private val repo: PostulacionRepository) {
    operator fun invoke() = repo.observarMiPostulacion()
}

class ObservarPostulacionesPorEstadoUseCase(private val repo: PostulacionRepository) {
    operator fun invoke(estado: EstadoPostulacion) = repo.observarPorEstado(estado)
}

class ObtenerPostulacionUseCase(private val repo: PostulacionRepository) {
    suspend operator fun invoke(uid: String) = repo.obtener(uid)
}

class ResolverPostulacionUseCase(private val repo: PostulacionRepository) {
    suspend operator fun invoke(uid: String, aprobar: Boolean, motivoRechazo: String?): Result<Unit> {
        if (!aprobar && motivoRechazo.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("El motivo de rechazo es obligatorio"))
        }
        return repo.resolver(uid, aprobar, motivoRechazo?.trim())
    }
}