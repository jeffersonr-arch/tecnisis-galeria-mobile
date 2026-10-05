package com.tecnisis.galeria.presentation.navigation

import com.tecnisis.galeria.domain.model.Rol

enum class Pantalla {
    Login, Postulante, SocioActivo, Anfitrion, EspecialistaArte, EspecialistaVentas, Gerente
}

fun Rol.pantallaInicio(): Pantalla = when (this) {
    Rol.POSTULANTE -> Pantalla.Postulante
    Rol.SOCIO_ACTIVO -> Pantalla.SocioActivo
    Rol.ANFITRION -> Pantalla.Anfitrion
    Rol.ESPECIALISTA_ARTE -> Pantalla.EspecialistaArte
    Rol.ESPECIALISTA_VENTAS -> Pantalla.EspecialistaVentas
    Rol.GERENTE -> Pantalla.Gerente
}

fun Rol.funciones(): List<String> = when (this) {
    Rol.POSTULANTE -> listOf("Registrar mi postulación")
    Rol.SOCIO_ACTIVO -> listOf("Mi perfil de socio", "Registrar obras")
    Rol.ANFITRION -> listOf("Revisar postulaciones", "Aprobar solicitudes de exhibición")
    Rol.ESPECIALISTA_ARTE -> listOf("Obras asignadas para evaluar")
    Rol.ESPECIALISTA_VENTAS -> listOf("Obras pendientes de tasación")
    Rol.GERENTE -> listOf("Dashboard de solicitudes", "Proyección económica")
}