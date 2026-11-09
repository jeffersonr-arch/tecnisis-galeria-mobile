package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU08: umbral de 50 %, máximo 5 y diccionario de las etiquetas de ML Kit. */
class EtiquetasImagenTest {

    @Test
    fun el_umbral_incluye_50_y_deja_fuera_49() {
        val visibles = etiquetasVisibles(listOf(EtiquetaCruda("Sky", 0.50f), EtiquetaCruda("Boat", 0.49f)))
        assertEquals(listOf("Cielo"), visibles.map { it.nombre })
        assertEquals(50, visibles.first().porcentaje)
    }

    @Test
    fun como_maximo_cinco_de_mayor_a_menor_confianza() {
        val crudas = listOf(
            EtiquetaCruda("Pier", 0.55f), EtiquetaCruda("Sky", 0.91f), EtiquetaCruda("Boat", 0.78f),
            EtiquetaCruda("Sunset", 0.66f), EtiquetaCruda("Pattern", 0.57f), EtiquetaCruda("Sand", 0.52f)
        )
        assertEquals(
            listOf("Cielo", "Barco", "Atardecer", "Patrón", "Muelle"),
            etiquetasVisibles(crudas).map { it.nombre }
        )
    }

    @Test
    fun traduce_las_cuatro_del_lienzo() {
        val visibles = etiquetasVisibles(
            listOf(EtiquetaCruda("Sky", 0.9f), EtiquetaCruda("Boat", 0.8f), EtiquetaCruda("Sunset", 0.7f), EtiquetaCruda("Pattern", 0.6f))
        )
        assertEquals(listOf("Cielo", "Barco", "Atardecer", "Patrón"), visibles.map { it.nombre })
        assertTrue(visibles.all { it.traducida })
    }

    @Test
    fun la_que_no_esta_en_el_diccionario_queda_en_ingles() {
        val visibles = etiquetasVisibles(listOf(EtiquetaCruda("Brig", 0.52f)))
        assertEquals("Brig", visibles.first().nombre)
        assertFalse(visibles.first().traducida)
    }

    @Test
    fun el_porcentaje_se_redondea() {
        assertEquals(67, etiquetasVisibles(listOf(EtiquetaCruda("Sky", 0.666f))).first().porcentaje)
        assertEquals(100, etiquetasVisibles(listOf(EtiquetaCruda("Sky", 0.999f))).first().porcentaje)
    }

    @Test
    fun sin_etiquetas_devuelve_lista_vacia() {
        assertTrue(etiquetasVisibles(emptyList()).isEmpty())
        assertTrue(etiquetasVisibles(listOf(EtiquetaCruda("Sky", 0.3f))).isEmpty())
    }
}