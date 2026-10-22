package com.tecnisis.galeria.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.PostulacionError
import com.tecnisis.galeria.domain.usecase.ObtenerPostulacionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetallePostulacionViewModel(
    private val uid: String,
    private val obtenerPostulacion: ObtenerPostulacionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetallePostulacionUiState())
    val uiState: StateFlow<DetallePostulacionUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.postulacion == null, errorCarga = null) }
            obtenerPostulacion(uid)
                .onSuccess { p -> _uiState.update { it.copy(cargando = false, postulacion = p) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, errorCarga = mensajeDe(e)) } }
        }
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        PostulacionError.NoEncontrada -> "La postulación ya no existe"
        PostulacionError.SinConexion -> "Sin conexión a internet. Inténtalo de nuevo"
        PostulacionError.SinPermiso -> "No tienes permiso para ver esta postulación"
        else -> "No se pudo cargar la postulación"
    }

    companion object {
        const val ARG_UID = "uid"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                val uid = checkNotNull(createSavedStateHandle().get<String>(ARG_UID)) {
                    "Falta el uid de la postulación en la ruta"
                }
                DetallePostulacionViewModel(uid, container.obtenerPostulacion)
            }
        }
    }
}