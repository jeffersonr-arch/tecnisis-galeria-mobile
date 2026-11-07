package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU07: reglas de disponibilidad y selección de Expertos, sin Android ni Firebase. */
class SeleccionExpertosTest {

    private val carmen = PerfilEspecialista("c", "Carmen", setOf(Tecnica.OLEO, Tecnica.ACRILICO), EstadoEspecialista.ACTIVO, carga = 2)
    private val andres = PerfilEspecialista("a", "Andrés", setOf(Tecnica.OLEO, Tecnica.ACUARELA), EstadoEspecialista.ACTIVO, carga = 4)
    private val elena = PerfilEspecialista("e", "Elena", setOf(Tecnica.OLEO), EstadoEspecialista.INACTIVO, carga = 0)
    private val jorge = PerfilEspecialista("j", "Jorge", setOf(Tecnica.TEXTIL), EstadoEspecialista.ACTIVO, carga = 1)
    private val expertos = listOf(carmen, andres, elena, jorge)

    private fun obra(id: String, tecnica: Tecnica, recomendado: String?, vararg afinidades: Pair<String, Int>) =
        ObraEnRevision(
            id = id, titulo = "Obra $id", tecnica = tecnica, altoCm = 60.0, anchoCm = 80.0, profundidadCm = null,
            nota = "", fotoRuta = "obras/u/$id.jpg", descripcionIa = "Descripción",
            candidatos = afinidades.map { CandidatoExperto(it.first, it.second) }, recomendadoUid = recomendado
        )

    @Test
    fun inactivo_o_con_carga_maxima_no_esta_disponible() {
        assertFalse(elena.disponibleCon())
        assertTrue(andres.disponibleCon())     // 4 de 5
        assertFalse(andres.disponibleCon(1))   // 4 + 1 obra de esta solicitud = 5
    }

    @Test
    fun solo_aparecen_los_que_dominan_la_tecnica() {
        val telar = obra("t", Tecnica.TEXTIL, "j", "j" to 91)
        assertEquals(listOf("j"), opcionesPara(telar, expertos, seleccionInicial(listOf(telar))).map { it.experto.uid })
    }

    @Test
    fun orden_disponibles_primero_luego_afinidad_y_a_igual_afinidad_menor_carga() {
        val paisaje = obra("p", Tecnica.OLEO, "c", "c" to 80, "a" to 80, "e" to 95)
        val orden = opcionesPara(paisaje, expertos, seleccionInicial(listOf(paisaje))).map { it.experto.uid }
        assertEquals(listOf("c", "a", "e"), orden) // Elena tiene más afinidad, pero está inactiva
    }

    @Test
    fun dos_obras_de_la_misma_solicitud_cuentan_para_el_cupo() {
        val o1 = obra("1", Tecnica.OLEO, "a", "a" to 90)
        val o2 = obra("2", Tecnica.OLEO, "c", "c" to 70, "a" to 85)
        val seleccion = mapOf("1" to "a", "2" to "a") // Andrés (4 de 5) elegido para las dos
        val opcionAndres = opcionesPara(o2, expertos, seleccion).first { it.experto.uid == "a" }
        assertFalse(opcionAndres.disponible)
        assertNull(expertoValido(o2, expertos, seleccion))
        assertFalse(todasConExperto(listOf(o1, o2), expertos, seleccion))
    }

    @Test
    fun se_aprueba_solo_si_todas_las_obras_tienen_experto() {
        val paisaje = obra("p", Tecnica.OLEO, "c", "c" to 87)
        val telar = obra("t", Tecnica.TEXTIL, null, "j" to 91) // la función no encontró Experto
        val obras = listOf(paisaje, telar)
        assertFalse(todasConExperto(obras, expertos, seleccionInicial(obras)))
        assertTrue(todasConExperto(obras, expertos, seleccionInicial(obras) + ("t" to "j")))
        assertFalse(todasConExperto(emptyList(), expertos, emptyMap()))
    }

    @Test
    fun analisis_en_curso_vence_a_los_3_minutos() {
        val inicio = 1_000_000L
        assertFalse(analisisVencido(inicio, inicio + 2 * 60_000))
        assertFalse(analisisVencido(inicio, inicio + 3 * 60_000)) // justo 3 minutos: todavía no
        assertTrue(analisisVencido(inicio, inicio + 3 * 60_000 + 1))
        assertTrue(analisisVencido(null, inicio))
    }

    @Test
    fun etapa_de_la_tarjeta_segun_estado_y_analisis() {
        val base = SolicitudResumen(
            id = "s", nombreSocio = "Rosa", numeroSocio = "SOC-0001", estado = EstadoSolicitud.ENVIADA,
            analisis = EstadoAnalisis.PENDIENTE, obrasSinExperto = null, fechaEnvio = null, obras = emptyList()
        )
        assertEquals(EtapaRevision.POR_ANALIZAR, base.etapa())
        assertEquals(EtapaRevision.POR_ANALIZAR, base.copy(analisis = EstadoAnalisis.ERROR).etapa())
        assertEquals(EtapaRevision.ANALIZANDO, base.copy(analisis = EstadoAnalisis.EN_CURSO).etapa())
        assertEquals(EtapaRevision.LISTA, base.copy(analisis = EstadoAnalisis.LISTO, obrasSinExperto = 0).etapa())
        assertEquals(EtapaRevision.FALTA_EXPERTO, base.copy(analisis = EstadoAnalisis.LISTO, obrasSinExperto = 1).etapa())
        assertEquals(EtapaRevision.APROBADA, base.copy(estado = EstadoSolicitud.APROBADA).etapa())
    }
}