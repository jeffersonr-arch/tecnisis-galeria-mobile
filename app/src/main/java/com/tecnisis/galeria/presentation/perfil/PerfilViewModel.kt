package com.tecnisis.galeria.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.SocioError
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.usecase.CerrarSesionUseCase
import com.tecnisis.galeria.domain.usecase.GuardarModoTemaUseCase
import com.tecnisis.galeria.domain.usecase.ObservarConexionUseCase
import com.tecnisis.galeria.domain.usecase.ObservarMiPerfilUseCase
import com.tecnisis.galeria.domain.usecase.ObservarModoTemaUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerUsuarioActualUseCase
import com.tecnisis.galeria.domain.usecase.SincronizarPerfilUseCase
import com.tecnisis.galeria.domain.model.ModoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que maneja el propio ViewModel (lo demás llega de DataStore, Room y la red) */
private data class EstadoLocal(
    val usuario: Usuario? = null,
    val sesionCerrada: Boolean = false,
    val sincronizando: Boolean = false,
    val mensaje: String? = null
)

class PerfilViewModel(
    obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
    observarModoTema: ObservarModoTemaUseCase,
    private val guardarModoTema: GuardarModoTemaUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    observarMiPerfil: ObservarMiPerfilUseCase,
    private val sincronizarPerfil: SincronizarPerfilUseCase,
    observarConexion: ObservarConexionUseCase
) : ViewModel() {

    private val local = MutableStateFlow(EstadoLocal())

    // Cuatro fuentes, un solo estado: lo local, el tema (DataStore), la ficha (Room) y la red
    val uiState: StateFlow<PerfilUiState> = combine(
        local,
        observarModoTema(),
        observarMiPerfil(),
        observarConexion()
    ) { l, modo, socio, conexion ->
        PerfilUiState(
            usuario = l.usuario,
            modoTema = modo,
            sesionCerrada = l.sesionCerrada,
            socio = socio,
            hayConexion = conexion,
            sincronizando = l.sincronizando,
            mensaje = l.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState())

    init {
        viewModelScope.launch {
            obtenerUsuarioActual().onSuccess { u -> local.update { it.copy(usuario = u) } }
        }
    }

    fun onModoTemaChange(modo: ModoTema) {
        viewModelScope.launch { guardarModoTema(modo) }
    }

    /** HU05: deslizar hacia abajo trae la ficha de Firestore y la guarda en Room */
    fun onActualizar() {
        if (local.value.sincronizando) return
        viewModelScope.launch {
            local.update { it.copy(sincronizando = true, mensaje = null) }
            val error = sincronizarPerfil().exceptionOrNull()
            local.update {
                it.copy(sincronizando = false, mensaje = error?.let { e -> mensajeDe(e) } ?: "Perfil actualizado")
            }
        }
    }

    fun onMensajeMostrado() = local.update { it.copy(mensaje = null) }

    fun onCerrarSesion() {
        cerrarSesion() // HU05: también borra la ficha guardada en el celular
        local.update { it.copy(sesionCerrada = true) }
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        SocioError.SinConexion -> "Sin conexión: se muestran los datos guardados"
        SocioError.SinFicha -> "Tu ficha de socio aún no está disponible"
        SocioError.SinSesion -> "Tu sesión expiró. Vuelve a ingresar"
        else -> "No se pudo actualizar. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                PerfilViewModel(
                    container.obtenerUsuarioActual,
                    container.observarModoTema,
                    container.guardarModoTema,
                    container.cerrarSesion,
                    container.observarMiPerfil,
                    container.sincronizarPerfil,
                    container.observarConexion
                )
            }
        }
    }
}