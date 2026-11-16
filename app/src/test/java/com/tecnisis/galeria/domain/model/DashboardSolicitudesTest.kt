package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** HU11: porcentajes, fechas, filtros combinados y ranking del dashboard del Gerente. */
class DashboardSolicitudesTest {

    private val lima: TimeZone = TimeZone.getTimeZone("America/Lima")

    /** Una hora del celular en Lima. */
    private fun hora(anio: Int, mes: Int, dia: Int, h: Int = 12, m: Int = 0): Long =
        Calendar.getInstance(lima).apply { clear(); set(anio, mes - 1, dia, h, m) }.timeInMillis

    /** Un día como lo entrega el DateRangePicker (medianoche UTC). */
    private fun diaUtc(anio: Int, mes: Int, dia: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(anio, mes - 1, dia) }.timeInMillis

    private val ahora = hora(2026, 11, 13, 14, 0)

    private fun obra(
        id: String, aceptada: Boolean, fecha: Long?, tecnica: Tecnica = Tecnica.OLEO,
        socio: String? = "María Elena López Díaz", numero: String? = "SOC-0002", experto: String? = "a2"
    ) = ObraEvaluada(id, aceptada, tecnica, socio, numero, experto, fecha)

    private val expertos = listOf(
        ExpertoDashboard("a2", "Andrés Salazar Ríos", activo = true, carga = 4),
        ExpertoDashboard("a3", "Patricia Núñez Vega", activo = true, carga = 0),
        ExpertoDashboard("a6", "Elena Torres Ugarte", activo = false, carga = 0)
    )

    private fun calcular(obras: List<ObraEvaluada>, filtros: FiltrosDashboard = FiltrosDashboard()) =
        calcularDashboard(obras, expertos, filtros, ahora, lima)

    @Test
    fun porcentajes_suman_100() {
        assertEquals(33, ResumenDona(1, 2).porcentajeAceptadas)
        assertEquals(67, ResumenDona(1, 2).porcentajeRechazadas)
        assertEquals(71, ResumenDona(12, 5).porcentajeAceptadas)
        assertEquals(29, ResumenDona(12, 5).porcentajeRechazadas)
        assertEquals(50, ResumenDona(1, 1).porcentajeAceptadas)
        assertEquals(100, ResumenDona(3, 0).porcentajeAceptadas)
        assertEquals(0, ResumenDona(0, 3).porcentajeAceptadas)
    }

    @Test
    fun medio_redondea_hacia_arriba() {
        // 1 de 8 = 12,5 % → 13 % y 87 %
        assertEquals(13, ResumenDona(1, 7).porcentajeAceptadas)
        assertEquals(87, ResumenDona(1, 7).porcentajeRechazadas)
    }

    @Test
    fun sin_obras_todo_en_cero() {
        assertEquals(0, ResumenDona(0, 0).porcentajeAceptadas)
        assertEquals(0, ResumenDona(0, 0).porcentajeRechazadas)
    }

    @Test
    fun texto_del_centro_singular_y_plural() {
        assertEquals("1 obra", ResumenDona(1, 0).textoCentro)
        assertEquals("17 obras", ResumenDona(12, 5).textoCentro)
        assertEquals("0 obras", ResumenDona(0, 0).textoCentro)
    }

    @Test
    fun siete_dias_empieza_a_las_00_de_hace_6_dias() {
        val obras = listOf(
            obra("dentro", true, hora(2026, 11, 7, 0, 0)),
            obra("fuera", true, hora(2026, 11, 6, 23, 59)),
            obra("hoy", false, hora(2026, 11, 13, 13, 0))
        )
        assertEquals(ResumenDona(1, 1), calcular(obras, FiltrosDashboard(fecha = FiltroFecha.SieteDias)).dona)
    }

    @Test
    fun treinta_dias_empieza_a_las_00_de_hace_29_dias() {
        val obras = listOf(
            obra("dentro", true, hora(2026, 10, 15, 0, 0)),
            obra("fuera", true, hora(2026, 10, 14, 23, 59))
        )
        assertEquals(ResumenDona(1, 0), calcular(obras, FiltrosDashboard(fecha = FiltroFecha.TreintaDias)).dona)
    }

    @Test
    fun rango_incluye_completos_el_primer_y_el_ultimo_dia() {
        val rango = FiltroFecha.Rango(diaUtc(2026, 11, 2), diaUtc(2026, 11, 5))
        val obras = listOf(
            obra("primero", true, hora(2026, 11, 2, 0, 0)),
            obra("ultimo", false, hora(2026, 11, 5, 23, 59)),
            obra("antes", true, hora(2026, 11, 1, 23, 59)),
            obra("despues", true, hora(2026, 11, 6, 0, 0))
        )
        assertEquals(ResumenDona(1, 1), calcular(obras, FiltrosDashboard(fecha = rango)).dona)
    }

    @Test
    fun sin_fecha_solo_cuenta_con_todo() {
        val obras = listOf(obra("vieja", true, null))
        assertEquals(1, calcular(obras).dona.total)
        assertEquals(0, calcular(obras, FiltrosDashboard(fecha = FiltroFecha.SieteDias)).dona.total)
    }

    @Test
    fun filtros_se_combinan() {
        val obras = listOf(
            obra("si", true, hora(2026, 11, 10)),
            obra("otra_tecnica", true, hora(2026, 11, 10), tecnica = Tecnica.TEXTIL),
            obra("otro_socio", true, hora(2026, 11, 10), socio = "Rosa Quispe Mamani", numero = "SOC-0001"),
            obra("otro_experto", true, hora(2026, 11, 10), experto = "a3"),
            obra("vieja", true, hora(2026, 9, 1))
        )
        val filtros = FiltrosDashboard(
            fecha = FiltroFecha.TreintaDias,
            tecnica = Tecnica.OLEO,
            socio = "María Elena López Díaz · SOC-0002",
            expertoUid = "a2"
        )
        assertEquals(ResumenDona(1, 0), calcular(obras, filtros).dona)
    }

    @Test
    fun ranking_ordena_por_evaluadas_y_empata_por_nombre() {
        val obras = listOf(
            obra("1", true, hora(2026, 11, 10), experto = "a3"),
            obra("2", false, hora(2026, 11, 10), experto = "a3")
        )
        val ranking = calcular(obras).ranking
        assertEquals(listOf("a3", "a2", "a6"), ranking.map { it.uid })
        assertEquals(listOf(2, 0, 0), ranking.map { it.evaluadas })
    }

    @Test
    fun ranking_trae_pendientes_sin_filtros_y_marca_inactivos() {
        val ranking = calcular(emptyList(), FiltrosDashboard(fecha = FiltroFecha.SieteDias)).ranking
        assertEquals(4, ranking.first { it.uid == "a2" }.pendientes)
        assertEquals(false, ranking.first { it.uid == "a6" }.activo)
        assertEquals(3, ranking.size)
    }

    @Test
    fun con_un_experto_elegido_el_ranking_solo_lo_muestra_a_el() {
        val ranking = calcular(listOf(obra("1", true, hora(2026, 11, 10))), FiltrosDashboard(expertoUid = "a2")).ranking
        assertEquals(listOf("a2"), ranking.map { it.uid })
        assertEquals(1, ranking.single().evaluadas)
    }

    @Test
    fun socios_de_la_a_a_la_z_con_sin_dato_al_final() {
        val obras = listOf(
            obra("1", true, null, socio = null, numero = null),
            obra("2", true, null, socio = "Rosa Quispe Mamani", numero = "SOC-0001"),
            obra("3", true, null),
            obra("4", false, null)
        )
        assertEquals(
            listOf("María Elena López Díaz · SOC-0002", "Rosa Quispe Mamani · SOC-0001", SOCIO_SIN_DATO),
            calcular(obras).socios
        )
        assertEquals(1, calcular(obras, FiltrosDashboard(socio = SOCIO_SIN_DATO)).dona.total)
    }

    @Test
    fun texto_del_rango() {
        assertEquals("2 nov – 13 nov", FiltroFecha.Rango(diaUtc(2026, 11, 2), diaUtc(2026, 11, 13)).texto())
    }

    @Test
    fun hoy_en_utc_usa_el_dia_del_celular() {
        // 13/11 a las 22:00 en Lima ya es 14/11 en UTC, pero para el celular sigue siendo 13/11
        assertEquals(diaUtc(2026, 11, 13), hoyEnUtc(hora(2026, 11, 13, 22, 0), lima))
    }
}