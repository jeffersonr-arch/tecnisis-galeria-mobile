package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.ObraRechazada
import kotlinx.coroutines.flow.Flow

/** HU09: histórico de obras rechazadas que consulta el Gerente. */
interface HistoricoRechazosRepository {
    /** En tiempo real: todas las obras con estado "rechazada" (sin orden; lo da el caso de uso). */
    fun observarRechazadas(): Flow<List<ObraRechazada>>
}