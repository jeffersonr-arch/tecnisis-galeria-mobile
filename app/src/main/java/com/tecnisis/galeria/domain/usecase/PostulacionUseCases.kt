package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.DecisionPostulacion
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.model.PostulacionError
import com.tecnisis.galeria.domain.model.decisionesPermitidas
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

/** HU04: Convalida la decisión del Anfitrión antes de llamar a la Cloud Function. */
class ResolverPostulacionUseCase(private val repo: PostulacionRepository) {
    suspend operator fun invoke(
        postulacion: Postulacion,
        decision: DecisionPostulacion,
        mensaje: String?
    ): Result<Unit> {
        if (decision !in postulacion.estado.decisionesPermitidas()) {
            return Result.failure(PostulacionError.DecisionNoPermitida)
        }
        val texto = mensaje?.trim()?.takeIf { it.isNotEmpty() }
        if (texto == null && decision.requiereMensaje(postulacion.estado)) {
            return Result.failure(PostulacionError.MensajeObligatorio)
        }
        return repo.resolver(postulacion.uid, decision, texto)
    }
}