package com.tecnisis.galeria.domain.repository

/** HU06: fotos de las obras guardadas dentro de la app. Las rutas y los content:// van como texto. */
interface FotoRepository {
    /** Archivo vacío donde la app de cámara guardará la foto. Devuelve su content://. */
    fun crearDestinoCamara(): String
    /** Copia la foto (de la cámara o la galería) dentro de la app, girada y reducida. Devuelve su ruta. */
    suspend fun prepararFoto(origen: String): Result<String>
    /** Borra una foto preparada que ya no se usa. */
    fun borrarFoto(ruta: String)
}