package com.tecnisis.galeria.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.SolicitudExhibicion

/** Room arma esto solo: la fila de la solicitud y todas las obras con su solicitudId. */
data class SolicitudConObras(
    @Embedded val solicitud: SolicitudEntity,
    @Relation(parentColumn = "id", entityColumn = "solicitudId")
    val obras: List<ObraEntity>
)

fun SolicitudConObras.aDominio() = SolicitudExhibicion(
    id = solicitud.id,
    estado = EstadoSolicitud.desdeValor(solicitud.estado),
    creadaEn = solicitud.creadaEn,
    enviadaEn = solicitud.enviadaEn,
    obras = obras.sortedBy { it.creadaEn }.map { it.aDominio() }
)