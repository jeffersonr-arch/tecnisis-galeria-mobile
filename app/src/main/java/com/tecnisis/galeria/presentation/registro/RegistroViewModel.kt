package com.tecnisis.galeria.presentation.registro

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.AuthError
import com.tecnisis.galeria.domain.usecase.RegistrarCuentaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistroViewModel(private val registrarCuenta: RegistrarCuentaUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun onNombreChange(valor: String) = _uiState.update { it.copy(nombre = valor, errorNombre = null) }
    fun onCorreoChange(valor: String) = _uiState.update { it.copy(correo = valor, errorCorreo = null) }
    fun onContrasenaChange(valor: String) = _uiState.update { it.copy(contrasena = valor, errorContrasena = null) }
    fun onConfirmacionChange(valor: String) =
        _uiState.update { it.copy(confirmacion = valor, errorConfirmacion = null) }

    fun onRegistrarClick() {
        val s = _uiState.value
        val errorNombre = if (s.nombre.isBlank()) "Ingresa tu nombre" else null
        val errorCorreo =
            if (!Patterns.EMAIL_ADDRESS.matcher(s.correo.trim()).matches()) "Correo no válido" else null
        val errorContrasena = if (s.contrasena.length < 6) "Mínimo 6 caracteres" else null
        val errorConfirmacion = if (s.confirmacion != s.contrasena) "Las contraseñas no coinciden" else null

        if (listOf(errorNombre, errorCorreo, errorContrasena, errorConfirmacion).any { it != null }) {
            _uiState.update {
                it.copy(
                    errorNombre = errorNombre,
                    errorCorreo = errorCorreo,
                    errorContrasena = errorContrasena,
                    errorConfirmacion = errorConfirmacion
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeError = null) }
            registrarCuenta(s.nombre, s.correo, s.contrasena)
                .onSuccess { _uiState.update { it.copy(cargando = false, registrado = true) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, mensajeError = mensajeDe(e)) } }
        }
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        AuthError.CorreoEnUso -> "Ya existe una cuenta con ese correo"
        AuthError.ContrasenaDebil -> "La contraseña es muy débil"
        AuthError.SinConexion -> "Sin conexión a internet"
        else -> "No se pudo crear la cuenta. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                RegistroViewModel(container.registrarCuenta)
            }
        }
    }
}