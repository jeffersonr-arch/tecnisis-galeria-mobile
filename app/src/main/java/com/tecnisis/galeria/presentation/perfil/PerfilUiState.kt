package com.tecnisis.galeria.presentation.perfil

import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.domain.model.Socio
import com.tecnisis.galeria.domain.model.Usuario

data class PerfilUiState(
    val usuario: Usuario? = null,
    val modoTema: ModoTema = ModoTema.SISTEMA,
    val sesionCerrada: Boolean = false,
    // HU05: ficha del Socio Activo leída de Room
    val socio: Socio? = null,
    val hayConexion: Boolean = true,
    val sincronizando: Boolean = false,
    val mensaje: String? = null
) {
    /** La sección "Mi membresía" y el deslizar para actualizar solo existen para el Socio Activo */
    val esSocio: Boolean
        get() = usuario?.rol == Rol.SOCIO_ACTIVO
}