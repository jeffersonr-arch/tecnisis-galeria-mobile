package com.tecnisis.galeria.domain.model

/** Ficha del Socio Activo: la crea la Cloud Function al aprobar (HU04) y se guarda en Room (HU05). */
data class Socio(
    val uid: String,
    val numeroSocio: String,      // SOC-0001
    val dni: String,
    val nombreCompleto: String,
    val direccion: String,
    val sexo: String,
    val telefono: String,
    val correo: String,
    val estadoMembresia: String,  // "activa" al aprobar
    val fechaAlta: Long?,
    val sincronizadoEn: Long?
)