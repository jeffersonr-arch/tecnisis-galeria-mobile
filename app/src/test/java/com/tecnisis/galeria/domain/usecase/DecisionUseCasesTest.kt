package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.CandidatoExperto
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.model.DecisionSolicitud
import com.tecnisis.galeria.domain.model.EstadoEspecialista
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.TipoDecision
import com.tecnisis.galeria.domain.model.seleccionInicial
import com.tecnisis.galeria.domain.repository.DecisionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU07 · Parte 2: aprobar y rechazar, probados con un repositorio falso (sin PDF, Storage ni Cloud Functions). */
class DecisionUseCasesTest {

    private class RepositorioFalso : DecisionRepository {
        var recibida: DecisionSolicitud? = null
        override suspend fun decidir(decision: DecisionSolicitud): Result<Unit> {
            recibida = decision
            return Result.success(Unit)
        }
        override suspend fun descargarDocumento(solicitudId: String, copia: CopiaDocumento): Result<String> =
            Result.success("/cache/documentos/$solicitudId-${copia.archivo}")
    }

    private val repo = RepositorioFalso()
    private val aprobar = AprobarSolicitudUseCase(repo)
    private val rechazar = RechazarSolicitudUseCase(repo)

    private val carmen = PerfilEspecialista("c", "Carmen", setOf(Tecnica.OLEO), EstadoEspecialista.ACTIVO, carga = 0)
    private fun obra(id: String, tecnica: Tecnica, recomendado: String?) = ObraEnRevision(
        id = id, titulo = "Obra $id", tecnica = tecnica, altoCm = 60.0, anchoCm = 80.0, profundidadCm = null,
        nota = "", fotoRuta = "", descripcionIa = null,
        candidatos = listOfNotNull(recomendado?.let { CandidatoExperto(it, 90) }), recomendadoUid = recomendado
    )

    @Test
    fun aprobar_con_una_obra_sin_experto_no_llega_al_repositorio() = runBlocking {
        val obras = listOf(obra("1", Tecnica.OLEO, "c"), obra("2", Tecnica.TEXTIL, null))
        val resultado = aprobar("s", obras, listOf(carmen), seleccionInicial(obras))
        assertEquals(DecisionError.FaltaExperto, resultado.exceptionOrNull())
        assertNull(repo.recibida)
    }

    @Test
    fun aprobar_envia_cada_obra_con_su_experto() = runBlocking {
        val obras = listOf(obra("1", Tecnica.OLEO, "c"), obra("2", Tecnica.OLEO, "c"))
        val resultado = aprobar("s", obras, listOf(carmen), seleccionInicial(obras))
        assertTrue(resultado.isSuccess)
        assertEquals(TipoDecision.APROBAR, repo.recibida?.tipo)
        assertEquals(mapOf("1" to "c", "2" to "c"), repo.recibida?.asignaciones)
    }

    @Test
    fun rechazar_sin_motivo_no_llega_al_repositorio() = runBlocking {
        assertEquals(DecisionError.MotivoInvalido, rechazar("s", "   ").exceptionOrNull())
        assertNull(repo.recibida)
    }

    @Test
    fun rechazar_recorta_el_motivo() = runBlocking {
        rechazar("s", "  No hay Experto disponible para Textil  ")
        assertEquals("No hay Experto disponible para Textil", repo.recibida?.motivo)
        assertEquals(TipoDecision.RECHAZAR, repo.recibida?.tipo)
    }
}