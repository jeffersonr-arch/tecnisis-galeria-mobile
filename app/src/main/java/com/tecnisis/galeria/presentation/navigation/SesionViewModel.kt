package com.tecnisis.galeria.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.usecase.ObservarUsuarioActualUseCase
import com.tecnisis.galeria.domain.usecase.ProgramarSincronizacionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Quién está usando la app ahora mismo; lo usa el NavHost para armar la barra. */
class SesionViewModel(
    observarUsuarioActual: ObservarUsuarioActualUseCase,
    programarSincronizacion: ProgramarSincronizacionUseCase
) : ViewModel() {

    val usuario: StateFlow<Usuario?> = observarUsuarioActual()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // HU05: cuando el usuario es Socio Activo (al abrir la app o justo al ser aprobado),
        // WorkManager descarga su ficha a Room apenas haya internet
        usuario
            .map { it?.rol }
            .distinctUntilChanged()
            .filter { it == Rol.SOCIO_ACTIVO }
            .onEach { programarSincronizacion() }
            .launchIn(viewModelScope)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                SesionViewModel(container.observarUsuarioActual, container.programarSincronizacion)
            }
        }
    }
}