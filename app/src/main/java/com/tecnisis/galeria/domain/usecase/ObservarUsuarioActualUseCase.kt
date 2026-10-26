package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObservarUsuarioActualUseCase(private val repo: AuthRepository) {
    operator fun invoke(): Flow<Usuario?> = repo.observarUsuarioActual()
}