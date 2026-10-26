package com.tecnisis.galeria.presentation.estado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.usecase.ObservarMiPostulacionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class EstadoPostulacionViewModel(observarMiPostulacion: ObservarMiPostulacionUseCase) : ViewModel() {

    val estado: StateFlow<EstadoPostulacionUi> = observarMiPostulacion()
        .map<Postulacion?, EstadoPostulacionUi> { postulacion ->
            if (postulacion == null) EstadoPostulacionUi.SinPostulacion
            else EstadoPostulacionUi.ConPostulacion(postulacion)
        }
        .catch { emit(EstadoPostulacionUi.Error("No se pudo cargar tu postulación")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoPostulacionUi.Cargando)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                EstadoPostulacionViewModel(container.observarMiPostulacion)
            }
        }
    }
}