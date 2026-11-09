package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU08: lo que ve el socio en Mis obras durante la evaluación artística, y su notificación. */
class EvaluacionSolicitudTest {

    private fun obra(id: String, estado: EstadoObra) = Obra(
        id = id, solicitudId = "s", titulo = "Obra $id", tecnica = Tecnica.OLEO, altoCm = 60.0, anchoCm = 80.0,
        profundidadCm = null, fotoLocal = "", fotoUrl = null, nota = "", estado = estado, creadaEn = 0L
    )

    private fun solicitud(estado: EstadoSolicitud, vararg estados: EstadoObra) = SolicitudExhibicion(
        id = "s", estado = estado, creadaEn = 0L, enviadaEn = 1L,
        obras = estados.mapIndexed { i, e -> obra("$i", e) }
    )

    @Test
    fun la_evaluacion_termina_cuando_ninguna_obra_sigue_en_evaluacion() {
        val s = solicitud(EstadoSolicitud.APROBADA, EstadoObra.ACEPTADA, EstadoObra.RECHAZADA, EstadoObra.ACEPTADA)
        assertTrue(s.evaluacionTerminada)
        assertEquals(3, s.cantidadEvaluadas)
    }

    @Test
    fun no_termina_si_queda_una_en_evaluacion() {
        val s = solicitud(EstadoSolicitud.APROBADA, EstadoObra.ACEPTADA, EstadoObra.EN_EVALUACION)
        assertFalse(s.evaluacionTerminada)
        assertEquals(1, s.cantidadEvaluadas)
    }

    @Test
    fun cuenta_aceptadas_y_rechazadas() {
        val s = solicitud(
            EstadoSolicitud.APROBADA,
            EstadoObra.ACEPTADA, EstadoObra.LISTA_EXHIBICION, EstadoObra.RECHAZADA, EstadoObra.EN_EVALUACION
        )
        assertEquals(2, s.cantidadAceptadas)
        assertEquals(1, s.cantidadRechazadas)
    }

    @Test
    fun una_solicitud_rechazada_no_tiene_evaluacion_artistica() {
        val s = solicitud(EstadoSolicitud.RECHAZADA, EstadoObra.NO_ADMITIDA, EstadoObra.NO_ADMITIDA)
        assertFalse(s.evaluacionTerminada)
        assertEquals(0, s.cantidadEvaluadas)
    }

    @Test
    fun el_aviso_nombra_la_obra_y_el_veredicto() {
        assertEquals("Tu obra «Telar» fue rechazada", avisoVeredicto(aceptada = false, tituloObra = "Telar").titulo)
        assertEquals("Tu obra «Paracas» fue aceptada", avisoVeredicto(aceptada = true, tituloObra = "Paracas").titulo)
        assertEquals("Tu obra fue aceptada", avisoVeredicto(aceptada = true, tituloObra = "").titulo)
    }

    @Test
    fun cada_obra_tiene_su_id_de_aviso_y_nunca_el_de_la_decision() {
        assertEquals(idAvisoVeredicto("obra-1"), idAvisoVeredicto("obra-1"))
        assertNotEquals(idAvisoVeredicto("obra-1"), idAvisoVeredicto("obra-2"))
        listOf("a", "b", "obra-1", "c2f1e0a4-1b2c-4d5e-8f90-123456789abc").forEach {
            assertTrue(idAvisoVeredicto(it) >= 3_000)
            assertNotEquals(2001, idAvisoVeredicto(it))
        }
    }
}