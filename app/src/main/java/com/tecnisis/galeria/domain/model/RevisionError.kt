package com.tecnisis.galeria.domain.model

/** HU07: errores al revisar solicitudes (lectura de Firestore, análisis con IA y fotos). */
sealed class RevisionError : Exception() {
    data object SinConexion : RevisionError()
    data object SinPermiso : RevisionError()     // la sesión venció o la cuenta no es Anfitrión
    data object NoEncontrada : RevisionError()
    data object YaResuelta : RevisionError()     // la solicitud ya no está "enviada"
    data class Servidor(val detalle: String) : RevisionError()
    data object Desconocido : RevisionError()
}