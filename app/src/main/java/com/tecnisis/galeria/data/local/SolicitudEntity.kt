package com.tecnisis.galeria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabla "solicitud" (HU06): una por cada solicitud de exhibición del socio. */
@Entity(tableName = "solicitud")
data class SolicitudEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val estado: String,      // EstadoSolicitud.valorFirestore
    val creadaEn: Long,
    val enviadaEn: Long?
)