package com.tecnisis.galeria.presentation.navigation

import com.tecnisis.galeria.domain.model.Rol

enum class Pantalla {
    Login, Registro,
    Postulante, SocioActivo, Anfitrion, EspecialistaArte, EspecialistaVentas, Gerente,
    Postulacion, EstadoPostulacion, Bandeja, DetallePostulacion, PerfilSocio
}

/** Una tarjeta del inicio; si destino es null, la función llega en un sprint futuro. */
data class FuncionRol(val titulo: String, val destino: Pantalla? = null)

fun Rol.pantallaInicio(): Pantalla = when (this) {
    Rol.POSTULANTE -> Pantalla.Postulante
    Rol.SOCIO_ACTIVO -> Pantalla.SocioActivo
    Rol.ANFITRION -> Pantalla.Anfitrion
    Rol.ESPECIALISTA_ARTE -> Pantalla.EspecialistaArte
    Rol.ESPECIALISTA_VENTAS -> Pantalla.EspecialistaVentas
    Rol.GERENTE -> Pantalla.Gerente
}

fun Rol.funciones(): List<FuncionRol> = when (this) {
    Rol.POSTULANTE -> listOf(
        FuncionRol("Completar mi postulación", Pantalla.Postulacion),
        FuncionRol("Estado de mi postulación", Pantalla.EstadoPostulacion)
    )
    Rol.SOCIO_ACTIVO -> listOf(
        FuncionRol("Mi perfil de socio", Pantalla.PerfilSocio),
        FuncionRol("Registrar obras")
    )
    Rol.ANFITRION -> listOf(
        FuncionRol("Revisar postulaciones", Pantalla.Bandeja),
        FuncionRol("Aprobar solicitudes de exhibición")
    )
    Rol.ESPECIALISTA_ARTE -> listOf(FuncionRol("Obras asignadas para evaluar"))
    Rol.ESPECIALISTA_VENTAS -> listOf(FuncionRol("Obras pendientes de tasación"))
    Rol.GERENTE -> listOf(
        FuncionRol("Dashboard de solicitudes"),
        FuncionRol("Proyección económica")
    )
}