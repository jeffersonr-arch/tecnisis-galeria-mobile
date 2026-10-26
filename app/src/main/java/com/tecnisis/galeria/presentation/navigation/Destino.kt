package com.tecnisis.galeria.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import com.tecnisis.galeria.domain.model.Rol

/** Una pestaña de la barra inferior. El primer destino de cada rol siempre es su inicio. */
data class Destino(val etiqueta: String, val icono: ImageVector, val pantalla: Pantalla)

private val DestinoPerfil = Destino("Perfil", Icons.Outlined.Person, Pantalla.Perfil)

fun Rol.destinos(): List<Destino> = when (this) {
    Rol.POSTULANTE -> listOf(
        Destino("Inicio", Icons.Outlined.Home, Pantalla.Postulante),
        Destino("Postulación", Icons.Outlined.AssignmentTurnedIn, Pantalla.EstadoPostulacion),
        DestinoPerfil
    )
    Rol.SOCIO_ACTIVO -> listOf(
        Destino("Inicio", Icons.Outlined.Home, Pantalla.SocioActivo),
        Destino("Mis obras", Icons.Outlined.Image, Pantalla.MisObras),      // HU06
        DestinoPerfil                                                      // HU05
    )
    Rol.ANFITRION -> listOf(
        Destino("Panel", Icons.Outlined.Dashboard, Pantalla.Anfitrion),
        Destino("Postulaciones", Icons.Outlined.Inbox, Pantalla.Bandeja),  // HU04
        Destino("Solicitudes", Icons.Outlined.Description, Pantalla.SolicitudesObras), // HU07
        DestinoPerfil
    )
    Rol.ESPECIALISTA_ARTE -> listOf(
        Destino("Inicio", Icons.Outlined.Home, Pantalla.EspecialistaArte),
        Destino("Asignadas", Icons.AutoMirrored.Outlined.FactCheck, Pantalla.ObrasAsignadas), // HU08
        Destino("Rechazos", Icons.Outlined.Block, Pantalla.Rechazos),            // HU09
        DestinoPerfil
    )
    Rol.ESPECIALISTA_VENTAS -> listOf(
        Destino("Inicio", Icons.Outlined.Home, Pantalla.EspecialistaVentas),
        Destino("Por tasar", Icons.Outlined.Sell, Pantalla.PorTasar),  // HU10
        Destino("Tasadas", Icons.Outlined.TaskAlt, Pantalla.Tasadas),  // HU10
        DestinoPerfil
    )
    Rol.GERENTE -> listOf(
        Destino("Panel", Icons.Outlined.Dashboard, Pantalla.Gerente),
        Destino("Solicitudes", Icons.Outlined.DonutLarge, Pantalla.DashboardSolicitudes), // HU11
        Destino("Proyección", Icons.Outlined.BarChart, Pantalla.ProyeccionEconomica),    // HU12
        DestinoPerfil
    )
}