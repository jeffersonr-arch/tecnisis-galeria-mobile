package com.tecnisis.galeria.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.usecase.CerrarSesionUseCase
import com.tecnisis.galeria.domain.usecase.GuardarModoTemaUseCase
import com.tecnisis.galeria.domain.usecase.ObservarModoTemaUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerUsuarioActualUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PerfilViewModel(
    obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
    observarModoTema: ObservarModoTemaUseCase,
    private val guardarModoTema: GuardarModoTemaUseCase,
    private val cerrarSesion: CerrarSesionUseCase
) : ViewModel() {

    private val usuario = MutableStateFlow<Usuario?>(null)
    private val sesionCerrada = MutableStateFlow(false)

    val uiState: StateFlow<PerfilUiState> =
        combine(usuario, observarModoTema(), sesionCerrada) { u, modo, cerrada ->
            PerfilUiState(usuario = u, modoTema = modo, sesionCerrada = cerrada)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState())

    init {
        viewModelScope.launch {
            obtenerUsuarioActual().onSuccess { usuario.value = it }
        }
    }

    fun onModoTemaChange(modo: ModoTema) {
        viewModelScope.launch { guardarModoTema(modo) }
    }

    fun onCerrarSesion() {
        cerrarSesion()
        sesionCerrada.value = true
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                PerfilViewModel(
                    container.obtenerUsuarioActual,
                    container.observarModoTema,
                    container.guardarModoTema,
                    container.cerrarSesion
                )
            }
        }
    }
}