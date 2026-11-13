package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU10: el documento de venta, su ruta y el aviso al socio. */
class DocumentoVentaTest {

    private val obra = ObraPorTasar(
        id = "5b801d3c-aaaa", solicitudId = "7f3c2a91-bbbb", uid = "u", titulo = "Mañana en Paracas",
        tecnica = Tecnica.OLEO, altoCm = 60.0, anchoCm = 80.0, profundidadCm = null, fotoRuta = "",
        nombreSocio = null, numeroSocio = "SOC-0002", comentarioVeredicto = null, fechaVeredicto = null,
        estado = EstadoObra.ACEPTADA
    )

    @Test
    fun documento_con_los_datos_de_la_obra() {
        val d = documentoVentaDe(obra, calcularMontos(250_000, 18), "  ", 0L)
        assertEquals("5B80-1D3C", d.numero)
        assertEquals("7F3C-2A91", d.numeroSolicitud)
        assertEquals(SIN_DATO, d.nombreSocio)
        assertEquals("SOC-0002", d.numeroSocio)
        assertEquals("60 × 80", d.medidas)
        assertEquals("Tasado por: Especialista de Ventas", d.firmante)
        assertEquals("Ganancia de la galería (18 %)", d.etiquetaGaleria)
    }

    @Test
    fun firmante_con_nombre() {
        val d = documentoVentaDe(obra, calcularMontos(100, 0), "Carla Ruiz", 0L)
        assertEquals("Tasado por: Especialista de Ventas · Carla Ruiz", d.firmante)
    }

    @Test
    fun ruta_de_cada_copia() {
        assertEquals("documentos_venta/o1/archivo.pdf", rutaDocumentoVenta("o1", CopiaDocumento.ARCHIVO))
        assertEquals("documentos_venta/o1/artista.pdf", rutaDocumentoVenta("o1", CopiaDocumento.ARTISTA))
    }

    @Test
    fun aviso_de_la_tasacion() {
        assertEquals("Tu obra «Telar» está lista para exhibición", avisoTasacion("Telar").titulo)
        assertEquals("Tu obra está lista para exhibición", avisoTasacion("").titulo)
        assertEquals("Toca para ver tu documento de venta.", avisoTasacion("Telar").texto)
        assertTrue(idAvisoTasacion("cualquiera") >= 2_000_000)
    }
}