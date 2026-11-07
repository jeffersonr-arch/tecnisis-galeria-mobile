package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.DatosObra
import com.tecnisis.galeria.domain.model.Obra
import com.tecnisis.galeria.domain.model.SolicitudExhibicion
import kotlinx.coroutines.flow.Flow

/** HU06: el borrador de obras vive en el celular (Room); nada de esto necesita internet. */
interface ObraRepository {
    /** La solicitud más reciente del socio con sus obras. Emite null si nunca armó una. */
    fun observarMiSolicitud(): Flow<SolicitudExhibicion?>
    suspend fun obtenerObra(id: String): Result<Obra>
    /** id null = obra nueva (crea el borrador si hace falta). Devuelve el id de la obra. */
    suspend fun guardarObra(id: String?, datos: DatosObra): Result<String>
    suspend fun eliminarObra(id: String): Result<Unit>
    /** Pasa el borrador a "Pendiente de envío". */
    suspend fun marcarParaEnvio(): Result<Unit>
}