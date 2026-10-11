package com.tecnisis.galeria.domain.model

data class Socio(
    val uid: String,
    val dni: String,
    val nombreCompleto: String,
    val direccion: String,
    val sexo: String,
    val telefono: String,
    val correo: String,
    val sincronizadoEn: Long?
)