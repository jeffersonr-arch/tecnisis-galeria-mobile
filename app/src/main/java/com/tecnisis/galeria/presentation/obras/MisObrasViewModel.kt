package com.tecnisis.galeria.presentation.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.usecase.EnviarSolicitudUseCase
import com.tecnisis.galeria.domain.usecase.ObservarConexionUseCase
import com.tecnisis.galeria.domain.usecase.ObservarMiSolicitudUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que maneja el propio ViewModel (lo demás llega de Room y de la red). */
private data class EstadoLocal(
    val confirmandoEnvio: Boolean = false,
    val enviando: Boolean = false,
    val mensaje: String? = null
)

class MisObrasViewModel(
    observarMiSolicitud: ObservarMiSolicitudUseCase,
    observarConexion: ObservarConexionUseCase,
    private val enviarSolicitud: EnviarSolicitudUseCase
) : ViewModel() {

    private val local = MutableStateFlow(EstadoLocal())

    // Room avisa cada vez que cambia una obra o el estado de la solicitud (por ejemplo, cuando el worker la envía)
    val uiState: StateFlow<MisObrasUiState> = combine(
        local,
        observarMiSolicitud(),
        observarConexion()
    ) { l, solicitud, conexion ->
        MisObrasUiState(
            cargando = false,
            solicitud = solicitud,
            hayConexion = conexion,
            confirmandoEnvio = l.confirmandoEnvio,
            enviando = l.enviando,
            mensaje = l.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MisObrasUiState())

    fun onEnviar() = local.update { it.copy(confirmandoEnvio = true) }

    fun onCancelarEnvio() = local.update { it.copy(confirmandoEnvio = false) }

    fun onConfirmarEnvio() {
        if (local.value.enviando) return
        viewModelScope.launch {
            local.update { it.copy(confirmandoEnvio = false, enviando = true) }
            val error = enviarSolicitud().exceptionOrNull()
            local.update { it.copy(enviando = false, mensaje = error?.let { e -> mensajeDe(e) }) }
        }
    }

    fun onMensajeMostrado() = local.update { it.copy(mensaje = null) }

    private fun mensajeDe(error: Throwable): String = when (error) {
        ObraError.SinObras -> "Agrega al menos una obra antes de enviar"
        ObraError.SolicitudNoEditable -> "Tu solicitud ya se envió"
        ObraError.SinSesion -> "Tu sesión expiró. Vuelve a ingresar"
        else -> "No se pudo enviar. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                MisObrasViewModel(container.observarMiSolicitud, container.observarConexion, container.enviarSolicitud)
            }
        }
    }
}