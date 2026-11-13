package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.ExpertoDashboard
import com.tecnisis.galeria.domain.model.FiltroFecha
import com.tecnisis.galeria.domain.model.FiltrosDashboard
import com.tecnisis.galeria.domain.model.ObraEvaluada
import com.tecnisis.galeria.domain.model.ResumenDona
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

/** HU11: el caso de uso junta obras, Expertos y filtros con un repositorio falso (sin Firebase). */
class DashboardUseCasesTest {

    private val ahora = 1_794_600_000_000L
    private val unDia = 24L * 60 * 60 * 1000

    private class RepositorioFalso(
        private val obras: List<ObraEvaluada>,
        private val expertos: List<ExpertoDashboard>
    ) : DashboardRepository {
        override fun observarObrasEvaluadas(): Flow<List<ObraEvaluada>> = flowOf(obras)
        override fun observarExpertos(): Flow<List<ExpertoDashboard>> = flowOf(expertos)
    }

    private fun obra(id: String, aceptada: Boolean, fecha: Long) =
        ObraEvaluada(id, aceptada, Tecnica.OLEO, "Rosa Quispe Mamani", "SOC-0001", "a2", fecha)

    private val caso = ObservarDashboardUseCase(
        RepositorioFalso(
            obras = listOf(obra("1", true, ahora), obra("2", false, ahora - 40 * unDia)),
            expertos = listOf(ExpertoDashboard("a2", "Andrés Salazar Ríos", activo = true, carga = 4))
        ),
        ahora = { ahora },
        zona = { TimeZone.getTimeZone("America/Lima") }
    )

    @Test
    fun con_todo_cuenta_las_dos() = runBlocking {
        val dashboard = caso(flowOf(FiltrosDashboard())).first()
        assertEquals(ResumenDona(1, 1), dashboard.dona)
        assertEquals(2, dashboard.ranking.single().evaluadas)
    }

    @Test
    fun con_30_dias_deja_fuera_la_antigua() = runBlocking {
        val dashboard = caso(flowOf(FiltrosDashboard(fecha = FiltroFecha.TreintaDias))).first()
        assertEquals(ResumenDona(1, 0), dashboard.dona)
        assertEquals(4, dashboard.ranking.single().pendientes)
    }
}