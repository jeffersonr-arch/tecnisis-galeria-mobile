package com.tecnisis.galeria.domain.model

/** HU09: ruta en Storage de cada copia del documento de rechazo de una obra: documentos_obras/{obraId}/archivo.pdf o artista.pdf. */
fun rutaDocumentoObra(obraId: String, copia: CopiaDocumento): String =
    "documentos_obras/$obraId/${copia.archivo}"

/**
 * HU09: todo lo que se escribe en el PDF de rechazo de una obra. Lo arma el dominio; el dibujo lo hace la capa data.
 * Del socio solo van nombre y número (los mismos que ve el Especialista en la obra).
 */
data class DocumentoRechazoObra(
    val numero: String,           // "9C21-4E7B": las 8 primeras cifras del Id de la obra
    val numeroSolicitud: String,  // las 8 primeras cifras del Id de la solicitud
    val fecha: Long,              // hora del veredicto (reloj del celular del Especialista)
    val fechaAsignacion: Long?,   // cuándo se aprobó la solicitud; null en obras antiguas
    val nombreSocio: String,
    val numeroSocio: String,
    val titulo: String,
    val tecnica: String,
    val medidas: String,          // "120 × 90"
    val motivo: String,           // el comentario del veredicto
    val evaluadoPor: String       // nombre del Especialista de Arte
) {
    val tituloDocumento: String
        get() = "Documento de rechazo de obra"

    val introduccion: String
        get() = "El Especialista de Arte evaluó la obra indicada y no la admite para la exhibición. " +
                "La obra no pasa a la evaluación económica. El motivo se detalla abajo."
}

/** Arma el documento con los datos de la obra asignada, el motivo y el nombre del Especialista. */
fun documentoRechazoDe(obra: ObraAsignada, motivo: String, evaluadoPor: String, fecha: Long) = DocumentoRechazoObra(
    numero = numeroDocumento(obra.id),
    numeroSolicitud = numeroDocumento(obra.solicitudId),
    fecha = fecha,
    fechaAsignacion = obra.fechaAsignacion,
    nombreSocio = obra.socioVisible,
    numeroSocio = obra.numeroSocioVisible,
    titulo = obra.titulo,
    tecnica = obra.tecnica.etiqueta,
    medidas = medidasTexto(obra.altoCm, obra.anchoCm, obra.profundidadCm),
    motivo = motivo.trim(),
    evaluadoPor = evaluadoPor.ifBlank { SIN_DATO }
)