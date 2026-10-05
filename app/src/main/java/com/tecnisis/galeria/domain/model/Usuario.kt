package com.tecnisis.galeria.domain.model

data class Usuario(
    val uid: String,
    val nombre: String,
    val correo: String,
    val rol: Rol
)