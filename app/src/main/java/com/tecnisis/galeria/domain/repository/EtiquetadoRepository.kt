package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.EtiquetaCruda

/** HU08: clasificación de la foto de una obra en el celular (ML Kit Image Labeling, sin internet). */
interface EtiquetadoRepository {
    /** Recibe los bytes del JPG y devuelve las etiquetas tal como las da ML Kit (en inglés, con su confianza). */
    suspend fun etiquetar(foto: ByteArray): Result<List<EtiquetaCruda>>
}