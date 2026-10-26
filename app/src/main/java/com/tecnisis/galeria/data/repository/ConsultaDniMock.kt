package com.tecnisis.galeria.data.repository

import com.tecnisis.galeria.domain.model.PersonaReniec
import com.tecnisis.galeria.domain.repository.ConsultaDniRepository
import kotlinx.coroutines.delay

/** API simulada para pruebas: no gasta consultas reales. Se activa con DNI_API_MOCK=true. */
class ConsultaDniMock : ConsultaDniRepository {

    private val registros = mapOf(
        "12345678" to PersonaReniec("JUAN CARLOS", "PEREZ", "GOMEZ"),
        "87654321" to PersonaReniec("MARIA ELENA", "LOPEZ", "DIAZ"),
        "11223344" to PersonaReniec("ROSA", "QUISPE", "MAMANI"),
        "44332211" to PersonaReniec("LUIS ALBERTO", "TORRES", "VEGA"),
        "55667788" to PersonaReniec("ANA LUCIA", "RAMOS", "FLORES"),
        "88776655" to PersonaReniec("PEDRO", "CASTILLO", "HUAMAN")
    )

    override suspend fun consultar(dni: String): PersonaReniec? {
        delay(1_500) // simula la demora de la red
        return registros[dni]
    }
}