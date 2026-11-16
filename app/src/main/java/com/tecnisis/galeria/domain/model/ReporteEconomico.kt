package com.tecnisis.galeria.domain.model

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** HU12: las dos tablas del reporte en PDF, en este orden. */
enum class TablaReporte { OBRAS, SOCIOS }

/** HU12: las filas [desde, hasta) de una tabla que entran en una hoja. */
data class TramoTabla(val tabla: TablaReporte, val desde: Int, val hasta: Int) {
    val filas: Int get() = hasta - desde
}

/** HU12: el título de la tabla y su fila de cabecera ocupan 2 renglones. */
const val RENGLONES_CABECERA_TABLA = 2

/**
 * HU12: reparte las filas del reporte en hojas A4. Primero la tabla por obra y después la tabla por socio.
 * [renglonesPrimera]: filas que caben en la hoja 1 (lleva el encabezado y el indicador); [renglonesOtras]: en las demás.
 * Cada tramo de tabla gasta 2 renglones de cabecera (se repite en cada hoja) y 1 de separación al terminar.
 * Una cabecera nunca queda sola al final de una hoja: si no entra con al menos 1 fila, pasa a la hoja siguiente.
 * Siempre devuelve al menos una hoja.
 */
fun paginarReporte(
    filasObras: Int,
    filasSocios: Int,
    renglonesPrimera: Int,
    renglonesOtras: Int
): List<List<TramoTabla>> {
    require(renglonesPrimera > RENGLONES_CABECERA_TABLA && renglonesOtras > RENGLONES_CABECERA_TABLA)
    val hojas = mutableListOf<MutableList<TramoTabla>>(mutableListOf())
    var libres = renglonesPrimera
    for ((tabla, total) in listOf(TablaReporte.OBRAS to filasObras, TablaReporte.SOCIOS to filasSocios)) {
        var desde = 0
        while (desde < total) {
            if (libres < RENGLONES_CABECERA_TABLA + 1) {
                hojas.add(mutableListOf())
                libres = renglonesOtras
            }
            val entran = minOf(total - desde, libres - RENGLONES_CABECERA_TABLA)
            hojas.last().add(TramoTabla(tabla, desde, desde + entran))
            desde += entran
            libres -= RENGLONES_CABECERA_TABLA + entran + 1
        }
    }
    return hojas
}

// ---------- Exportación (PDF y CSV) ----------

/** HU12: formatos del reporte, con la extensión y el tipo que usa el menú Compartir. */
enum class FormatoReporte(val extension: String, val tipoMime: String) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv")
}

/** HU12: un reporte ya escrito en la caché del celular. */
data class ArchivoReporte(val ruta: String, val formato: FormatoReporte)

private val localePe: Locale = Locale.forLanguageTag("es-PE")

/** HU12: "proyeccion-20261115-1430.pdf", con la hora del celular. */
fun nombreReporte(generado: Long, zona: TimeZone, formato: FormatoReporte): String {
    val sello = SimpleDateFormat("yyyyMMdd-HHmm", localePe).apply { timeZone = zona }.format(Date(generado))
    return "proyeccion-$sello.${formato.extension}"
}

/** HU12: "Generado el 15/11/2026 14:30" (va en el PDF). */
fun textoGenerado(generado: Long, zona: TimeZone): String =
    "Generado el " + SimpleDateFormat("dd/MM/yyyy HH:mm", localePe).apply { timeZone = zona }.format(Date(generado))

/** HU12: un campo del CSV. Con coma, comillas o salto de línea va entre comillas y sus comillas se duplican. */
fun campoCsv(texto: String): String =
    if (texto.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + texto.replace("\"", "\"\"") + "\""
    else texto

/** HU12: un monto del CSV: 1250.50, sin "S/" ni separador de miles. */
fun montoCsv(centimos: Long): String = BigDecimal.valueOf(centimos, 2).toPlainString()

/** HU12: columnas del CSV, en la primera fila. */
val CABECERA_CSV = listOf(
    "Obra", "Socio", "Número de socio", "Técnica", "Fecha de tasación",
    "PVP", "Porcentaje", "Ganancia de la galería", "Socio recibe"
)

/**
 * HU12: el texto del CSV (sin BOM: lo agrega quien escribe el archivo). Cabecera en la primera fila y
 * una fila por obra, en el mismo orden de la proyección. Sin «Otras» ni fila de totales. Filas separadas con \r\n.
 */
fun textoCsv(proyeccion: ProyeccionEconomica, zona: TimeZone): String {
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", localePe).apply { timeZone = zona }
    val filas = proyeccion.obras.map { o ->
        listOf(
            o.titulo,
            o.nombreSocio?.trim()?.ifEmpty { null } ?: SOCIO_SIN_DATO,
            o.numeroSocio?.trim().orEmpty(),
            o.tecnica?.etiqueta.orEmpty(),
            o.fechaTasacion?.let { formatoFecha.format(Date(it)) }.orEmpty(),
            montoCsv(o.pvpCentimos),
            o.porcentaje.toString(),
            montoCsv(o.montoGaleriaCentimos),
            montoCsv(o.montoSocioCentimos)
        )
    }
    return (listOf(CABECERA_CSV) + filas).joinToString("") { fila -> fila.joinToString(",") { campoCsv(it) } + "\r\n" }
}