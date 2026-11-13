package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.DashboardSolicitudes
import com.tecnisis.galeria.domain.model.FiltrosDashboard
import com.tecnisis.galeria.domain.model.calcularDashboard
import com.tecnisis.galeria.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.TimeZone

/**
 * HU11: dashboard de solicitudes en tiempo real. Se recalcula cuando cambian las obras, los Expertos
 * o los filtros. El reloj y la zona se pueden cambiar en las pruebas.
 */
class ObservarDashboardUseCase(
    private val repo: DashboardRepository,
    private val ahora: () -> Long = { System.currentTimeMillis() },
    private val zona: () -> TimeZone = { TimeZone.getDefault() }
) {
    operator fun invoke(filtros: Flow<FiltrosDashboard>): Flow<DashboardSolicitudes> =
        combine(repo.observarObrasEvaluadas(), repo.observarExpertos(), filtros) { obras, expertos, f ->
            calcularDashboard(obras, expertos, f, ahora(), zona())
        }
}