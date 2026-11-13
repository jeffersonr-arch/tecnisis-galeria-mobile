package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.ExpertoDashboard
import com.tecnisis.galeria.domain.model.ObraEvaluada
import kotlinx.coroutines.flow.Flow

/** HU11: datos del dashboard de solicitudes del Gerente, en tiempo real. */
interface DashboardRepository {
    /** Obras con veredicto: estado aceptada, lista_exhibicion o rechazada. */
    fun observarObrasEvaluadas(): Flow<List<ObraEvaluada>>

    /** Todos los Expertos (colección expertos), activos e inactivos. */
    fun observarExpertos(): Flow<List<ExpertoDashboard>>
}