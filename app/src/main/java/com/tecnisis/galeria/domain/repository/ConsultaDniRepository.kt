package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.PersonaReniec

interface ConsultaDniRepository {
    /** Devuelve la persona, o null si RENIEC no tiene ese DNI. Lanza IOException si falla la red. */
    suspend fun consultar(dni: String): PersonaReniec?
}