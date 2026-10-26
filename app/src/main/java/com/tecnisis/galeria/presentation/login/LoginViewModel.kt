package com.tecnisis.galeria.presentation.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.AuthError
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.usecase.HaySesionActivaUseCase
import com.tecnisis.galeria.domain.usecase.IniciarSesionUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerUsuarioActualUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
    private val haySesionActiva: HaySesionActivaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    private var ingresoAutomaticoIntentado = false
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun configurarHuella(biometriaDisponible: Boolean) {
        val haySesion = haySesionActiva()
        _uiState.update { it.copy(puedeUsarHuella = biometriaDisponible && haySesion) }
        if (!haySesion || ingresoAutomaticoIntentado) return
        ingresoAutomaticoIntentado = true
        if (biometriaDisponible) {
            _uiState.update { it.copy(pedirHuella = true) }
        } else {
            ejecutar { obtenerUsuarioActual() }
        }
    }

    fun onHuellaPedida() = _uiState.update { it.copy(pedirHuella = false) }

    fun onCorreoChange(valor: String) =
        _uiState.update { it.copy(correo = valor, errorCorreo = null, mensajeError = null) }

    fun onContrasenaChange(valor: String) =
        _uiState.update { it.copy(contrasena = valor, errorContrasena = null, mensajeError = null) }

    fun onIngresarClick() {
        val estado = _uiState.value
        val errorCorreo = when {
            estado.correo.isBlank() -> "Ingresa tu correo"
            !Patterns.EMAIL_ADDRESS.matcher(estado.correo.trim()).matches() -> "Formato de correo no válido"
            else -> null
        }
        val errorContrasena = when {
            estado.contrasena.isBlank() -> "Ingresa tu contraseña"
            estado.contrasena.length < 6 -> "Mínimo 6 caracteres"
            else -> null
        }
        if (errorCorreo != null || errorContrasena != null) {
            _uiState.update { it.copy(errorCorreo = errorCorreo, errorContrasena = errorContrasena) }
            return
        }
        ejecutar { iniciarSesion(estado.correo, estado.contrasena) }
    }

    fun onHuellaExitosa() = ejecutar { obtenerUsuarioActual() }

    fun onHuellaError(mensaje: String) = _uiState.update { it.copy(mensajeError = mensaje) }

    private fun ejecutar(accion: suspend () -> Result<Usuario>) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeError = null) }
            accion()
                .onSuccess { usuario ->
                    _uiState.update { it.copy(cargando = false, usuarioAutenticado = usuario) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = mensajeDe(error),
                            puedeUsarHuella = it.puedeUsarHuella && haySesionActiva()
                        )
                    }
                }
        }
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        AuthError.CredencialesInvalidas -> "Correo o contraseña incorrectos"
        AuthError.UsuarioDeshabilitado -> "Esta cuenta está deshabilitada. Contacta a la galería"
        AuthError.SinConexion -> "Sin conexión a internet. Revisa tu red e inténtalo de nuevo"
        AuthError.DemasiadosIntentos -> "Demasiados intentos fallidos. Espera unos minutos"
        AuthError.SinRol -> "Tu cuenta no tiene un rol asignado. Contacta al Gerente"
        AuthError.SinSesion -> "Tu sesión expiró. Ingresa con tu correo y contraseña"
        else -> "Ocurrió un error inesperado. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                LoginViewModel(
                    container.iniciarSesion,
                    container.obtenerUsuarioActual,
                    container.haySesionActiva
                )
            }
        }
    }
}