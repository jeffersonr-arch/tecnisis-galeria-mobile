package com.tecnisis.galeria.domain.model

enum class Rol(val valorFirestore: String, val etiqueta: String) {
    POSTULANTE("postulante", "Postulante"),
    SOCIO_ACTIVO("socio_activo", "Socio Activo"),
    ANFITRION("anfitrion", "Anfitrión"),
    ESPECIALISTA_ARTE("especialista_arte", "Especialista de Arte"),
    ESPECIALISTA_VENTAS("especialista_ventas", "Especialista de Ventas"),
    GERENTE("gerente", "Gerente");

    companion object {
        fun desdeFirestore(valor: String?): Rol? =
            entries.firstOrNull { it.valorFirestore == valor?.trim()?.lowercase() }
    }
}