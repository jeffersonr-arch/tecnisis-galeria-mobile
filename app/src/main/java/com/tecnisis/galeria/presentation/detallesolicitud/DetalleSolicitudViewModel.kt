package com.tecnisis.galeria.presentation.detallesolicitud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.EstadoAnalisis
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.RevisionError
import com.tecnisis.galeria.domain.model.seleccionInicial
import com.tecnisis.galeria.domain.usecase.AnalizarSolicitudUseCase
import com.tecnisis.galeria.domain.usecase.DescargarFotoObraUseCase
import com.tecnisis.galeria.domain.usecase.ObservarExpertosUseCase
import com.tecnisis.galeria.domain.usecase.ObservarSolicitudEnRevisionUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetalleSolicitudViewModel(
    private val id: String,
    private val observarSolicitud: ObservarSolicitudEnRevisionUseCase,
    private val observarExpertos: ObservarExpertosUseCase,
    private val analizarSolicitud: AnalizarSolicitudUseCase,
    private val descargarFotoObra: DescargarFotoObraUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleSolicitudUiState())
    val uiState: StateFlow<DetalleSolicitudUiState> = _uiState.asStateFlow()

    /** El análisis automático se pide una sola vez al abrir; después, solo con "Reintentar". */
    private var yaPidioAnalisis = false

    init {
        escuchar()
        // Reloj para que "Reintentar" aparezca solo si el análisis se queda "en curso" más de 3 minutos
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                _uiState.update { it.copy(ahora = System.currentTimeMillis()) }
            }
        }
    }

    private fun escuchar() {
        viewModelScope.launch {
            combine(observarSolicitud(id), observarExpertos()) { solicitud, expertos -> solicitud to expertos }
                .catch { e -> _uiState.update { it.copy(cargando = false, errorCarga = mensajeDe(e)) } }
                .collect { (solicitud, expertos) ->
                    _uiState.update { estado ->
                        val obras = solicitud?.obras.orEmpty()
                        // Sin cambios del Anfitrión, la selección sigue a la recomendación (se llena al terminar el análisis)
                        val seleccion = if (estado.hayCambios) {
                            seleccionInicial(obras) + estado.seleccion.filterKeys { k -> obras.any { it.id == k } }
                        } else {
                            seleccionInicial(obras)
                        }
                        estado.copy(
                            cargando = false,
                            solicitud = solicitud,
                            expertos = expertos,
                            seleccion = seleccion,
                            errorCarga = if (solicitud == null) "La solicitud ya no existe" else null
                        )
                    }
                    if (!yaPidioAnalisis && solicitud != null &&
                        solicitud.estado == EstadoSolicitud.ENVIADA &&
                        solicitud.analisis == EstadoAnalisis.PENDIENTE
                    ) {
                        yaPidioAnalisis = true
                        analizar()
                    }
                }
        }
    }

    /** Llama a la función con Retrofit. El resultado llega solo por Firestore (la pantalla escucha en tiempo real). */
    fun analizar() {
        if (_uiState.value.llamandoAnalisis) return
        yaPidioAnalisis = true
        viewModelScope.launch {
            _uiState.update { it.copy(llamandoAnalisis = true) }
            analizarSolicitud(id).onFailure { e -> _uiState.update { it.copy(aviso = mensajeDe(e)) } }
            _uiState.update { it.copy(llamandoAnalisis = false, ahora = System.currentTimeMillis()) }
        }
    }

    fun onCambiar(obraId: String) = _uiState.update { it.copy(obraCambiando = obraId) }

    fun onCerrarHoja() = _uiState.update { it.copy(obraCambiando = null) }

    fun onElegirExperto(obraId: String, uid: String) = _uiState.update { estado ->
        val nueva = estado.seleccion + (obraId to uid)
        estado.copy(
            seleccion = nueva,
            hayCambios = nueva != seleccionInicial(estado.obras),
            obraCambiando = null
        )
    }

    /** Volver (flecha o botón Atrás): si cambió algún Experto, primero pregunta. Devuelve true si puede salir ya. */
    fun onVolverPedido(): Boolean {
        if (!_uiState.value.hayCambios) return true
        _uiState.update { it.copy(confirmarSalida = true) }
        return false
    }

    fun onCancelarSalida() = _uiState.update { it.copy(confirmarSalida = false) }

    fun onAvisoMostrado() = _uiState.update { it.copy(aviso = null) }

    suspend fun descargarFoto(ruta: String): ByteArray? =
        descargarFotoObra(ruta).getOrNull()

    private fun mensajeDe(e: Throwable): String = when (e) {
        RevisionError.SinConexion -> "Sin conexión o el análisis tardó demasiado. Si sigue \"Analizando\", espera"
        RevisionError.SinPermiso -> "Tu sesión venció o tu cuenta no es de Anfitrión"
        RevisionError.NoEncontrada -> "La solicitud ya no existe"
        RevisionError.YaResuelta -> "Esta solicitud ya fue resuelta"
        is RevisionError.Servidor -> "El análisis falló (${e.detalle}). Toca Reintentar"
        else -> "No se pudo completar la acción. Inténtalo de nuevo"
    }

    companion object {
        const val ARG_ID = "solicitudId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                val id = checkNotNull(createSavedStateHandle().get<String>(ARG_ID)) {
                    "Falta el id de la solicitud en la ruta"
                }
                DetalleSolicitudViewModel(
                    id,
                    container.observarSolicitudEnRevision,
                    container.observarExpertos,
                    container.analizarSolicitud,
                    container.descargarFotoObra
                )
            }
        }
    }
}