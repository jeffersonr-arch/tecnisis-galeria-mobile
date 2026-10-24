package com.tecnisis.galeria.presentation.bandeja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.usecase.ObservarPostulacionesPorEstadoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BandejaViewModel(observarPorEstado: ObservarPostulacionesPorEstadoUseCase) : ViewModel() {

    private val pestana = MutableStateFlow(PestanaBandeja.OBSERVADAS)

    val uiState: StateFlow<BandejaUiState> = combine(
        observarPorEstado(EstadoPostulacion.OBSERVADA),
        observarPorEstado(EstadoPostulacion.VERIFICADA),
        pestana
    ) { observadas, verificadas, pestanaActual ->
        BandejaUiState(
            cargando = false,
            pestana = pestanaActual,
            observadas = observadas,
            verificadas = verificadas
        )
    }
        .catch {
            emit(
                BandejaUiState(
                    cargando = false,
                    mensajeError = "No se pudieron cargar las postulaciones. Revisa tu conexión"
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BandejaUiState())

    fun onPestanaChange(nueva: PestanaBandeja) {
        pestana.value = nueva
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                BandejaViewModel(container.observarPostulacionesPorEstado)
            }
        }
    }
}