package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** HU10: reglas del PVP, del porcentaje, del redondeo y del formato en soles. */
class TasacionTest {

    @Test
    fun pvp_acepta_coma_punto_y_sin_decimales() {
        assertEquals(LecturaPvp.Valido(125_050), leerPvp("1250,50"))
        assertEquals(LecturaPvp.Valido(125_050), leerPvp("1250.50"))
        assertEquals(LecturaPvp.Valido(125_000), leerPvp("1250"))
        assertEquals(LecturaPvp.Valido(1_250), leerPvp("12,5"))
        assertEquals(LecturaPvp.Valido(2_500), leerPvp(" 25 "))
    }

    @Test
    fun pvp_rechaza_separador_de_miles_y_otros_formatos() {
        val formato = LecturaPvp.Invalido(ErrorPvp.FORMATO)
        assertEquals(formato, leerPvp("1,250.50"))
        assertEquals(formato, leerPvp("12,345"))
        assertEquals(formato, leerPvp("1250."))
        assertEquals(formato, leerPvp("-5"))
        assertEquals(formato, leerPvp("12.345"))
        assertEquals(formato, leerPvp("abc"))
    }

    @Test
    fun pvp_debe_ser_mayor_a_cero_y_maximo_un_millon() {
        assertEquals(LecturaPvp.Invalido(ErrorPvp.CERO), leerPvp("0"))
        assertEquals(LecturaPvp.Invalido(ErrorPvp.CERO), leerPvp("0,00"))
        assertEquals(LecturaPvp.Valido(PVP_MAXIMO_CENTIMOS), leerPvp("1000000"))
        assertEquals(LecturaPvp.Invalido(ErrorPvp.MAXIMO), leerPvp("1000000.01"))
        assertEquals(LecturaPvp.Invalido(ErrorPvp.MAXIMO), leerPvp("99999999999999999999"))
        assertEquals(LecturaPvp.Vacio, leerPvp("  "))
    }

    @Test
    fun porcentaje_entero_de_0_a_100() {
        assertEquals(LecturaPorcentaje.Valido(0), leerPorcentaje("0"))
        assertEquals(LecturaPorcentaje.Valido(100), leerPorcentaje("100"))
        assertEquals(LecturaPorcentaje.Invalido, leerPorcentaje("101"))
        assertEquals(LecturaPorcentaje.Invalido, leerPorcentaje("18.5"))
        assertEquals(LecturaPorcentaje.Invalido, leerPorcentaje("-1"))
        assertEquals(LecturaPorcentaje.Vacio, leerPorcentaje(""))
    }

    @Test
    fun montos_suman_el_pvp() {
        assertEquals(MontosTasacion(250_000, 18, 45_000, 205_000), calcularMontos(250_000, 18))
        assertEquals(MontosTasacion(125_050, 30, 37_515, 87_535), calcularMontos(125_050, 30))
        assertEquals(MontosTasacion(80_000, 0, 0, 80_000), calcularMontos(80_000, 0))
        assertEquals(MontosTasacion(100, 100, 100, 0), calcularMontos(100, 100))
    }

    @Test
    fun medio_centimo_sube() {
        assertEquals(1L, calcularMontos(1, 50).galeriaCentimos)   // 0,005 → 0,01
        assertEquals(2L, calcularMontos(3, 50).galeriaCentimos)   // 0,015 → 0,02
        assertEquals(0L, calcularMontos(1, 49).galeriaCentimos)   // 0,0049 → 0,00
    }

    @Test
    fun montos_de_textos() {
        assertEquals(MontosTasacion(250_000, 18, 45_000, 205_000), montosDe("2500", "18"))
        assertNull(montosDe("2500", ""))
        assertNull(montosDe("1,250.50", "18"))
    }

    @Test
    fun soles_en_formato_peruano() {
        assertEquals("S/ 1,250.50", textoSoles(125_050))
        assertEquals("S/ 0.00", textoSoles(0))
        assertEquals("S/ 1,000,000.00", textoSoles(PVP_MAXIMO_CENTIMOS))
        assertEquals("S/ 375.15", textoSoles(37_515))
    }
}