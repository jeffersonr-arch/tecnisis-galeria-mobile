package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.CopiaDocumento

/** HU09: abrir el documento de rechazo de una obra (lo usan el Especialista, el socio, el Anfitrión y el Gerente). */
interface DocumentoObraRepository {
    /**
     * Descarga una copia de documentos_obras/{obraId}/ a la caché del celular y devuelve la ruta del archivo.
     * Si el archivo no existe (obra rechazada antes de la HU09) falla con DecisionError.NoEncontrada.
     */
    suspend fun descargar(obraId: String, copia: CopiaDocumento): Result<String>
}