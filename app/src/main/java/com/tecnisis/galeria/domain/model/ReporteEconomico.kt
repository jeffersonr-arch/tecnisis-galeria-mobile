package com.tecnisis.galeria.domain.model

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