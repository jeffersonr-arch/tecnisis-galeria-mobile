package com.tecnisis.galeria.domain.model

/**
 * HU10: un Documento de Venta (documentos_venta/{obraId}), tal como lo ve el Especialista de Ventas en Tasadas.
 * Lo escribe solo la Cloud Function registrarTasacion; los montos van en céntimos.
 */
data class ObraTasada(
    val obraId: String,
    val titulo: String,
    val tecnica: Tecnica,
    val fotoRuta: String,
    val nombreSocio: String?,
    val numeroSocio: String?,
    val pvpCentimos: Long,
    val porcentaje: Int,
    val montoGaleriaCentimos: Long,
    val montoSocioCentimos: Long,
    val fechaTasacion: Long?
)

/** Pestaña "Tasadas": la tasada más reciente primero (sin fecha, al final). */
fun obrasTasadas(obras: List<ObraTasada>): List<ObraTasada> =
    obras.sortedByDescending { it.fechaTasacion ?: 0L }