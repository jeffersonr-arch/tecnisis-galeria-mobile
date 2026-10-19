package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.domain.repository.PreferenciasRepository

class GuardarModoTemaUseCase(private val repo: PreferenciasRepository) {
    suspend operator fun invoke(modo: ModoTema) = repo.guardarModoTema(modo)
}