package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU08: reglas del comentario del veredicto. */
class VeredictoTest {

    @Test
    fun aceptar_sin_comentario_es_valido() {
        assertTrue(comentarioValido(TipoVeredicto.ACEPTAR, ""))
        assertTrue(comentarioValido(TipoVeredicto.ACEPTAR, "   "))
    }

    @Test
    fun aceptar_con_mas_de_500_no_es_valido() {
        assertTrue(comentarioValido(TipoVeredicto.ACEPTAR, "a".repeat(500)))
        assertFalse(comentarioValido(TipoVeredicto.ACEPTAR, "a".repeat(501)))
    }

    @Test
    fun rechazar_vacio_o_solo_espacios_no_es_valido() {
        assertFalse(comentarioValido(TipoVeredicto.RECHAZAR, ""))
        assertFalse(comentarioValido(TipoVeredicto.RECHAZAR, "    "))
    }

    @Test
    fun rechazar_con_1_y_con_500_es_valido() {
        assertTrue(comentarioValido(TipoVeredicto.RECHAZAR, "x"))
        assertTrue(comentarioValido(TipoVeredicto.RECHAZAR, "a".repeat(500)))
    }

    @Test
    fun rechazar_cuenta_sin_los_espacios_de_los_extremos() {
        assertTrue(comentarioValido(TipoVeredicto.RECHAZAR, "  " + "a".repeat(500) + "  "))
        assertFalse(comentarioValido(TipoVeredicto.RECHAZAR, "a".repeat(501)))
    }

    @Test
    fun cada_veredicto_deja_su_estado() {
        assertEquals(EstadoObra.ACEPTADA, TipoVeredicto.ACEPTAR.estado)
        assertEquals(EstadoObra.RECHAZADA, TipoVeredicto.RECHAZAR.estado)
        assertEquals("aceptar", TipoVeredicto.ACEPTAR.valorFuncion)
        assertEquals("rechazar", TipoVeredicto.RECHAZAR.valorFuncion)
    }
}