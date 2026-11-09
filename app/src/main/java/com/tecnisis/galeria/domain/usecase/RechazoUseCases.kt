package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.historicoRechazos
import com.tecnisis.galeria.domain.repository.DocumentoObraRepository
import com.tecnisis.galeria.domain.repository.HistoricoRechazosRepository
import kotlinx.coroutines.flow.map

/** HU09: descarga una copia del documento de rechazo de una obra y devuelve la ruta del archivo. */
class DescargarDocumentoObraUseCase(private val repo: DocumentoObraRepository) {
    suspend operator fun invoke(obraId: String, copia: CopiaDocumento) = repo.descargar(obraId, copia)
}

/** HU09: histórico del Gerente, la más reciente primero. */
class ObservarObrasRechazadasUseCase(private val repo: HistoricoRechazosRepository) {
    operator fun invoke() = repo.observarRechazadas().map { historicoRechazos(it) }
}