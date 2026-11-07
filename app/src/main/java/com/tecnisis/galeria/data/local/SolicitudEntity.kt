package com.tecnisis.galeria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabla "solicitud" (HU06). Versión 3 (HU07): + motivoRechazo y resueltaEn. */
@Entity(tableName = "solicitud")
data class SolicitudEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val estado: String,      // EstadoSolicitud.valorFirestore
    val creadaEn: Long,
    val enviadaEn: Long?,
    val motivoRechazo: String? = null, // HU07: lo baja la sincronización si el Anfitrión rechazó
    val resueltaEn: Long? = null       // HU07: cuándo se aprobó o rechazó
)