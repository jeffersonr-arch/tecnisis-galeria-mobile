package com.tecnisis.galeria.domain.model

sealed class PostulacionError : Exception() {
    data object DniYaRegistrado : PostulacionError()
    data object YaPostulo : PostulacionError()
    data object SinConexion : PostulacionError()
    data object SinPermiso : PostulacionError()
    data object NoEncontrada : PostulacionError()
    data class Servidor(val detalle: String) : PostulacionError()
    data object Desconocido : PostulacionError()
}