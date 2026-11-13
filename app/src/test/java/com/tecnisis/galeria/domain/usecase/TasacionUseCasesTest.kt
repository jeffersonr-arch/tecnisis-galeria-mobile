package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.MontosTasacion
import com.tecnisis.galeria.domain.model.ObraPorTasar
import com.tecnisis.galeria.domain.model.ObraTasada
import com.tecnisis.galeria.domain.model.TasacionError
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.TasacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** HU10: los casos de uso de la tasación con un repositorio falso (sin Firebase). */
class TasacionUseCasesTest {

    private fun porTasar(id: String, estado: EstadoObra, fecha: Long?) = ObraPorTasar(
        id = id, solicitudId = "s", uid = "u", titulo = id, tecnica = Tecnica.OLEO, altoCm = 1.0, anchoCm = 1.0,
        profundidadCm = null, fotoRuta = "", nombreSocio = null, numeroSocio = null, comentarioVeredicto = null,
        fechaVeredicto = fecha, estado = estado
    )

    private fun tasada(id: String, fecha: Long?) = ObraTasada(
        obraId = id, titulo = id, tecnica = Tecnica.OLEO, fotoRuta = "", nombreSocio = null, numeroSocio = null,
        pvpCentimos = 100, porcentaje = 10, montoGaleriaCentimos = 10, montoSocioCentimos = 90, fechaTasacion = fecha
    )

    private class RepositorioFalso(
        private val porTasar: List<ObraPorTasar> = emptyList(),
        private val tasadas: List<ObraTasada> = emptyList()
    ) : TasacionRepository {
        var guardado: MontosTasacion? = null
        override fun observarPorTasar(): Flow<List<ObraPorTasar>> = flowOf(porTasar)
        override fun observarTasadas(): Flow<List<ObraTasada>> = flowOf(tasadas)
        override suspend fun obtenerPorTasar(id: String): Result<ObraPorTasar> = Result.failure(TasacionError.NoEncontrada)
        override suspend fun registrarTasacion(obraId: String, montos: MontosTasacion): Result<Unit> {
            guardado = montos
            return Result.success(Unit)
        }
    }

    @Test
    fun por_tasar_solo_aceptadas_y_la_mas_antigua_primero() = runBlocking {
        val repo = RepositorioFalso(
            porTasar = listOf(
                porTasar("nueva", EstadoObra.ACEPTADA, 9L),
                porTasar("sinFecha", EstadoObra.ACEPTADA, null),
                porTasar("vieja", EstadoObra.ACEPTADA, 1L),
                porTasar("lista", EstadoObra.LISTA_EXHIBICION, 0L)
            )
        )
        assertEquals(listOf("vieja", "nueva", "sinFecha"), ObservarObrasPorTasarUseCase(repo)().first().map { it.id })
    }

    @Test
    fun tasadas_la_mas_reciente_primero() = runBlocking {
        val repo = RepositorioFalso(tasadas = listOf(tasada("vieja", 1L), tasada("nueva", 9L)))
        assertEquals(listOf("nueva", "vieja"), ObservarObrasTasadasUseCase(repo)().first().map { it.obraId })
    }

    @Test
    fun registrar_calcula_los_montos() = runBlocking {
        val repo = RepositorioFalso()
        val resultado = RegistrarTasacionUseCase(repo)("o", "2500", "18")
        assertEquals(MontosTasacion(250_000, 18, 45_000, 205_000), resultado.getOrNull())
        assertEquals(MontosTasacion(250_000, 18, 45_000, 205_000), repo.guardado)
    }

    @Test
    fun datos_invalidos_no_llaman_al_repositorio() = runBlocking {
        val repo = RepositorioFalso()
        assertEquals(TasacionError.PvpInvalido, RegistrarTasacionUseCase(repo)("o", "1,250.50", "18").exceptionOrNull())
        assertEquals(TasacionError.PorcentajeInvalido, RegistrarTasacionUseCase(repo)("o", "2500", "101").exceptionOrNull())
        assertNull(repo.guardado)
    }
}