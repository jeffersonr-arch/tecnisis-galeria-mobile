package com.tecnisis.galeria.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
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
    Postulacion, EstadoPostulacion, Bandeja, DetallePostulacion,
    Perfil, MisObras, RegistrarObra, SolicitudesObras, DetalleSolicitud, ObrasAsignadas, DetalleObraAsignada, Rechazos, PorTasar, Tasadas,
    DashboardSolicitudes, ProyeccionEconomica,
    ObrasRechazadas
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
        FuncionRol("Mis obras", Pantalla.MisObras, "Registra tus obras y envía tu solicitud", Icons.Outlined.Image)
    )
    Rol.ANFITRION -> listOf(
        FuncionRol("Revisar postulaciones", Pantalla.Bandeja, "Observadas y verificadas por resolver", Icons.Outlined.Inbox),
        FuncionRol("Aprobar solicitudes de exhibición", Pantalla.SolicitudesObras, "Revisa las obras y el Experto recomendado", Icons.Outlined.Description)
    )
    Rol.ESPECIALISTA_ARTE -> listOf(
        FuncionRol("Obras asignadas para evaluar", Pantalla.ObrasAsignadas, "Revisa cada obra y registra tu veredicto",Icons.AutoMirrored.Outlined.FactCheck)
    )
    Rol.ESPECIALISTA_VENTAS -> listOf(
        FuncionRol("Obras pendientes de tasación", Pantalla.PorTasar, "Define el precio y la ganancia de la galería", Icons.Outlined.Sell)
    )
    Rol.GERENTE -> listOf(
        FuncionRol("Obras rechazadas", Pantalla.ObrasRechazadas, "Histórico con el motivo y su documento", Icons.Outlined.Block),
        FuncionRol("Dashboard de solicitudes", null, "Llega en el Sprint 6 (HU11)", Icons.Outlined.DonutLarge),
        FuncionRol("Proyección económica", null, "Llega en el Sprint 6 (HU12)", Icons.Outlined.BarChart)
    )
}