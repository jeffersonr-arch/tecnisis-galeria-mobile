package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.CampoObra
import com.tecnisis.galeria.domain.model.DatosObra
import com.tecnisis.galeria.domain.model.FormularioObra
import com.tecnisis.galeria.domain.model.Obra
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.model.SolicitudExhibicion
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.EnvioSolicitudScheduler
import com.tecnisis.galeria.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU06: guardar y enviar, probados con un repositorio falso (sin Room ni WorkManager). */
class ObraUseCasesTest {

    private class RepositorioFalso : ObraRepository {
        var guardado: Pair<String?, DatosObra>? = null
        var resultadoMarcar: Result<Unit> = Result.success(Unit)

        override fun observarMiSolicitud(): Flow<SolicitudExhibicion?> = emptyFlow()
        override suspend fun obtenerObra(id: String): Result<Obra> = Result.failure(ObraError.NoEncontrada)
        override suspend fun guardarObra(id: String?, datos: DatosObra): Result<String> {
            guardado = id to datos
            return Result.success(id ?: "obra-nueva")
        }
        override suspend fun eliminarObra(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun marcarParaEnvio(): Result<Unit> = resultadoMarcar
    }

    private class ProgramadorFalso : EnvioSolicitudScheduler {
        var veces = 0
        override fun programar() {
            veces++
        }
    }

    private val repo = RepositorioFalso()
    private val programador = ProgramadorFalso()
    private val guardar = GuardarObraUseCase(repo)
    private val enviar = EnviarSolicitudUseCase(repo, programador)

    @Test
    fun guardar_con_errores_no_llega_a_room() = runBlocking {
        val resultado = guardar(
            FormularioObra(titulo = "Sin foto", tecnica = Tecnica.OLEO, alto = "60", ancho = "80")
        )
        assertEquals(ObraError.DatosInvalidos(setOf(CampoObra.FOTO)), resultado.exceptionOrNull())
        assertNull(repo.guardado)
    }

    @Test
    fun guardar_valido_envia_las_medidas_como_numero() = runBlocking {
        val resultado = guardar(
            FormularioObra(
                titulo = "Vasija", tecnica = Tecnica.CERAMICA, alto = "35",
                ancho = "22,5", profundidad = "22", fotoLocal = "/f.jpg"
            )
        )
        assertEquals("obra-nueva", resultado.getOrNull())
        val datos = repo.guardado!!.second
        assertEquals(22.5, datos.anchoCm, 0.0)
        assertEquals(22.0, datos.profundidadCm!!, 0.0)
    }

    @Test
    fun enviar_programa_workmanager_una_vez() = runBlocking {
        assertTrue(enviar().isSuccess)
        assertEquals(1, programador.veces)
    }

    @Test
    fun enviar_sin_obras_no_programa_nada() = runBlocking {
        repo.resultadoMarcar = Result.failure(ObraError.SinObras)
        assertEquals(ObraError.SinObras, enviar().exceptionOrNull())
        assertEquals(0, programador.veces)
    }
}