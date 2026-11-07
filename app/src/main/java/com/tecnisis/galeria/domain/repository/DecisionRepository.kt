package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionSolicitud

/** HU07: aprobar o rechazar una solicitud de exhibición y abrir su documento por duplicado. */
interface DecisionRepository {
    /**
     * Genera los 2 PDF en el celular, los sube a Storage y llama a la Cloud Function decidirSolicitud,
     * que cambia la solicitud, sus obras y la carga de los Expertos, y avisa al socio.
     */
    suspend fun decidir(decision: DecisionSolicitud): Result<Unit>

    /** Descarga una copia del documento a la caché del celular y devuelve la ruta del archivo. */
    suspend fun descargarDocumento(solicitudId: String, copia: CopiaDocumento): Result<String>
}