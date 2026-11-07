package com.tecnisis.galeria.domain.model

/** HU07: las dos copias del documento. Tienen el mismo contenido; solo cambia la franja de arriba. */
enum class CopiaDocumento(val archivo: String, val etiqueta: String) {
    ARCHIVO("archivo.pdf", "Copia · Archivo de la galería"),
    ARTISTA("artista.pdf", "Copia · Artista")
}

/** Ruta en Firebase Storage: documentos/{solicitudId}/archivo.pdf o artista.pdf. */
fun rutaDocumento(solicitudId: String, copia: CopiaDocumento): String =
    "documentos/$solicitudId/${copia.archivo}"

/** Una fila de la tabla de obras del PDF. "ultima" es el Experto (aprobación) o "No admitida" (rechazo). */
data class FilaDocumento(
    val numero: Int,
    val obra: String,
    val tecnica: String,
    val medidas: String,
    val ultima: String
)

/** HU07: todo lo que se escribe en el PDF. Lo arma el dominio; el dibujo lo hace la capa data. */
data class DocumentoSolicitud(
    val tipo: TipoDecision,
    val numero: String,        // "7F3C-2A91": las 8 primeras cifras del id
    val fecha: Long,           // hora de la decisión (reloj del celular del Anfitrión)
    val nombreSocio: String,
    val numeroSocio: String,
    val dni: String,
    val correo: String,
    val filas: List<FilaDocumento>,
    val motivo: String?,       // solo en el rechazo
    val resueltoPor: String    // correo del Anfitrión
) {
    val titulo: String
        get() = if (tipo == TipoDecision.APROBAR) "Constancia de aprobación de la solicitud de exhibición"
        else "Documento de rechazo de la solicitud de exhibición"

    val introduccion: String
        get() = if (tipo == TipoDecision.APROBAR)
            "La galería aprueba la solicitud de exhibición indicada. Cada obra pasa a la evaluación artística a cargo del Experto asignado."
        else
            "La galería no admite la solicitud de exhibición indicada. Las obras listadas quedan como no admitidas. El socio puede presentar una nueva solicitud."

    val columnaFinal: String
        get() = if (tipo == TipoDecision.APROBAR) "Experto asignado" else "Estado"
}

/** "7f3c2a91-0b5e-..." → "7F3C-2A91". */
fun numeroDocumento(solicitudId: String): String {
    val cifras = solicitudId.replace("-", "").take(8).uppercase()
    return if (cifras.length <= 4) cifras else cifras.take(4) + "-" + cifras.drop(4)
}

/** 60.0 → "60"; 22.5 → "22,5". */
fun medidaTexto(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString().replace('.', ',')

/** "60 × 80" o "35 × 22 × 22" (la profundidad solo si la tiene). */
fun medidasTexto(alto: Double, ancho: Double, profundidad: Double?): String =
    listOfNotNull(alto, ancho, profundidad).joinToString(" × ") { medidaTexto(it) }

/**
 * Filas del PDF en el orden de la pantalla. Al aprobar, la última columna es el nombre del Experto
 * asignado; al rechazar, "No admitida".
 */
fun filasDocumento(
    obras: List<ObraEnRevision>,
    tipo: TipoDecision,
    asignaciones: Map<String, String>,
    nombreDe: (String) -> String
): List<FilaDocumento> = obras.mapIndexed { i, obra ->
    FilaDocumento(
        numero = i + 1,
        obra = obra.titulo,
        tecnica = obra.tecnica.etiqueta,
        medidas = medidasTexto(obra.altoCm, obra.anchoCm, obra.profundidadCm),
        ultima = if (tipo == TipoDecision.APROBAR) {
            asignaciones[obra.id]?.let(nombreDe) ?: "—"
        } else {
            EstadoObra.NO_ADMITIDA.etiqueta
        }
    )
}