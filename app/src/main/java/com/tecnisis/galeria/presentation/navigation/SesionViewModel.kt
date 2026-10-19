package com.tecnisis.galeria.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.usecase.ObservarUsuarioActualUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Quién está usando la app ahora mismo; lo usa el NavHost para armar la barra. */
class SesionViewModel(observarUsuarioActual: ObservarUsuarioActualUseCase) : ViewModel() {

    val usuario: StateFlow<Usuario?> = observarUsuarioActual()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                SesionViewModel(container.observarUsuarioActual)
            }
        }
    }
}