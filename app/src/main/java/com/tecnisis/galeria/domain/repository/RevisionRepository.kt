package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.EstadoAnalisis
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.SolicitudEnRevision
import com.tecnisis.galeria.domain.model.SolicitudResumen
import kotlinx.coroutines.flow.Flow

/** HU07: lo que necesita el Anfitrión para revisar las solicitudes de exhibición. */
interface RevisionRepository {
    /** En tiempo real. Resueltas = falso: "enviada"; true: "aprobada" y "rechazada". */
    fun observarSolicitudes(resueltas: Boolean): Flow<List<SolicitudResumen>>
    /** La solicitud con sus obras, en tiempo real. Emite null si no existe. */
    fun observarSolicitud(id: String): Flow<SolicitudEnRevision?>
    fun observarExpertos(): Flow<List<PerfilEspecialista>>
    /** Llama con Retrofit a la función analizarSolicitud. Devuelve el estado que respondió. */
    suspend fun analizar(id: String): Result<EstadoAnalisis>
    /** Descarga la foto de una obra desde Storage (ruta obras/{uid}/{idObra}.jpg). */
    suspend fun descargarFoto(ruta: String): Result<ByteArray>
}