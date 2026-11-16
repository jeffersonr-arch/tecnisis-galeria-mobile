package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.ObraProyectada
import kotlinx.coroutines.flow.Flow

/** HU12: los Documentos de Venta (documentos_venta) para la proyección del Gerente, en tiempo real. */
interface ProyeccionRepository {
    fun observarObrasTasadas(): Flow<List<ObraProyectada>>
}