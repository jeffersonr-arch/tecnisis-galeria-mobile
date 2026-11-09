package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.ObraRechazada
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.HistoricoRechazosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU09: el caso de uso del histórico entrega la lista ordenada, con un repositorio falso (sin Firebase). */
class RechazoUseCasesTest {

    private fun obra(id: String, fecha: Long) = ObraRechazada(
        id = id, titulo = id, tecnica = Tecnica.OLEO, altoCm = 1.0, anchoCm = 1.0, profundidadCm = null,
        nombreSocio = null, numeroSocio = null, evaluadaPorNombre = null, motivo = "", fechaVeredicto = fecha,
        documentoRechazo = emptyList()
    )

    private class RepositorioFalso(private val obras: List<ObraRechazada>) : HistoricoRechazosRepository {
        override fun observarRechazadas(): Flow<List<ObraRechazada>> = flowOf(obras)
    }

    @Test
    fun historico_llega_ordenado() = runBlocking {
        val caso = ObservarObrasRechazadasUseCase(RepositorioFalso(listOf(obra("viejo", 1L), obra("nuevo", 9L))))
        assertEquals(listOf("nuevo", "viejo"), caso().first().map { it.id })
    }
}