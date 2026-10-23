package com.tecnisis.galeria.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.DecisionPostulacion
import com.tecnisis.galeria.domain.model.PostulacionError
import com.tecnisis.galeria.domain.usecase.ObtenerPostulacionUseCase
import com.tecnisis.galeria.domain.usecase.ResolverPostulacionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetallePostulacionViewModel(
    private val uid: String,
    private val obtenerPostulacion: ObtenerPostulacionUseCase,
    private val resolverPostulacion: ResolverPostulacionUseCase
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

    fun onAbrirDecision(decision: DecisionPostulacion) =
        _uiState.update { it.copy(decisionAbierta = decision, mensaje = "", errorMensaje = null) }

    fun onMensajeChange(valor: String) =
        _uiState.update { it.copy(mensaje = valor, errorMensaje = null) }

    fun onCancelarDecision() =
        _uiState.update { it.copy(decisionAbierta = null, mensaje = "", errorMensaje = null) }

    fun onConfirmarDecision() {
        val estado = _uiState.value
        val postulacion = estado.postulacion ?: return
        val decision = estado.decisionAbierta ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true, errorMensaje = null) }
            resolverPostulacion(postulacion, decision, estado.mensaje)
                .onSuccess {
                    if (decision == DecisionPostulacion.PEDIR_INFORMACION) {
                        _uiState.update {
                            it.copy(enviando = false, decisionAbierta = null, mensaje = "", aviso = "Mensaje enviado al postulante")
                        }
                        cargar()
                    } else {
                        _uiState.update {
                            it.copy(
                                enviando = false,
                                decisionAbierta = null,
                                resuelta = true,
                                aviso = if (decision == DecisionPostulacion.APROBAR) "Membresía aprobada"
                                else "Postulación rechazada"
                            )
                        }
                    }
                }
                .onFailure { e ->
                    if (e == PostulacionError.MensajeObligatorio) {
                        _uiState.update { it.copy(enviando = false, errorMensaje = "Este campo es obligatorio") }
                    } else {
                        _uiState.update { it.copy(enviando = false, decisionAbierta = null, aviso = mensajeDe(e)) }
                    }
                }
        }
    }

    fun onAvisoMostrado() = _uiState.update { it.copy(aviso = null) }

    private fun mensajeDe(error: Throwable): String = when (error) {
        PostulacionError.NoEncontrada -> "La postulación ya no existe"
        PostulacionError.SinConexion -> "Sin conexión a internet. Inténtalo de nuevo"
        PostulacionError.SinPermiso -> "Solo el Anfitrión puede ver y resolver postulaciones"
        PostulacionError.DecisionNoPermitida -> "Esta postulación ya fue resuelta o no admite esa decisión"
        is PostulacionError.Servidor -> "Error del servidor: ${error.detalle}"
        else -> "No se pudo completar la acción. Inténtalo de nuevo"
    }

    companion object {
        const val ARG_UID = "uid"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                val uid = checkNotNull(createSavedStateHandle().get<String>(ARG_UID)) {
                    "Falta el uid de la postulación en la ruta"
                }
                DetallePostulacionViewModel(uid, container.obtenerPostulacion, container.resolverPostulacion)
            }
        }
    }
}