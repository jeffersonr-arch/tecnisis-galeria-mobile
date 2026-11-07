package com.tecnisis.galeria.domain.model

/** HU07: errores al aprobar o rechazar una solicitud y al abrir sus documentos. */
sealed class DecisionError : Exception() {
    data object FaltaExperto : DecisionError()        // alguna obra no tiene Experto válido: no se aprueba
    data object MotivoInvalido : DecisionError()      // vacío o con más de 500 caracteres
    data object SinConexion : DecisionError()
    data object SinPermiso : DecisionError()          // la sesión venció o la cuenta no es Anfitrión
    data object YaResuelta : DecisionError()          // otra persona la resolvió antes
    data object ExpertoNoDisponible : DecisionError() // un Experto se ocupó o se desactivó mientras tanto
    data object NoEncontrada : DecisionError()        // la solicitud o el documento ya no existen
    data object PdfNoGenerado : DecisionError()       // no se pudo escribir el PDF en el celular
    data class Servidor(val detalle: String) : DecisionError()
    data object Desconocido : DecisionError()
}