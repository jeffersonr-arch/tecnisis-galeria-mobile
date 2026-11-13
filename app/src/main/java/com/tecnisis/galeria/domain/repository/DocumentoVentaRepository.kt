package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.CopiaDocumento

/** HU10: abrir el documento de venta de una obra (lo usan el Especialista de Ventas y el socio). */
interface DocumentoVentaRepository {
    /**
     * Descarga una copia de documentos_venta/{obraId}/ a la caché del celular y devuelve la ruta del archivo.
     * Si el archivo no existe falla con DecisionError.NoEncontrada (los mismos errores que en la HU09).
     */
    suspend fun descargar(obraId: String, copia: CopiaDocumento): Result<String>
}