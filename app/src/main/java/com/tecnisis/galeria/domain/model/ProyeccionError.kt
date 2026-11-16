package com.tecnisis.galeria.domain.model

/** HU12: errores de la proyección económica del Gerente. */
sealed class ProyeccionError : Exception() {
    data object SinConexion : ProyeccionError()
    data object SinPermiso : ProyeccionError()      // la sesión venció o la cuenta no es Gerente
    data object NoSeGenero : ProyeccionError()      // no se pudo escribir el PDF o el CSV
    data object Desconocido : ProyeccionError()
}