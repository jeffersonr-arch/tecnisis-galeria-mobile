package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.repository.ConectividadRepository
import kotlinx.coroutines.flow.Flow

class ObservarConexionUseCase(private val repo: ConectividadRepository) {
    operator fun invoke(): Flow<Boolean> = repo.observarConexion()
}