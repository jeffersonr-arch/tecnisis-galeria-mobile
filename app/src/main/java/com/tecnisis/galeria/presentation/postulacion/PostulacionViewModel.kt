package com.tecnisis.galeria.presentation.postulacion

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.model.PostulacionError
import com.tecnisis.galeria.domain.usecase.EnviarPostulacionUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerUsuarioActualUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostulacionViewModel(
    private val enviarPostulacion: EnviarPostulacionUseCase,
    obtenerUsuarioActual: ObtenerUsuarioActualUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostulacionUiState())
    val uiState: StateFlow<PostulacionUiState> = _uiState.asStateFlow()

    init {
        // Rellena el correo con el de la cuenta
        viewModelScope.launch {
            obtenerUsuarioActual().onSuccess { usuario ->
                _uiState.update { it.copy(correo = usuario.correo) }
            }
        }
    }

    fun onCambio(campo: Campo, valor: String) = _uiState.update { estado ->
        val actualizado = when (campo) {
            Campo.DNI -> estado.copy(dni = valor.filter { it.isDigit() }.take(8))
            Campo.NOMBRES -> estado.copy(nombres = valor)
            Campo.APELLIDO_PATERNO -> estado.copy(apellidoPaterno = valor)
            Campo.APELLIDO_MATERNO -> estado.copy(apellidoMaterno = valor)
            Campo.DIRECCION -> estado.copy(direccion = valor)
            Campo.SEXO -> estado.copy(sexo = valor)
            Campo.TELEFONO -> estado.copy(telefono = valor.filter { it.isDigit() }.take(9))
            Campo.CORREO -> estado.copy(correo = valor)
        }
        actualizado.copy(errores = actualizado.errores - campo, mensajeError = null)
    }

    fun onEnviarClick() {
        val s = _uiState.value
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeError = null) }
            val postulacion = Postulacion(
                uid = "", // el repositorio pone el uid de la cuenta
                dni = s.dni,
                nombres = s.nombres,
                apellidoPaterno = s.apellidoPaterno,
                apellidoMaterno = s.apellidoMaterno,
                direccion = s.direccion,
                sexo = s.sexo,
                telefono = s.telefono,
                correo = s.correo
            )
            enviarPostulacion(postulacion)
                .onSuccess { _uiState.update { it.copy(cargando = false, enviada = true) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, mensajeError = mensajeDe(e)) } }
        }
    }

    private fun validar(s: PostulacionUiState): Map<Campo, String> = buildMap {
        if (s.dni.length != 8) put(Campo.DNI, "El DNI debe tener 8 dígitos")
        if (s.nombres.isBlank()) put(Campo.NOMBRES, "Obligatorio")
        if (s.apellidoPaterno.isBlank()) put(Campo.APELLIDO_PATERNO, "Obligatorio")
        if (s.apellidoMaterno.isBlank()) put(Campo.APELLIDO_MATERNO, "Obligatorio")
        if (s.direccion.isBlank()) put(Campo.DIRECCION, "Obligatorio")
        if (s.sexo.isBlank()) put(Campo.SEXO, "Selecciona una opción")
        if (s.telefono.length != 9 || !s.telefono.startsWith("9")) put(Campo.TELEFONO, "9 dígitos, empieza con 9")
        if (!Patterns.EMAIL_ADDRESS.matcher(s.correo.trim()).matches()) put(Campo.CORREO, "Correo no válido")
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        PostulacionError.DniYaRegistrado -> "Este DNI ya está registrado en la galería"
        PostulacionError.YaPostulo -> "Ya enviaste una postulación. Revisa su estado en el inicio"
        PostulacionError.SinConexion -> "Sin conexión a internet. Inténtalo de nuevo"
        PostulacionError.SinPermiso -> "No tienes permiso para esta acción. Vuelve a iniciar sesión"
        else -> "No se pudo enviar la postulación. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                PostulacionViewModel(container.enviarPostulacion, container.obtenerUsuarioActual)
            }
        }
    }
}