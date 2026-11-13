package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.MontosTasacion
import com.tecnisis.galeria.domain.model.ObraPorTasar
import com.tecnisis.galeria.domain.model.ObraTasada
import kotlinx.coroutines.flow.Flow

/** HU10: lo que necesita el Especialista de Ventas para tasar las obras aceptadas. */
interface TasacionRepository {
    /** En tiempo real: las obras con estado "aceptada" (sin orden; lo da el caso de uso). */
    fun observarPorTasar(): Flow<List<ObraPorTasar>>

    /** En tiempo real: los documentos de venta ya guardados (sin orden; lo da el caso de uso). */
    fun observarTasadas(): Flow<List<ObraTasada>>

    /** Una obra por tasar, leída del servidor. Si ya se tasó falla con TasacionError.YaTasada. */
    suspend fun obtenerPorTasar(id: String): Result<ObraPorTasar>

    /**
     * Genera y sube el documento de venta por duplicado y después llama a la Cloud Function registrarTasacion,
     * que recalcula los montos, crea documentos_venta/{obraId} y pasa la obra a "lista_exhibicion".
     * Si falla la subida, no se guarda nada.
     */
    suspend fun registrarTasacion(obraId: String, montos: MontosTasacion): Result<Unit>
}