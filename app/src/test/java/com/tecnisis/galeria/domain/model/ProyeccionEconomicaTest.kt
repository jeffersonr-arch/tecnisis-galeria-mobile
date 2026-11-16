package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU12: etiquetas, barras (máximo 10 con «Otras»), agrupado por socio y totales de la proyección. */
class ProyeccionEconomicaTest {

    /** Galería = PVP × % ÷ 100 redondeado (la misma regla de la HU10). */
    private fun obra(
        titulo: String, pvp: Long, porcentaje: Int, uid: String = "maria",
        nombre: String? = "María Elena López Díaz", numero: String? = "SOC-0002"
    ): ObraProyectada {
        val galeria = (pvp * porcentaje + 50) / 100
        return ObraProyectada(
            obraId = titulo, uid = uid, titulo = titulo, tecnica = Tecnica.OLEO,
            nombreSocio = nombre, numeroSocio = numero, pvpCentimos = pvp, porcentaje = porcentaje,
            montoGaleriaCentimos = galeria, montoSocioCentimos = pvp - galeria, fechaTasacion = null
        )
    }

    private fun rosa(titulo: String, pvp: Long, porcentaje: Int) =
        obra(titulo, pvp, porcentaje, uid = "rosa", nombre = "Rosa Quispe Mamani", numero = "SOC-0001")

    /** Las 12 obras del lienzo de diseño. */
    private val doce = listOf(
        obra("Mañana, en Paracas", 450_000, 30), rosa("Retablo de Sarhua", 320_000, 35),
        obra("Nocturno limeño", 280_000, 30), rosa("Cerros de Ayacucho", 240_000, 30),
        obra("Tejido de Taquile", 180_000, 35, uid = "sin", nombre = null, numero = null),
        obra("Bodegón con lúcuma", 195_000, 25), rosa("Huaco contemporáneo", 150_000, 30),
        obra("Lluvia en Barranco", 125_050, 30), rosa("Grabado del Rímac", 110_000, 25),
        obra("Retrato de la abuela", 90_000, 25), rosa("Estudio en azul", 65_000, 20),
        obra("Boceto I", 40_000, 0)
    )

    @Test
    fun etiqueta_de_18_caracteres_en_total() {
        assertEquals("SOC-0002 · María…", obra("x", 1, 0).etiquetaSocio)
        assertEquals("SOC-0001 · Rosa Q…", rosa("x", 1, 0).etiquetaSocio)
        assertEquals("Mañana, en Paracas", etiquetaCorta("Mañana, en Paracas"))  // justo 18
        assertEquals("Retrato de la abu…", etiquetaCorta("Retrato de la abuela"))
        assertEquals("Sin dato", obra("x", 1, 0, nombre = null).etiquetaSocio)
        assertEquals("Sin dato", obra("x", 1, 0, nombre = "  ").etiquetaSocio)
    }

    @Test
    fun socio_completo_para_el_pdf() {
        assertEquals("María Elena López Díaz · SOC-0002", obra("x", 1, 0).socio)
        assertEquals("Sin dato", obra("x", 1, 0, nombre = null).socio)
    }

    @Test
    fun diez_obras_son_10_barras_sin_otras_y_once_son_9_mas_otras() {
        val diez = calcularProyeccion(doce.take(10)).barras(AgruparPor.OBRA)
        assertEquals(10, diez.size)
        assertFalse(diez.any { it.esOtras })
        val once = calcularProyeccion(doce.take(11)).barras(AgruparPor.OBRA)
        assertEquals(10, once.size)
        assertEquals(BarraProyeccion("Otras (2)", 35_500, esOtras = true), once.last())
    }

    @Test
    fun obra_con_0_por_ciento_tiene_barra_en_cero() {
        val cuatro = calcularProyeccion(listOf(doce[0], doce[1], doce[7], doce[11])).barras(AgruparPor.OBRA)
        assertEquals(4, cuatro.size)
        assertFalse(cuatro.any { it.esOtras })
        assertEquals(BarraProyeccion("Boceto I", 0), cuatro.last())
    }

    @Test
    fun doce_obras_son_9_barras_mas_otras_al_final() {
        val barras = calcularProyeccion(doce).barras(AgruparPor.OBRA)
        assertEquals(10, barras.size)
        assertEquals(BarraProyeccion("Mañana, en Paracas", 135_000), barras.first())
        assertEquals(BarraProyeccion("Grabado del Rímac", 27_500), barras[8])
        // Otras = Retrato (225.00) + Estudio (130.00) + Boceto (0.00), aunque sume más que la barra 9
        assertEquals(BarraProyeccion("Otras (3)", 35_500, esOtras = true), barras.last())
    }

    @Test
    fun empate_de_ganancia_por_titulo_de_la_a_a_la_z() {
        val p = calcularProyeccion(listOf(obra("Zócalo", 100_000, 10), obra("Árbol", 100_000, 10), obra("Mar", 100_000, 10)))
        assertEquals(listOf("Árbol", "Mar", "Zócalo"), p.obras.map { it.titulo })
    }

    @Test
    fun por_socio_agrupa_por_uid_y_suma_la_ganancia_de_la_galeria() {
        val barras = calcularProyeccion(doce).barras(AgruparPor.SOCIO)
        assertEquals(
            listOf(
                BarraProyeccion("SOC-0002 · María…", 327_765),
                BarraProyeccion("SOC-0001 · Rosa Q…", 269_500),
                BarraProyeccion("Sin dato", 63_000)
            ),
            barras
        )
        val maria = calcularProyeccion(doce).socios.first()
        assertEquals(6, maria.obras)
        assertEquals("María Elena López Díaz · SOC-0002", maria.socio)
    }

    @Test
    fun totales_y_resumen() {
        val p = calcularProyeccion(doce)
        assertEquals(660_265, p.totalGaleriaCentimos)
        assertEquals(2_245_050, p.totalPvpCentimos)
        assertEquals(1_584_785, p.totalSociosCentimos)
        assertEquals("12 obras tasadas · PVP total S/ 22,450.50 · Socios reciben S/ 15,847.85", p.resumen)
        assertTrue(calcularProyeccion(listOf(doce[0])).resumen.startsWith("1 obra tasada ·"))
    }

    @Test
    fun sin_obras_todo_en_cero() {
        val p = calcularProyeccion(emptyList())
        assertEquals(0, p.cantidad)
        assertEquals(0L, p.totalGaleriaCentimos)
        assertTrue(p.barras(AgruparPor.OBRA).isEmpty())
        assertTrue(p.barras(AgruparPor.SOCIO).isEmpty())
    }
}