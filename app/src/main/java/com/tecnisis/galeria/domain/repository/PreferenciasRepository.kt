package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.ModoTema
import kotlinx.coroutines.flow.Flow

interface PreferenciasRepository {
    fun observarModoTema(): Flow<ModoTema>
    suspend fun guardarModoTema(modo: ModoTema)
}