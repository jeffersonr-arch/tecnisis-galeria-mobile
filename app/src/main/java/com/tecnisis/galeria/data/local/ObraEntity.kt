package com.tecnisis.galeria.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.Obra
import com.tecnisis.galeria.domain.model.Tecnica

/** Tabla "obra" (HU06). El índice acelera "las obras de esta solicitud". */
@Entity(tableName = "obra", indices = [Index("solicitudId")])
data class ObraEntity(
    @PrimaryKey val id: String,
    val solicitudId: String,
    val uid: String,
    val titulo: String,
    val tecnica: String,     // Tecnica.valorFirestore
    val altoCm: Double,
    val anchoCm: Double,
    val profundidadCm: Double?,
    val fotoLocal: String,
    val fotoUrl: String?,
    val nota: String,
    val estado: String,      // EstadoObra.valorFirestore
    val creadaEn: Long
)

fun ObraEntity.aDominio() = Obra(
    id = id,
    solicitudId = solicitudId,
    titulo = titulo,
    tecnica = Tecnica.desdeValor(tecnica) ?: Tecnica.TECNICA_MIXTA,
    altoCm = altoCm,
    anchoCm = anchoCm,
    profundidadCm = profundidadCm,
    fotoLocal = fotoLocal,
    fotoUrl = fotoUrl,
    nota = nota,
    estado = EstadoObra.desdeValor(estado),
    creadaEn = creadaEn
)