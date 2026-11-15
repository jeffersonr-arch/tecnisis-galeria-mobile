package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.ProyeccionEconomica
import com.tecnisis.galeria.domain.model.calcularProyeccion
import com.tecnisis.galeria.domain.repository.ProyeccionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** HU12: proyección económica en tiempo real; se recalcula cada vez que cambia documentos_venta. */
class ObservarProyeccionUseCase(private val repo: ProyeccionRepository) {
    operator fun invoke(): Flow<ProyeccionEconomica> = repo.observarObrasTasadas().map { calcularProyeccion(it) }
}