package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU07 · Parte 2: motivo, sugerencia, asignaciones, filas del PDF y aviso, sin Android ni Firebase. */
class DecisionSolicitudTest {

    private val carmen = PerfilEspecialista("c", "Carmen Villanueva", setOf(Tecnica.OLEO), EstadoEspecialista.ACTIVO, carga = 2)
    private val jorge = PerfilEspecialista("j", "Jorge Huamán Ccori", setOf(Tecnica.TEXTIL), EstadoEspecialista.ACTIVO, carga = 5)
    private val expertos = listOf(carmen, jorge)

    private fun obra(id: String, titulo: String, tecnica: Tecnica, recomendado: String?, profundidad: Double? = null) =
        ObraEnRevision(
            id = id, titulo = titulo, tecnica = tecnica, altoCm = 60.0, anchoCm = 22.5, profundidadCm = profundidad,
            nota = "", fotoRuta = "obras/u/$id.jpg", descripcionIa = "Descripción",
            candidatos = listOfNotNull(recomendado?.let { CandidatoExperto(it, 80) }), recomendadoUid = recomendado
        )

    private val paisaje = obra("p", "Mañana en Paracas", Tecnica.OLEO, "c")
    private val telar = obra("t", "Telar de Ocongate", Tecnica.TEXTIL, null, profundidad = 3.0) // Jorge tiene 5 de 5

    @Test
    fun motivo_vacio_o_solo_espacios_no_vale() {
        assertFalse(motivoValido(""))
        assertFalse(motivoValido("    "))
        assertTrue(motivoValido("  Falta Experto  "))
    }

    @Test
    fun motivo_de_500_vale_y_de_501_no() {
        assertTrue(motivoValido("a".repeat(MOTIVO_MAXIMO)))
        assertFalse(motivoValido("a".repeat(MOTIVO_MAXIMO + 1)))
        assertTrue(motivoValido(" " + "a".repeat(MOTIVO_MAXIMO) + " ")) // los espacios de los extremos no cuentan
    }

    @Test
    fun sin_experto_en_alguna_obra_no_hay_asignaciones() {
        val obras = listOf(paisaje, telar)
        assertNull(asignacionesDe(obras, expertos, seleccionInicial(obras)))
    }

    @Test
    fun con_todas_las_obras_asignadas_devuelve_obra_y_experto() {
        val obras = listOf(paisaje)
        assertEquals(mapOf("p" to "c"), asignacionesDe(obras, expertos, seleccionInicial(obras)))
    }

    @Test
    fun la_sugerencia_nombra_las_tecnicas_sin_experto() {
        val obras = listOf(paisaje, telar)
        assertEquals("No hay Experto disponible para Textil", motivoSugerido(obras, expertos, seleccionInicial(obras)))
        assertNull(motivoSugerido(listOf(paisaje), expertos, seleccionInicial(listOf(paisaje))))
    }

    @Test
    fun unir_con_y() {
        assertEquals("Textil", unirConY(listOf("Textil")))
        assertEquals("Textil y Cerámica", unirConY(listOf("Textil", "Cerámica")))
        assertEquals("Óleo, Textil y Cerámica", unirConY(listOf("Óleo", "Textil", "Cerámica")))
    }

    @Test
    fun numero_y_medidas_del_documento() {
        assertEquals("7F3C-2A91", numeroDocumento("7f3c2a91-0b5e-4c1d-9e8f-123456789abc"))
        assertEquals("60 × 22,5", medidasTexto(60.0, 22.5, null))
        assertEquals("35 × 22 × 22", medidasTexto(35.0, 22.0, 22.0))
    }

    @Test
    fun filas_de_aprobacion_llevan_el_experto_y_las_de_rechazo_no_admitida() {
        val obras = listOf(paisaje, telar)
        val aprobacion = filasDocumento(obras, TipoDecision.APROBAR, mapOf("p" to "c", "t" to "j")) { uid ->
            expertos.first { it.uid == uid }.nombre
        }
        assertEquals(listOf("Carmen Villanueva", "Jorge Huamán Ccori"), aprobacion.map { it.ultima })
        assertEquals(listOf(1, 2), aprobacion.map { it.numero })

        val rechazo = filasDocumento(obras, TipoDecision.RECHAZAR, emptyMap()) { it }
        assertEquals(listOf("No admitida", "No admitida"), rechazo.map { it.ultima })
        assertEquals("60 × 22,5 × 3", rechazo[1].medidas)
    }

    @Test
    fun la_seleccion_inicial_usa_el_asignado_si_ya_se_aprobo() {
        val aprobada = paisaje.copy(recomendadoUid = "c", expertoAsignadoUid = "x")
        assertEquals(mapOf("p" to "x"), seleccionInicial(listOf(aprobada)))
    }

    @Test
    fun aviso_de_aprobacion_en_singular_y_plural_y_de_rechazo() {
        assertEquals("Tu obra pasó a evaluación artística. Toca para ver tu documento.", avisoDecision(true, 1).texto)
        assertEquals("Tus 3 obras pasaron a evaluación artística. Toca para ver tu documento.", avisoDecision(true, 3).texto)
        assertEquals("Tu solicitud fue rechazada", avisoDecision(false, 3).titulo)
    }
}