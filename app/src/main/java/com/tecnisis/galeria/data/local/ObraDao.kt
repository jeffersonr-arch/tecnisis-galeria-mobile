package com.tecnisis.galeria.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** HU06: consultas sobre las tablas "solicitud" y "obra". */
@Dao
interface ObraDao {

    /** La solicitud más reciente con sus obras. El Flow emite de nuevo si cambia cualquiera de las dos tablas. */
    @Transaction
    @Query("SELECT * FROM solicitud WHERE uid = :uid ORDER BY creadaEn DESC LIMIT 1")
    fun observarUltima(uid: String): Flow<SolicitudConObras?>

    @Query("SELECT * FROM solicitud WHERE uid = :uid ORDER BY creadaEn DESC LIMIT 1")
    suspend fun ultimaSolicitud(uid: String): SolicitudEntity?

    @Query("SELECT * FROM solicitud WHERE id = :id")
    suspend fun solicitud(id: String): SolicitudEntity?

    @Transaction
    @Query("SELECT * FROM solicitud WHERE uid = :uid AND estado = :estado")
    suspend fun solicitudesConEstado(uid: String, estado: String): List<SolicitudConObras>

    @Query("SELECT * FROM obra WHERE id = :id")
    suspend fun obra(id: String): ObraEntity?

    @Query("SELECT * FROM obra WHERE solicitudId = :solicitudId ORDER BY creadaEn")
    suspend fun obrasDe(solicitudId: String): List<ObraEntity>

    @Query("SELECT COUNT(*) FROM obra WHERE solicitudId = :solicitudId")
    suspend fun contarObras(solicitudId: String): Int

    @Upsert
    suspend fun guardarSolicitud(solicitud: SolicitudEntity)

    @Upsert
    suspend fun guardarObra(obra: ObraEntity)

    @Query("DELETE FROM obra WHERE id = :id")
    suspend fun borrarObra(id: String)

    @Query("DELETE FROM solicitud WHERE id = :id")
    suspend fun borrarSolicitud(id: String)

    @Query("UPDATE solicitud SET estado = :estado, enviadaEn = :enviadaEn WHERE id = :id")
    suspend fun cambiarEstadoSolicitud(id: String, estado: String, enviadaEn: Long?)

    @Query("UPDATE obra SET estado = :estado WHERE solicitudId = :solicitudId")
    suspend fun cambiarEstadoObras(solicitudId: String, estado: String)

    @Query("UPDATE obra SET fotoUrl = :url WHERE id = :id")
    suspend fun marcarFotoSubida(id: String, url: String)
}