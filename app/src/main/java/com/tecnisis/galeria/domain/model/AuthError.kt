package com.tecnisis.galeria.domain.model

sealed class AuthError : Exception() {
    data object CredencialesInvalidas : AuthError()
    data object UsuarioDeshabilitado : AuthError()
    data object SinConexion : AuthError()
    data object DemasiadosIntentos : AuthError()
    data object SinRol : AuthError()
    data object SinSesion : AuthError()
    data object Desconocido : AuthError()
}