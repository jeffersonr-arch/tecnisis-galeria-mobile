package com.tecnisis.galeria.domain.model

/** HU07: máximo de obras sin veredicto que un Especialista de Arte puede tener a la vez. */
const val CARGA_MAXIMA = 5

/** HU07: el estado del Especialista lo pone una persona. "Ocupado" no se guarda: se calcula con la carga. */
enum class EstadoEspecialista(val valorFirestore: String) {
    ACTIVO("activo"),
    INACTIVO("inactivo");

    companion object {
        /** Un valor desconocido cuenta como inactivo: así nunca se asigna a alguien por error. */
        fun desdeValor(valor: String?): EstadoEspecialista =
            entries.firstOrNull { it.valorFirestore == valor } ?: INACTIVO
    }
}

/**
 * HU07: ficha de trabajo de un Especialista de Arte (colección expertos/{uid}, mismo uid que usuarios/{uid}).
 * No es otro actor: es el mismo Especialista de Arte, con lo que necesita la recomendación.
 * En el proceso de la galería cumple el papel de "Experto" de la obra (por eso la colección se llama expertos).
 */
data class PerfilEspecialista(
    val uid: String,
    val nombre: String,
    val tecnicas: Set<Tecnica>,
    val estado: EstadoEspecialista,
    val carga: Int // obras asignadas sin veredicto
) {
    fun domina(tecnica: Tecnica): Boolean = tecnica in tecnicas

    /** Disponible = activo y con cupo, contando las obras de esta misma solicitud que ya lo tienen elegido. */
    fun disponibleCon(extra: Int = 0): Boolean =
        estado == EstadoEspecialista.ACTIVO && carga + extra < CARGA_MAXIMA
}