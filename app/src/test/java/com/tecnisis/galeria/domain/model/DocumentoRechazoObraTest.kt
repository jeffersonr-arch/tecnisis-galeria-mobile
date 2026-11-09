package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU09: ruta del documento, datos que van en el PDF, histórico del Gerente y aviso al socio. */
class DocumentoRechazoObraTest {

    private fun obra(socio: String? = "María Elena López Díaz", profundidad: Double? = null) = ObraAsignada(
        id = "9c214e7b-1111", solicitudId = "7f3c2a91-2222", titulo = "Telar de Ocongate", tecnica = Tecnica.TEXTIL,
        altoCm = 120.0, anchoCm = 90.0, profundidadCm = profundidad, nota = "", fotoRuta = "", descripcionIa = null,
        nombreSocio = socio, numeroSocio = socio?.let { "SOC-0002" }, estado = EstadoObra.EN_EVALUACION,
        fechaAsignacion = 1L, fechaEnvio = null, comentarioVeredicto = null, fechaVeredicto = null
    )

    private fun rechazada(id: String, fecha: Long?, documento: Boolean = true) = ObraRechazada(
        id = id, titulo = "Obra $id", tecnica = Tecnica.OLEO, altoCm = 60.0, anchoCm = 80.0, profundidadCm = null,
        nombreSocio = null, numeroSocio = null, evaluadaPorNombre = null, motivo = "Motivo", fechaVeredicto = fecha,
        documentoRechazo = if (documento) listOf("a", "b") else emptyList()
    )

    @Test
    fun ruta_de_cada_copia() {
        assertEquals("documentos_obras/o1/archivo.pdf", rutaDocumentoObra("o1", CopiaDocumento.ARCHIVO))
        assertEquals("documentos_obras/o1/artista.pdf", rutaDocumentoObra("o1", CopiaDocumento.ARTISTA))
    }

    @Test
    fun documento_con_numeros_medidas_y_motivo_sin_espacios() {
        val d = documentoRechazoDe(obra(profundidad = 22.5), "  Bordes deshilachados  ", "Jorge Huamán Ccori", 5L)
        assertEquals("9C21-4E7B", d.numero)
        assertEquals("7F3C-2A91", d.numeroSolicitud)
        assertEquals("120 × 90 × 22,5", d.medidas)
        assertEquals("Textil", d.tecnica)
        assertEquals("Bordes deshilachados", d.motivo)
        assertEquals("SOC-0002", d.numeroSocio)
    }

    @Test
    fun obra_antigua_sin_socio_ni_especialista_muestra_guion() {
        val d = documentoRechazoDe(obra(socio = null), "Motivo", "", 5L)
        assertEquals(SIN_DATO, d.nombreSocio)
        assertEquals(SIN_DATO, d.numeroSocio)
        assertEquals(SIN_DATO, d.evaluadoPor)
    }

    @Test
    fun historico_ordena_del_mas_reciente_al_mas_antiguo_y_sin_fecha_al_final() {
        val orden = historicoRechazos(listOf(rechazada("a", 10L), rechazada("b", null), rechazada("c", 30L)))
        assertEquals(listOf("c", "a", "b"), orden.map { it.id })
    }

    @Test
    fun obra_rechazada_antigua_sin_datos_ni_documento() {
        val r = rechazada("a", 10L, documento = false)
        assertEquals(SIN_DATO, r.socioVisible)
        assertEquals(SIN_DATO, r.especialistaVisible)
        assertFalse(r.tieneDocumento)
        assertEquals("Óleo · 60 × 80 cm", r.detalle)
    }

    @Test
    fun obra_asignada_sabe_si_tiene_documento() {
        assertFalse(obra().tieneDocumentoRechazo)
        assertTrue(obra().copy(documentoRechazo = listOf("a", "b")).tieneDocumentoRechazo)
    }

    @Test
    fun aviso_de_rechazo_menciona_el_documento() {
        assertEquals("Toca para ver el motivo y tu documento.", avisoVeredicto(aceptada = false, tituloObra = "Telar").texto)
    }
}