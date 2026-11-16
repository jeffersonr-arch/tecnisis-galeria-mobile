package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.ArchivoReporte
import com.tecnisis.galeria.domain.model.FormatoReporte
import com.tecnisis.galeria.domain.model.ProyeccionEconomica
import com.tecnisis.galeria.domain.model.ProyeccionError
import com.tecnisis.galeria.domain.model.calcularProyeccion
import com.tecnisis.galeria.domain.repository.ProyeccionRepository
import com.tecnisis.galeria.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** HU12: proyección económica en tiempo real; se recalcula cada vez que cambia documentos_venta. */
class ObservarProyeccionUseCase(private val repo: ProyeccionRepository) {
    operator fun invoke(): Flow<ProyeccionEconomica> = repo.observarObrasTasadas().map { calcularProyeccion(it) }
}

/** HU12: exporta la proyección que se ve en pantalla. Sin obras tasadas no genera nada. */
class ExportarProyeccionUseCase(
    private val repo: ReporteRepository,
    private val ahora: () -> Long = { System.currentTimeMillis() }
) {
    suspend operator fun invoke(proyeccion: ProyeccionEconomica, formato: FormatoReporte): Result<ArchivoReporte> =
        if (proyeccion.cantidad == 0) Result.failure(ProyeccionError.NoSeGenero)
        else repo.generar(proyeccion, formato, ahora())
}