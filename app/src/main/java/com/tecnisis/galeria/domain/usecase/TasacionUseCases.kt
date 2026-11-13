package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.LecturaPorcentaje
import com.tecnisis.galeria.domain.model.LecturaPvp
import com.tecnisis.galeria.domain.model.MontosTasacion
import com.tecnisis.galeria.domain.model.TasacionError
import com.tecnisis.galeria.domain.model.calcularMontos
import com.tecnisis.galeria.domain.model.leerPorcentaje
import com.tecnisis.galeria.domain.model.leerPvp
import com.tecnisis.galeria.domain.model.obrasPorTasar
import com.tecnisis.galeria.domain.model.obrasTasadas
import com.tecnisis.galeria.domain.repository.DocumentoVentaRepository
import com.tecnisis.galeria.domain.repository.TasacionRepository
import kotlinx.coroutines.flow.map

/** HU10: pestaña Por tasar, la del veredicto más antiguo primero. */
class ObservarObrasPorTasarUseCase(private val repo: TasacionRepository) {
    operator fun invoke() = repo.observarPorTasar().map { obrasPorTasar(it) }
}

/** HU10: pestaña Tasadas, la más reciente primero. */
class ObservarObrasTasadasUseCase(private val repo: TasacionRepository) {
    operator fun invoke() = repo.observarTasadas().map { obrasTasadas(it) }
}

class ObtenerObraPorTasarUseCase(private val repo: TasacionRepository) {
    suspend operator fun invoke(id: String) = repo.obtenerPorTasar(id)
}

/**
 * HU10: valida el PVP y el porcentaje escritos y calcula los montos. Si algo no cumple, ni siquiera genera el PDF
 * ni llama a la función. Devuelve los montos que se guardaron.
 */
class RegistrarTasacionUseCase(private val repo: TasacionRepository) {
    suspend operator fun invoke(obraId: String, pvp: String, porcentaje: String): Result<MontosTasacion> {
        val precio = leerPvp(pvp) as? LecturaPvp.Valido
            ?: return Result.failure(TasacionError.PvpInvalido)
        val pct = leerPorcentaje(porcentaje) as? LecturaPorcentaje.Valido
            ?: return Result.failure(TasacionError.PorcentajeInvalido)
        val montos = calcularMontos(precio.centimos, pct.valor)
        return repo.registrarTasacion(obraId, montos).map { montos }
    }
}

/** HU10: descarga una copia del documento de venta y devuelve la ruta del archivo. */
class DescargarDocumentoVentaUseCase(private val repo: DocumentoVentaRepository) {
    suspend operator fun invoke(obraId: String, copia: CopiaDocumento) = repo.descargar(obraId, copia)
}