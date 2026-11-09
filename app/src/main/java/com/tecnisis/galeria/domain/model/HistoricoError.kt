package com.tecnisis.galeria.domain.model

/** HU09: errores al leer el histórico de obras rechazadas (Gerente). */
sealed class HistoricoError : Exception() {
    data object SinConexion : HistoricoError()
    data object SinPermiso : HistoricoError()   // la sesión venció o la cuenta no es Gerente
    data object Desconocido : HistoricoError()
}