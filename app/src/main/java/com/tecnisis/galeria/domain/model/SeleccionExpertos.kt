package com.tecnisis.galeria.domain.model

/** HU07: una fila del selector "Cambiar Experto" de una obra. */
data class OpcionExperto(
    val experto: PerfilEspecialista,
    val afinidad: Int,
    val recomendado: Boolean,
    val disponible: Boolean
)

/**
 * Selección inicial (obraId → uid del Experto o null): el asignado si la solicitud ya se aprobó;
 * si no, lo que recomendó la función.
 */
fun seleccionInicial(obras: List<ObraEnRevision>): Map<String, String?> =
    obras.associate { it.id to (it.expertoAsignadoUid ?: it.recomendadoUid) }

/** Cuántas OTRAS obras de esta solicitud tienen elegido a cada Experto. */
fun asignacionesExtra(seleccion: Map<String, String?>, exceptoObra: String): Map<String, Int> =
    seleccion.filterKeys { it != exceptoObra }.values.filterNotNull().groupingBy { it }.eachCount()

/**
 * Opciones de una obra: solo Expertos que dominan su técnica (filtro duro).
 * Orden: primero los disponibles; entre ellos, mayor afinidad; a igual afinidad, menor carga.
 */
fun opcionesPara(
    obra: ObraEnRevision,
    expertos: List<PerfilEspecialista>,
    seleccion: Map<String, String?>
): List<OpcionExperto> {
    val extra = asignacionesExtra(seleccion, obra.id)
    val afinidades = obra.candidatos.associate { it.uid to it.afinidad }
    return expertos
        .filter { it.domina(obra.tecnica) }
        .map { e ->
            OpcionExperto(
                experto = e,
                afinidad = afinidades[e.uid] ?: 0,
                recomendado = e.uid == obra.recomendadoUid,
                disponible = e.disponibleCon(extra[e.uid] ?: 0)
            )
        }
        .sortedWith(
            compareByDescending<OpcionExperto> { it.disponible }
                .thenByDescending { it.afinidad }
                .thenBy { it.experto.carga }
        )
}

/** El Experto elegido para la obra si todavía vale (domina la técnica y está disponible); si no, null. */
fun expertoValido(
    obra: ObraEnRevision,
    expertos: List<PerfilEspecialista>,
    seleccion: Map<String, String?>
): PerfilEspecialista? {
    val uid = seleccion[obra.id] ?: return null
    return opcionesPara(obra, expertos, seleccion)
        .firstOrNull { it.experto.uid == uid && it.disponible }
        ?.experto
}

/** Se puede aprobar solo si TODAS las obras tienen un Experto válido. */
fun todasConExperto(
    obras: List<ObraEnRevision>,
    expertos: List<PerfilEspecialista>,
    seleccion: Map<String, String?>
): Boolean = obras.isNotEmpty() && obras.all { expertoValido(it, expertos, seleccion) != null }