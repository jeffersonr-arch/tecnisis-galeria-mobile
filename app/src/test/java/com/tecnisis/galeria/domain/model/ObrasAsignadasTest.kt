package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** HU08: cómo se separan y ordenan las obras del Especialista en "Por evaluar" y "Evaluadas". */
class ObrasAsignadasTest {

    private fun obra(
        id: String,
        estado: EstadoObra,
        asignada: Long? = null,
        enviada: Long? = null,
        veredicto: Long? = null,
        socio: String? = "Rosa Quispe Mamani",
        numero: String? = "SOC-0001"
    ) = ObraAsignada(
        id = id, solicitudId = "s", titulo = "Obra $id", tecnica = Tecnica.OLEO, altoCm = 60.0, anchoCm = 80.0,
        profundidadCm = null, nota = "", fotoRuta = "", descripcionIa = null, nombreSocio = socio, numeroSocio = numero,
        estado = estado, fechaAsignacion = asignada, fechaEnvio = enviada, comentarioVeredicto = null,
        fechaVeredicto = veredicto
    )

    @Test
    fun por_evaluar_solo_en_evaluacion_y_la_asignada_hace_mas_tiempo_primero() {
        val obras = listOf(
            obra("b", EstadoObra.EN_EVALUACION, asignada = 200),
            obra("x", EstadoObra.ACEPTADA, asignada = 50, veredicto = 300),
            obra("a", EstadoObra.EN_EVALUACION, asignada = 100)
        )
        assertEquals(listOf("a", "b"), obrasPorEvaluar(obras).map { it.id })
    }

    @Test
    fun sin_fecha_de_asignacion_se_ordena_por_la_de_envio() {
        val obras = listOf(
            obra("nueva", EstadoObra.EN_EVALUACION, asignada = 500),
            obra("vieja", EstadoObra.EN_EVALUACION, asignada = null, enviada = 10)
        )
        assertEquals(listOf("vieja", "nueva"), obrasPorEvaluar(obras).map { it.id })
    }

    @Test
    fun evaluadas_son_aceptadas_y_rechazadas_con_el_veredicto_mas_reciente_primero() {
        val obras = listOf(
            obra("acep", EstadoObra.ACEPTADA, veredicto = 100),
            obra("pend", EstadoObra.EN_EVALUACION, asignada = 999),
            obra("rech", EstadoObra.RECHAZADA, veredicto = 300),
            obra("lista", EstadoObra.LISTA_EXHIBICION, veredicto = 200)
        )
        assertEquals(listOf("rech", "lista", "acep"), obrasEvaluadas(obras).map { it.id })
    }

    @Test
    fun obra_antigua_sin_datos_del_socio_muestra_guion() {
        val antigua = obra("a", EstadoObra.EN_EVALUACION, socio = null, numero = "")
        assertEquals(SIN_DATO, antigua.socioVisible)
        assertEquals(SIN_DATO, antigua.numeroSocioVisible)
        val nueva = obra("n", EstadoObra.EN_EVALUACION)
        assertEquals("Rosa Quispe Mamani", nueva.socioVisible)
        assertEquals("SOC-0001", nueva.numeroSocioVisible)
    }
}