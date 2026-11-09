package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.ObraAsignada
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.TipoVeredicto
import kotlinx.coroutines.flow.Flow

/** HU08: lo que necesita el Especialista de Arte para evaluar las obras que le asignaron. */
interface EvaluacionRepository {
    /** En tiempo real: todas las obras con expertoUid == uid de la sesión (por evaluar y evaluadas). */
    fun observarAsignadas(): Flow<List<ObraAsignada>>

    /** Una obra asignada, en tiempo real. Emite null si no existe. */
    fun observarObra(id: String): Flow<ObraAsignada?>

    /** La ficha del Especialista de la sesión (expertos/{uid}): nombre, técnicas y carga. Emite null si no existe. */
    fun observarMiFicha(): Flow<PerfilEspecialista?>

    /**
     * Llama a la Cloud Function registrarVeredicto, que cambia la obra, baja la carga del Especialista
     * y avisa al socio. El comentario ya llega validado y sin espacios en los extremos.
     */
    suspend fun registrarVeredicto(obraId: String, tipo: TipoVeredicto, comentario: String): Result<Unit>
}