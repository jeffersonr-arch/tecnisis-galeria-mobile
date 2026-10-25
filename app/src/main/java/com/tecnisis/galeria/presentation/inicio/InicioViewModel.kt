package com.tecnisis.galeria.presentation.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.AuthError
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.usecase.CerrarSesionUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerUsuarioActualUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoInicio {
    data object Cargando : EstadoInicio
    data class Autorizado(val usuario: Usuario) : EstadoInicio
    data object Denegado : EstadoInicio
    data object SinConexion : EstadoInicio // no se pudo leer el rol, pero la sesión sigue abierta
}

class InicioViewModel(
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
    private val cerrarSesion: CerrarSesionUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoInicio>(EstadoInicio.Cargando)
    val estado: StateFlow<EstadoInicio> = _estado.asStateFlow()

    fun verificarAcceso(rolDePantalla: Rol) {
        viewModelScope.launch {
            _estado.value = EstadoInicio.Cargando
            _estado.value = obtenerUsuarioActual().fold(
                onSuccess = { usuario ->
                    if (usuario.rol == rolDePantalla) EstadoInicio.Autorizado(usuario)
                    else EstadoInicio.Denegado
                },
                // Sin internet no se echa al usuario: se le deja reintentar
                onFailure = { error ->
                    if (error == AuthError.SinConexion) EstadoInicio.SinConexion
                    else EstadoInicio.Denegado
                }
            )
        }
    }

    fun onCerrarSesion() = cerrarSesion()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                InicioViewModel(container.obtenerUsuarioActual, container.cerrarSesion)
            }
        }
    }
}