package com.tecnisis.galeria.domain.model

/** HU07: largo máximo del motivo de rechazo (lo revisan la app y la Cloud Function). */
const val MOTIVO_MAXIMO = 500

/** HU07: lo que decide el Anfitrión. valorFuncion es lo que recibe la Cloud Function decidirSolicitud. */
enum class TipoDecision(val valorFuncion: String) {
    APROBAR("aprobar"),
    RECHAZAR("rechazar")
}

/** HU07: la decisión completa que viaja a la Cloud Function. */
data class DecisionSolicitud(
    val solicitudId: String,
    val tipo: TipoDecision,
    val asignaciones: Map<String, String>, // obraId → uid del Experto (solo al aprobar)
    val motivo: String                     // solo al rechazar; ya recortado
)

/** El motivo es obligatorio: de 1 a 500 caracteres sin contar los espacios de los extremos. */
fun motivoValido(motivo: String): Boolean = motivo.trim().length in 1..MOTIVO_MAXIMO

/**
 * Obra → uid del Experto para aprobar. Devuelve null si alguna obra no tiene un Experto válido:
 * en ese caso no se puede aprobar (criterio de la HU07).
 */
fun asignacionesDe(
    obras: List<ObraEnRevision>,
    expertos: List<PerfilEspecialista>,
    seleccion: Map<String, String?>
): Map<String, String>? {
    if (!todasConExperto(obras, expertos, seleccion)) return null
    return obras.associate { obra -> obra.id to expertoValido(obra, expertos, seleccion)!!.uid }
}

/**
 * Texto que el diálogo de rechazo ofrece con un toque. Nombra las técnicas sin Experto disponible.
 * Devuelve null si todas las obras tienen Experto (el Anfitrión igual puede rechazar, escribiendo el motivo).
 */
fun motivoSugerido(
    obras: List<ObraEnRevision>,
    expertos: List<PerfilEspecialista>,
    seleccion: Map<String, String?>
): String? {
    val tecnicas = obras
        .filter { expertoValido(it, expertos, seleccion) == null }
        .map { it.tecnica.etiqueta }
        .distinct()
    if (tecnicas.isEmpty()) return null
    return "No hay Experto disponible para ${unirConY(tecnicas)}"
}

/** ["Textil"] → "Textil"; ["Textil", "Cerámica"] → "Textil y Cerámica"; tres → "A, B y C". */
fun unirConY(partes: List<String>): String = when (partes.size) {
    0 -> ""
    1 -> partes[0]
    else -> partes.dropLast(1).joinToString(", ") + " y " + partes.last()
}