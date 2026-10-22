package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.DecisionPostulacion
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.model.PostulacionError
import com.tecnisis.galeria.domain.model.decisionesPermitidas
import com.tecnisis.galeria.domain.repository.PostulacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU04: reglas del Anfitrión, probadas sin Firebase (repositorio falso). */
class ResolverPostulacionUseCaseTest {

    /** Guarda lo que le pidieron en vez de llamar a la Cloud Function. */
    private class RepositorioFalso : PostulacionRepository {
        var llamada: Triple<String, DecisionPostulacion, String?>? = null

        override suspend fun enviar(postulacion: Postulacion): Result<String> = Result.success(postulacion.uid)
        override fun observarMiPostulacion(): Flow<Postulacion?> = emptyFlow()
        override suspend fun obtener(uid: String): Result<Postulacion> =
            Result.failure(PostulacionError.NoEncontrada)
        override suspend fun registrarVerificacion(
            uid: String, estado: EstadoPostulacion, motivo: String?
        ): Result<Unit> = Result.success(Unit)
        override fun observarPorEstado(estado: EstadoPostulacion): Flow<List<Postulacion>> = emptyFlow()
        override suspend fun resolver(
            uid: String, decision: DecisionPostulacion, mensaje: String?
        ): Result<Unit> {
            llamada = Triple(uid, decision, mensaje)
            return Result.success(Unit)
        }
    }

    private val repo = RepositorioFalso()
    private val resolver = ResolverPostulacionUseCase(repo)

    private fun postulacion(estado: EstadoPostulacion) = Postulacion(
        uid = "rosa", dni = "11223344", nombres = "Rosa Maria", apellidoPaterno = "Quispe",
        apellidoMaterno = "Mamani", direccion = "Av. Jose Olaya 133", sexo = "Femenino",
        telefono = "999888777", correo = "rosa@tecnisis.pe", estado = estado
    )

    @Test
    fun rechazar_sin_motivo_no_llama_a_la_funcion() = runBlocking {
        val resultado = resolver(postulacion(EstadoPostulacion.VERIFICADA), DecisionPostulacion.RECHAZAR, "   ")
        assertEquals(PostulacionError.MensajeObligatorio, resultado.exceptionOrNull())
        assertNull(repo.llamada)
    }

    @Test
    fun rechazar_con_motivo_envia_el_texto_sin_espacios() = runBlocking {
        val resultado = resolver(postulacion(EstadoPostulacion.OBSERVADA), DecisionPostulacion.RECHAZAR, "  DNI falso  ")
        assertTrue(resultado.isSuccess)
        assertEquals(Triple("rosa", DecisionPostulacion.RECHAZAR, "DNI falso"), repo.llamada)
    }

    @Test
    fun aprobar_una_verificada_no_exige_nota() = runBlocking {
        val resultado = resolver(postulacion(EstadoPostulacion.VERIFICADA), DecisionPostulacion.APROBAR, "")
        assertTrue(resultado.isSuccess)
        assertEquals(Triple("rosa", DecisionPostulacion.APROBAR, null), repo.llamada)
    }

    @Test
    fun aprobar_una_observada_exige_la_nota_de_correccion() = runBlocking {
        val resultado = resolver(postulacion(EstadoPostulacion.OBSERVADA), DecisionPostulacion.APROBAR, null)
        assertEquals(PostulacionError.MensajeObligatorio, resultado.exceptionOrNull())
        assertNull(repo.llamada)
    }

    @Test
    fun pedir_informacion_a_una_verificada_no_esta_permitido() = runBlocking {
        val resultado = resolver(
            postulacion(EstadoPostulacion.VERIFICADA), DecisionPostulacion.PEDIR_INFORMACION, "Trae tu DNI"
        )
        assertEquals(PostulacionError.DecisionNoPermitida, resultado.exceptionOrNull())
        assertNull(repo.llamada)
    }

    @Test
    fun una_postulacion_aprobada_o_rechazada_ya_no_admite_decisiones() {
        assertTrue(EstadoPostulacion.APROBADA.decisionesPermitidas().isEmpty())
        assertTrue(EstadoPostulacion.RECHAZADA.decisionesPermitidas().isEmpty())
        assertTrue(EstadoPostulacion.PENDIENTE_VERIFICACION.decisionesPermitidas().isEmpty())
    }
}