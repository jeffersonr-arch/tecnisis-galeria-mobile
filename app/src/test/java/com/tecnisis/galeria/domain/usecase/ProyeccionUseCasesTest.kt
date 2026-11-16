package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.ArchivoReporte
import com.tecnisis.galeria.domain.model.FormatoReporte
import com.tecnisis.galeria.domain.model.ObraProyectada
import com.tecnisis.galeria.domain.model.ProyeccionEconomica
import com.tecnisis.galeria.domain.model.ProyeccionError
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.calcularProyeccion
import com.tecnisis.galeria.domain.repository.ProyeccionRepository
import com.tecnisis.galeria.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU12: casos de uso de la proyección con repositorios falsos (sin Firebase ni archivos). */
class ProyeccionUseCasesTest {

    private val obra = ObraProyectada(
        obraId = "o1", uid = "maria", titulo = "Nocturno limeño", tecnica = Tecnica.OLEO,
        nombreSocio = "María Elena López Díaz", numeroSocio = "SOC-0002",
        pvpCentimos = 280_000, porcentaje = 30, montoGaleriaCentimos = 84_000, montoSocioCentimos = 196_000,
        fechaTasacion = null
    )

    private class ProyeccionFalsa(private val obras: List<ObraProyectada>) : ProyeccionRepository {
        override fun observarObrasTasadas(): Flow<List<ObraProyectada>> = flowOf(obras)
    }

    private class ReporteFalso : ReporteRepository {
        var llamadas = 0
        var generadoRecibido = 0L
        override suspend fun generar(proyeccion: ProyeccionEconomica, formato: FormatoReporte, generado: Long): Result<ArchivoReporte> {
            llamadas++
            generadoRecibido = generado
            return Result.success(ArchivoReporte("/cache/documentos/x.${formato.extension}", formato))
        }
    }

    @Test
    fun observar_calcula_la_proyeccion() = runBlocking {
        val p = ObservarProyeccionUseCase(ProyeccionFalsa(listOf(obra))).invoke().first()
        assertEquals(84_000L, p.totalGaleriaCentimos)
        assertEquals(1, p.cantidad)
    }

    @Test
    fun exportar_sin_obras_no_genera_nada() = runBlocking {
        val reporte = ReporteFalso()
        val resultado = ExportarProyeccionUseCase(reporte) { 1L }(calcularProyeccion(emptyList()), FormatoReporte.PDF)
        assertEquals(ProyeccionError.NoSeGenero, resultado.exceptionOrNull())
        assertEquals(0, reporte.llamadas)
    }

    @Test
    fun exportar_pasa_la_hora_del_celular() = runBlocking {
        val reporte = ReporteFalso()
        val resultado = ExportarProyeccionUseCase(reporte) { 1_794_771_000_000L }(calcularProyeccion(listOf(obra)), FormatoReporte.CSV)
        assertTrue(resultado.isSuccess)
        assertEquals(FormatoReporte.CSV, resultado.getOrNull()?.formato)
        assertEquals(1_794_771_000_000L, reporte.generadoRecibido)
    }
}