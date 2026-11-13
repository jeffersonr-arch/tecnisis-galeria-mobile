package com.tecnisis.galeria.domain.model

/** HU10: errores al leer las obras por tasar o tasadas y al guardar una tasación. */
sealed class TasacionError : Exception() {
    data object PvpInvalido : TasacionError()        // no cumple leerPvp
    data object PorcentajeInvalido : TasacionError() // no cumple leerPorcentaje
    data object SinConexion : TasacionError()
    data object SinPermiso : TasacionError()         // la sesión venció o la cuenta no es Especialista de Ventas
    data object YaTasada : TasacionError()           // la obra ya no está "aceptada" (ya se tasó)
    data object NoEncontrada : TasacionError()
    data object PdfNoGenerado : TasacionError()      // no se pudo escribir el PDF de venta en el celular
    data object FaltanDocumentos : TasacionError()   // el PDF de venta no se subió (no se guardó nada)
    data class Servidor(val detalle: String) : TasacionError()
    data object Desconocido : TasacionError()
}