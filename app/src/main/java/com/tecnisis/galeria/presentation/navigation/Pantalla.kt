package com.tecnisis.galeria.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.ui.graphics.vector.ImageVector
import com.tecnisis.galeria.domain.model.Rol

enum class Pantalla {
    Login, Registro,
    Postulante, SocioActivo, Anfitrion, EspecialistaArte, EspecialistaVentas, Gerente,
    Postulacion, EstadoPostulacion, Bandeja, DetallePostulacion, PerfilSocio,
    // HU14: perfil común y pestañas de los sprints siguientes
    Perfil, MisObras, SolicitudesObras, ObrasAsignadas, Rechazos, PorTasar, Tasadas,
    DashboardSolicitudes, ProyeccionEconomica
}

/** Una tarjeta del inicio; si destino es null, la función llega en un sprint futuro. */
data class FuncionRol(
    val titulo: String,
    val destino: Pantalla? = null,
    val subtitulo: String = "",
    val icono: ImageVector = Icons.Outlined.Description
)

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
        FuncionRol("Mi postulación", Pantalla.Postulacion, "Completa y envía tus datos de identidad", Icons.Outlined.Description),
        FuncionRol("Estado de mi postulación", Pantalla.EstadoPostulacion, "Sigue cada paso en tiempo real", Icons.Outlined.Schedule),
        FuncionRol("Registrar obras", null, "Disponible cuando seas socio", Icons.Outlined.Image)
    )
    Rol.SOCIO_ACTIVO -> listOf(
        FuncionRol("Mi perfil de socio", Pantalla.Perfil, "Tus datos y tu membresía", Icons.Outlined.Badge),
        FuncionRol("Registrar obras", null, "Llega en el Sprint 3 (HU06)", Icons.Outlined.Image)
    )
    Rol.ANFITRION -> listOf(
        FuncionRol("Revisar postulaciones", Pantalla.Bandeja, "Observadas y verificadas por resolver", Icons.Outlined.Inbox),
        FuncionRol("Aprobar solicitudes de exhibición", null, "Llega en el Sprint 3 (HU07)", Icons.Outlined.Description)
    )
    Rol.ESPECIALISTA_ARTE -> listOf(
        FuncionRol("Obras asignadas para evaluar", null, "Llega en el Sprint 4 (HU08)",
            Icons.AutoMirrored.Outlined.FactCheck
        )
    )
    Rol.ESPECIALISTA_VENTAS -> listOf(
        FuncionRol("Obras pendientes de tasación", null, "Llega en el Sprint 5 (HU10)", Icons.Outlined.Sell)
    )
    Rol.GERENTE -> listOf(
        FuncionRol("Dashboard de solicitudes", null, "Llega en el Sprint 6 (HU11)", Icons.Outlined.DonutLarge),
        FuncionRol("Proyección económica", null, "Llega en el Sprint 6 (HU12)", Icons.Outlined.BarChart)
    )
}