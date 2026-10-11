package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import kotlinx.coroutines.flow.Flow

interface PostulacionRepository {
    /** Guarda la postulación del usuario actual y devuelve su uid. */
    suspend fun enviar(postulacion: Postulacion): Result<String>
    fun observarMiPostulacion(): Flow<Postulacion?>
    suspend fun obtener(uid: String): Result<Postulacion>
    suspend fun registrarVerificacion(uid: String, estado: EstadoPostulacion, motivo: String?): Result<Unit>
    fun observarPorEstado(estado: EstadoPostulacion): Flow<List<Postulacion>>
    suspend fun resolver(uid: String, aprobar: Boolean, motivoRechazo: String?): Result<Unit>
}