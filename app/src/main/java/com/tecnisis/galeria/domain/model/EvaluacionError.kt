package com.tecnisis.galeria.domain.model

/** HU08: errores al leer las obras asignadas, analizar la foto y registrar el veredicto. */
sealed class EvaluacionError : Exception() {
    data object ComentarioInvalido : EvaluacionError() // al rechazar está vacío, o tiene más de 500 caracteres
    data object SinConexion : EvaluacionError()
    data object SinPermiso : EvaluacionError()         // la sesión venció o la cuenta no es Especialista de Arte
    data object NoAsignada : EvaluacionError()         // la obra no está asignada a este Especialista
    data object YaEvaluada : EvaluacionError()         // la obra ya tiene veredicto
    data object NoEncontrada : EvaluacionError()
    data object FotoNoAnalizada : EvaluacionError()    // ML Kit no pudo leer la foto
    data object PdfNoGenerado : EvaluacionError()      // HU09: no se pudo escribir el PDF de rechazo en el celular
    data object FaltanDocumentos : EvaluacionError()   // HU09: el PDF de rechazo no se subió (no se guardó nada)
    data class Servidor(val detalle: String) : EvaluacionError()
    data object Desconocido : EvaluacionError()
}