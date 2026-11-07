package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.EtiquetaDetectada
import com.tecnisis.galeria.domain.model.EvaluacionError
import com.tecnisis.galeria.domain.model.TipoVeredicto
import com.tecnisis.galeria.domain.model.comentarioValido
import com.tecnisis.galeria.domain.model.etiquetasVisibles
import com.tecnisis.galeria.domain.repository.EtiquetadoRepository
import com.tecnisis.galeria.domain.repository.EvaluacionRepository

/** HU08: casos de uso del Especialista de Arte. */
class ObservarObrasAsignadasUseCase(private val repo: EvaluacionRepository) {
    operator fun invoke() = repo.observarAsignadas()
}

class ObservarObraAsignadaUseCase(private val repo: EvaluacionRepository) {
    operator fun invoke(id: String) = repo.observarObra(id)
}

class ObservarMiFichaExpertoUseCase(private val repo: EvaluacionRepository) {
    operator fun invoke() = repo.observarMiFicha()
}

/** Rechazar exige comentario (1 a 500); aceptar lo deja opcional. Si no cumple, ni siquiera llama a la función. */
class RegistrarVeredictoUseCase(private val repo: EvaluacionRepository) {
    suspend operator fun invoke(obraId: String, tipo: TipoVeredicto, comentario: String): Result<Unit> {
        if (!comentarioValido(tipo, comentario)) return Result.failure(EvaluacionError.ComentarioInvalido)
        return repo.registrarVeredicto(obraId, tipo, comentario.trim())
    }
}

/** ML Kit da las etiquetas en inglés y con cualquier confianza; aquí se aplican el umbral, el máximo y el diccionario. */
class EtiquetarFotoUseCase(private val repo: EtiquetadoRepository) {
    suspend operator fun invoke(foto: ByteArray): Result<List<EtiquetaDetectada>> =
        repo.etiquetar(foto).map { etiquetasVisibles(it) }
}