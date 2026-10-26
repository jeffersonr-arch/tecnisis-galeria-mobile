package com.tecnisis.galeria.domain.model

sealed class SocioError : Exception() {
    data object SinSesion : SocioError()
    data object SinConexion : SocioError()
    data object SinFicha : SocioError()   // todavía no existe socios/{uid} (no fue aprobado)
    data object Desconocido : SocioError()
}