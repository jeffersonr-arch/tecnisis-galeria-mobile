package com.tecnisis.galeria.domain.model

sealed class ObraError : Exception() {
    data class DatosInvalidos(val campos: Set<CampoObra>) : ObraError()
    data object SinSesion : ObraError()
    data object SolicitudNoEditable : ObraError() // ya se envió: no se edita, borra ni agrega
    data object LimiteDeObras : ObraError()       // el borrador ya tiene 10 obras
    data object SinObras : ObraError()            // se intentó enviar sin obras
    data object NoEncontrada : ObraError()
    data object FotoNoLeida : ObraError()         // no se pudo abrir o reducir la foto
    data object SinConexion : ObraError()         // solo en la subida (WorkManager reintenta)
    data object SinPermiso : ObraError()          // las reglas de Firestore o Storage lo rechazaron
    data object Desconocido : ObraError()
}