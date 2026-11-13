package com.tecnisis.galeria.domain.model

/** HU10: ruta en Storage de cada copia del documento de venta: documentos_venta/{obraId}/archivo.pdf o artista.pdf. */
fun rutaDocumentoVenta(obraId: String, copia: CopiaDocumento): String =
    "documentos_venta/$obraId/${copia.archivo}"

/**
 * HU10: todo lo que se escribe en el PDF de venta de una obra. Lo arma el dominio; el dibujo lo hace la capa data.
 * Del socio solo van nombre y número.
 */
data class DocumentoVenta(
    val numero: String,           // las 8 primeras cifras del Id de la obra
    val numeroSolicitud: String,  // las 8 primeras cifras del Id de la solicitud
    val fecha: Long,              // hora de la tasación (reloj del celular de Ventas)
    val nombreSocio: String,
    val numeroSocio: String,
    val titulo: String,
    val tecnica: String,
    val medidas: String,          // "60 × 80"
    val montos: MontosTasacion,
    val tasadoPor: String         // nombre del Especialista de Ventas (vacío si no tiene)
) {
    val tituloDocumento: String
        get() = "Documento de venta"

    val introduccion: String
        get() = "El Especialista de Ventas tasó la obra indicada. La obra queda lista para exhibición y venta " +
                "con los montos que se detallan abajo."

    /** "Tasado por: Especialista de Ventas · Nombre"; sin nombre, solo el rol. */
    val firmante: String
        get() = if (tasadoPor.isBlank()) "Tasado por: Especialista de Ventas"
        else "Tasado por: Especialista de Ventas · $tasadoPor"

    /** "Ganancia de la galería (18 %)". */
    val etiquetaGaleria: String
        get() = "Ganancia de la galería (${montos.porcentaje} %)"
}

/** Arma el documento con la obra aceptada, los montos ya calculados y el nombre de quien tasó. */
fun documentoVentaDe(obra: ObraPorTasar, montos: MontosTasacion, tasadoPor: String, fecha: Long) = DocumentoVenta(
    numero = numeroDocumento(obra.id),
    numeroSolicitud = numeroDocumento(obra.solicitudId),
    fecha = fecha,
    nombreSocio = obra.nombreSocioVisible,
    numeroSocio = obra.numeroSocioVisible,
    titulo = obra.titulo,
    tecnica = obra.tecnica.etiqueta,
    medidas = medidasTexto(obra.altoCm, obra.anchoCm, obra.profundidadCm),
    montos = montos,
    tasadoPor = tasadoPor.trim()
)