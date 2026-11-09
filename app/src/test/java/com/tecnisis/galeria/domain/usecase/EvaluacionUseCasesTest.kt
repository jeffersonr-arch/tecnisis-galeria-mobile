package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.EtiquetaCruda
import com.tecnisis.galeria.domain.model.EvaluacionError
import com.tecnisis.galeria.domain.model.ObraAsignada
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.TipoVeredicto
import com.tecnisis.galeria.domain.repository.EtiquetadoRepository
import com.tecnisis.galeria.domain.repository.EvaluacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU08: registrar el veredicto y etiquetar la foto, probados con repositorios falsos (sin Firebase ni ML Kit). */
class EvaluacionUseCasesTest {

    private class RepositorioFalso : EvaluacionRepository {
        var recibido: Triple<String, TipoVeredicto, String>? = null
        override fun observarAsignadas(): Flow<List<ObraAsignada>> = throw NotImplementedError()
        override fun observarObra(id: String): Flow<ObraAsignada?> = throw NotImplementedError()
        override fun observarMiFicha(): Flow<PerfilEspecialista?> = throw NotImplementedError()
        override suspend fun registrarVeredicto(obraId: String, tipo: TipoVeredicto, comentario: String): Result<Unit> {
            recibido = Triple(obraId, tipo, comentario)
            return Result.success(Unit)
        }
    }

    private class EtiquetadoFalso(private val crudas: List<EtiquetaCruda>) : EtiquetadoRepository {
        override suspend fun etiquetar(foto: ByteArray): Result<List<EtiquetaCruda>> = Result.success(crudas)
    }

    private val repo = RepositorioFalso()
    private val registrar = RegistrarVeredictoUseCase(repo)

    @Test
    fun rechazar_sin_comentario_no_llega_al_repositorio() = runBlocking {
        val resultado = registrar("o1", TipoVeredicto.RECHAZAR, "   ")
        assertEquals(EvaluacionError.ComentarioInvalido, resultado.exceptionOrNull())
        assertNull(repo.recibido)
    }

    @Test
    fun aceptar_sin_comentario_si_llega_al_repositorio() = runBlocking {
        val resultado = registrar("o1", TipoVeredicto.ACEPTAR, "")
        assertTrue(resultado.isSuccess)
        assertEquals(Triple("o1", TipoVeredicto.ACEPTAR, ""), repo.recibido)
    }

    @Test
    fun el_comentario_viaja_sin_espacios_en_los_extremos() = runBlocking {
        registrar("o1", TipoVeredicto.RECHAZAR, "  Los bordes están deshilachados  ")
        assertEquals("Los bordes están deshilachados", repo.recibido?.third)
    }

    @Test
    fun etiquetar_aplica_el_umbral_el_maximo_y_el_diccionario() = runBlocking {
        val etiquetar = EtiquetarFotoUseCase(
            EtiquetadoFalso(listOf(EtiquetaCruda("Boat", 0.78f), EtiquetaCruda("Sky", 0.91f), EtiquetaCruda("Toy", 0.2f)))
        )
        val visibles = etiquetar(ByteArray(0)).getOrThrow()
        assertEquals(listOf("Cielo" to 91, "Barco" to 78), visibles.map { it.nombre to it.porcentaje })
    }
}