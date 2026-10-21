package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.domain.repository.PreferenciasRepository
import kotlinx.coroutines.flow.Flow

class ObservarModoTemaUseCase(private val repo: PreferenciasRepository) {
    operator fun invoke(): Flow<ModoTema> = repo.observarModoTema()
}