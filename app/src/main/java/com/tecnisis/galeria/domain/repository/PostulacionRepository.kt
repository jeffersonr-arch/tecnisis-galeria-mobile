package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.DecisionPostulacion
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
    /** HU04: llama a la Cloud Function. mensaje = nota al aprobar, motivo al rechazar o pedido de información. */
    suspend fun resolver(uid: String, decision: DecisionPostulacion, mensaje: String?): Result<Unit>
}