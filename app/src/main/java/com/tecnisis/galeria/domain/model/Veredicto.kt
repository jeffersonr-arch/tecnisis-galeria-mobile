package com.tecnisis.galeria.domain.model

/** HU08: largo máximo del comentario del veredicto (lo revisan la app y la Cloud Function registrarVeredicto). */
const val COMENTARIO_MAXIMO = 500

/**
 * HU08: lo que decide el Especialista de Arte sobre una obra.
 * valorFuncion es lo que recibe la Cloud Function; estado es como queda la obra en Firestore.
 */
enum class TipoVeredicto(val valorFuncion: String, val estado: EstadoObra) {
    ACEPTAR("aceptar", EstadoObra.ACEPTADA),
    RECHAZAR("rechazar", EstadoObra.RECHAZADA)
}

/**
 * Rechazar: comentario obligatorio, de 1 a 500 caracteres sin contar los espacios de los extremos.
 * Aceptar: el comentario es opcional (0 a 500).
 */
fun comentarioValido(tipo: TipoVeredicto, comentario: String): Boolean {
    val largo = comentario.trim().length
    return when (tipo) {
        TipoVeredicto.ACEPTAR -> largo <= COMENTARIO_MAXIMO
        TipoVeredicto.RECHAZAR -> largo in 1..COMENTARIO_MAXIMO
    }
}