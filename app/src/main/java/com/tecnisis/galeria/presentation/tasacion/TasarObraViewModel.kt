package com.tecnisis.galeria.presentation.tasacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.TasacionError
import com.tecnisis.galeria.domain.usecase.DescargarFotoObraUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerObraPorTasarUseCase
import com.tecnisis.galeria.domain.usecase.RegistrarTasacionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tasar una obra aceptada: PVP, porcentaje, cálculo en vivo, confirmación y guardado. */
class TasarObraViewModel(
    private val id: String,
    private val obtenerObraPorTasar: ObtenerObraPorTasarUseCase,
    private val descargarFotoObra: DescargarFotoObraUseCase,
    private val registrarTasacion: RegistrarTasacionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasarObraUiState())
    val uiState: StateFlow<TasarObraUiState> = _uiState.asStateFlow()

    init {
        // Se lee una vez del servidor: al tasarla, Ventas deja de poder leerla (la regla solo deja leer las aceptadas)
        viewModelScope.launch {
            obtenerObraPorTasar(id)
                .onSuccess { obra -> _uiState.update { it.copy(cargando = false, obra = obra) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, errorCarga = mensajeCarga(e)) } }
        }
    }

    suspend fun descargarFoto(ruta: String): ByteArray? =
        descargarFotoObra(ruta).getOrNull()

    /** Máximo 13 caracteres ("1000000,00" ya entra con espacio de sobra). */
    fun onPvp(texto: String) {
        if (texto.length <= 13) _uiState.update { it.copy(pvp = texto) }
    }

    fun onPorcentaje(texto: String) {
        if (texto.length <= 3) _uiState.update { it.copy(porcentaje = texto) }
    }

    /** Abre el diálogo solo si los dos campos son válidos. */
    fun onGuardar() {
        if (!_uiState.value.puedeGuardar) return
        _uiState.update { it.copy(confirmando = true, errorDialogo = null, yaTasada = false) }
    }

    /** Mientras se guarda, el diálogo no se cierra. */
    fun onCancelar() {
        if (_uiState.value.guardando) return
        _uiState.update { it.copy(confirmando = false, errorDialogo = null) }
    }

    fun onConfirmar() {
        val estado = _uiState.value
        val obra = estado.obra ?: return
        if (estado.guardando || estado.yaTasada) return
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorDialogo = null) }
            registrarTasacion(id, estado.pvp, estado.porcentaje)
                .onSuccess { _uiState.update { it.copy(confirmando = false, tituloGuardada = obra.titulo) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorDialogo = mensajeGuardar(e), yaTasada = e == TasacionError.YaTasada)
                    }
                }
            _uiState.update { it.copy(guardando = false) }
        }
    }

    private fun mensajeGuardar(e: Throwable): String = when (e) {
        TasacionError.PvpInvalido, TasacionError.PorcentajeInvalido -> "Revisa el precio y el porcentaje"
        TasacionError.SinConexion -> "Necesitas internet para guardar la tasación. No se guardó nada."
        TasacionError.SinPermiso -> "Tu sesión venció o tu cuenta no es de Especialista de Ventas"
        TasacionError.YaTasada -> "Esta obra ya fue tasada. Vuelve a Por tasar."
        TasacionError.NoEncontrada -> "La obra ya no existe"
        TasacionError.PdfNoGenerado -> "No se pudo generar el documento de venta. No se guardó nada."
        TasacionError.FaltanDocumentos -> "No se pudo subir el documento de venta. No se guardó nada; inténtalo de nuevo."
        is TasacionError.Servidor -> "No se pudo guardar la tasación (${e.detalle})"
        else -> "No se pudo guardar la tasación. Inténtalo de nuevo"
    }

    private fun mensajeCarga(e: Throwable): String = when (e) {
        TasacionError.SinConexion -> "Sin conexión a internet. Vuelve a abrir la obra cuando tengas señal"
        TasacionError.YaTasada -> "Esta obra ya no está por tasar"
        TasacionError.NoEncontrada -> "La obra ya no existe"
        else -> "No se pudo cargar la obra. Inténtalo de nuevo"
    }

    companion object {
        const val ARG_ID = "obraId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                val id = checkNotNull(createSavedStateHandle().get<String>(ARG_ID)) {
                    "Falta el id de la obra en la ruta"
                }
                TasarObraViewModel(id, container.obtenerObraPorTasar, container.descargarFotoObra, container.registrarTasacion)
            }
        }
    }
}