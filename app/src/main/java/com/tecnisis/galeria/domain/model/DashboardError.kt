package com.tecnisis.galeria.domain.model

/** HU11: errores al leer los datos del dashboard del Gerente. */
sealed class DashboardError : Exception() {
    data object SinConexion : DashboardError()
    data object SinPermiso : DashboardError()   // la sesión venció o la cuenta no es Gerente
    data object Desconocido : DashboardError()
}