package com.tecnisis.galeria.domain.model

sealed class PostulacionError : Exception() {
    data object DniYaRegistrado : PostulacionError()
    data object YaPostulo : PostulacionError()
    data object SinConexion : PostulacionError()
    data object SinPermiso : PostulacionError()
    data object NoEncontrada : PostulacionError()
    data object MensajeObligatorio : PostulacionError()  // HU04: falta el motivo, la nota o el mensaje
    data object DecisionNoPermitida : PostulacionError() // HU04: ya resuelta o el estado no admite esa decisión
    data class Servidor(val detalle: String) : PostulacionError()
    data object Desconocido : PostulacionError()
}