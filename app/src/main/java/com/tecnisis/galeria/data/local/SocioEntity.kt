package com.tecnisis.galeria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tecnisis.galeria.domain.model.Socio

/** Tabla "socio" de SQLite: la ficha del Socio Activo guardada en el celular (HU05). */
@Entity(tableName = "socio")
data class SocioEntity(
    @PrimaryKey val uid: String,
    val numeroSocio: String,
    val dni: String,
    val nombreCompleto: String,
    val direccion: String,
    val sexo: String,
    val telefono: String,
    val correo: String,
    val estadoMembresia: String,
    val fechaAlta: Long?,
    val sincronizadoEn: Long
)

/** De la fila de la tabla al modelo del dominio (las pantallas nunca ven SocioEntity). */
fun SocioEntity.aDominio() = Socio(
    uid = uid,
    numeroSocio = numeroSocio,
    dni = dni,
    nombreCompleto = nombreCompleto,
    direccion = direccion,
    sexo = sexo,
    telefono = telefono,
    correo = correo,
    estadoMembresia = estadoMembresia,
    fechaAlta = fechaAlta,
    sincronizadoEn = sincronizadoEn
)