package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU06: reglas del formulario de obra, sin Android ni Firebase. */
class FormularioObraTest {

    private val completo = FormularioObra(
        titulo = "Mañana en Paracas", tecnica = Tecnica.OLEO,
        alto = "60", ancho = "80", fotoLocal = "/data/obras/foto.jpg"
    )

    @Test
    fun formulario_completo_no_tiene_errores() {
        assertTrue(completo.camposInvalidos().isEmpty())
    }

    @Test
    fun formulario_vacio_marca_titulo_tecnica_medidas_y_foto() {
        assertEquals(
            setOf(CampoObra.TITULO, CampoObra.TECNICA, CampoObra.ALTO, CampoObra.ANCHO, CampoObra.FOTO),
            FormularioObra().camposInvalidos()
        )
    }

    @Test
    fun titulo_con_solo_espacios_es_invalido() {
        assertEquals(setOf(CampoObra.TITULO), completo.copy(titulo = "   ").camposInvalidos())
    }

    @Test
    fun medida_cero_negativa_o_texto_es_invalida() {
        assertNull(leerMedida("0"))
        assertNull(leerMedida("0,04")) // redondea a 0.0
        assertNull(leerMedida("-3"))
        assertNull(leerMedida("abc"))
        assertNull(leerMedida("NaN"))
        assertNull(leerMedida("1000,1"))
    }

    @Test
    fun medida_acepta_coma_y_redondea_a_un_decimal() {
        assertEquals(60.3, leerMedida("60,25")!!, 0.0)
        assertEquals(0.1, leerMedida("0,05")!!, 0.0)
        assertEquals(1000.0, leerMedida(" 1000 ")!!, 0.0)
    }

    @Test
    fun profundidad_vacia_vale_pero_cero_no() {
        assertTrue(completo.copy(profundidad = "").camposInvalidos().isEmpty())
        assertEquals(setOf(CampoObra.PROFUNDIDAD), completo.copy(profundidad = "0").camposInvalidos())
    }

    @Test
    fun aDatos_limpia_espacios_y_deja_profundidad_null() {
        val datos = completo.copy(titulo = "  Mañana en Paracas ", nota = "  Amanecer  ").aDatos()
        assertEquals("Mañana en Paracas", datos.titulo)
        assertEquals("Amanecer", datos.nota)
        assertNull(datos.profundidadCm)
    }
}