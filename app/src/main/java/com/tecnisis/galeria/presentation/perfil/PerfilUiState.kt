package com.tecnisis.galeria.presentation.perfil

import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.domain.model.Usuario

data class PerfilUiState(
    val usuario: Usuario? = null,
    val modoTema: ModoTema = ModoTema.SISTEMA,
    val sesionCerrada: Boolean = false
)