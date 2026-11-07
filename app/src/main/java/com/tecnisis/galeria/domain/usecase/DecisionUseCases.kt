package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.model.DecisionSolicitud
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.TipoDecision
import com.tecnisis.galeria.domain.model.asignacionesDe
import com.tecnisis.galeria.domain.model.motivoValido
import com.tecnisis.galeria.domain.repository.AvisosRepository
import com.tecnisis.galeria.domain.repository.DecisionRepository
import com.tecnisis.galeria.domain.repository.SeguimientoRepository

/** HU07: solo aprueba si TODAS las obras tienen un Experto válido; si no, ni siquiera llama al repositorio. */
class AprobarSolicitudUseCase(private val repo: DecisionRepository) {
    suspend operator fun invoke(
        solicitudId: String,
        obras: List<ObraEnRevision>,
        expertos: List<PerfilEspecialista>,
        seleccion: Map<String, String?>
    ): Result<Unit> {
        val asignaciones = asignacionesDe(obras, expertos, seleccion)
            ?: return Result.failure(DecisionError.FaltaExperto)
        return repo.decidir(DecisionSolicitud(solicitudId, TipoDecision.APROBAR, asignaciones, motivo = ""))
    }
}

/** HU07: el motivo es obligatorio (1 a 500 caracteres) y se guarda sin espacios en los extremos. */
class RechazarSolicitudUseCase(private val repo: DecisionRepository) {
    suspend operator fun invoke(solicitudId: String, motivo: String): Result<Unit> {
        if (!motivoValido(motivo)) return Result.failure(DecisionError.MotivoInvalido)
        return repo.decidir(DecisionSolicitud(solicitudId, TipoDecision.RECHAZAR, emptyMap(), motivo.trim()))
    }
}

class DescargarDocumentoUseCase(private val repo: DecisionRepository) {
    suspend operator fun invoke(solicitudId: String, copia: CopiaDocumento) =
        repo.descargarDocumento(solicitudId, copia)
}

/** Lo usa el worker de WorkManager. */
class SincronizarSolicitudesUseCase(private val repo: SeguimientoRepository) {
    suspend operator fun invoke() = repo.sincronizar()
}

/** Lo usa el servicio de notificaciones cuando llega la decisión. */
class ProgramarSeguimientoUseCase(private val repo: SeguimientoRepository) {
    operator fun invoke() = repo.programarSincronizacion()
}

/** Lo usa el servicio de notificaciones cuando FCM entrega un token nuevo. */
class GuardarTokenUseCase(private val repo: AvisosRepository) {
    operator fun invoke(token: String) = repo.guardarToken(token)
}