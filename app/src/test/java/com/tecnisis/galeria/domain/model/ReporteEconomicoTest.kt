package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** HU12: reparto de las tablas del reporte en hojas A4. */
class ReporteEconomicoTest {

    private val primera = 35
    private val otras = 42

    @Test
    fun doce_obras_y_tres_socios_entran_en_una_hoja() {
        assertEquals(
            listOf(listOf(TramoTabla(TablaReporte.OBRAS, 0, 12), TramoTabla(TablaReporte.SOCIOS, 0, 3))),
            paginarReporte(12, 3, primera, otras)
        )
    }

    @Test
    fun cincuenta_obras_siguen_en_la_hoja_2_con_su_cabecera() {
        val hojas = paginarReporte(50, 3, primera, otras)
        assertEquals(2, hojas.size)
        assertEquals(listOf(TramoTabla(TablaReporte.OBRAS, 0, 33)), hojas[0])          // 35 − 2 de cabecera
        assertEquals(
            listOf(TramoTabla(TablaReporte.OBRAS, 33, 50), TramoTabla(TablaReporte.SOCIOS, 0, 3)),
            hojas[1]
        )
    }

    @Test
    fun la_cabecera_nunca_queda_sola_al_final_de_la_hoja() {
        // 31 obras: quedan 35 − (2 + 31 + 1) = 1 renglón; no entra la cabecera con una fila → hoja 2
        val hojas = paginarReporte(31, 3, primera, otras)
        assertEquals(listOf(TramoTabla(TablaReporte.OBRAS, 0, 31)), hojas[0])
        assertEquals(listOf(TramoTabla(TablaReporte.SOCIOS, 0, 3)), hojas[1])
    }

    @Test
    fun una_tabla_larga_ocupa_varias_hojas() {
        val hojas = paginarReporte(130, 1, primera, otras)
        assertEquals(listOf(33, 40, 40, 17), hojas.map { h -> h.filter { it.tabla == TablaReporte.OBRAS }.sumOf { it.filas } })
        assertEquals(130, hojas.flatten().filter { it.tabla == TablaReporte.OBRAS }.sumOf { it.filas })
        assertEquals(TramoTabla(TablaReporte.SOCIOS, 0, 1), hojas.last().last())
    }

    @Test
    fun sin_filas_queda_una_hoja_vacia() {
        assertEquals(listOf(emptyList<TramoTabla>()), paginarReporte(0, 0, primera, otras))
    }
}