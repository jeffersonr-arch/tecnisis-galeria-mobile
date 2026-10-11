package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.Socio
import kotlinx.coroutines.flow.Flow

interface SocioRepository {
    fun observarMiPerfil(): Flow<Socio?>
    suspend fun sincronizar(): Result<Unit>
    fun programarSincronizacion()
}