package com.tecnisis.galeria.presentation.solicitudes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.RevisionError
import com.tecnisis.galeria.domain.usecase.DescargarFotoObraUseCase
import com.tecnisis.galeria.domain.usecase.ObservarSolicitudesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SolicitudesViewModel(
    observarSolicitudes: ObservarSolicitudesUseCase,
    private val descargarFotoObra: DescargarFotoObraUseCase
) : ViewModel() {

    private val pestana = MutableStateFlow(PestanaSolicitudes.POR_REVISAR)

    val uiState: StateFlow<SolicitudesUiState> = combine(
        observarSolicitudes(resueltas = false),
        observarSolicitudes(resueltas = true),
        pestana
    ) { porRevisar, resueltas, pestanaActual ->
        SolicitudesUiState(
            cargando = false,
            pestana = pestanaActual,
            porRevisar = porRevisar,
            resueltas = resueltas
        )
    }
        .catch { e -> emit(SolicitudesUiState(cargando = false, mensajeError = mensajeDe(e))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SolicitudesUiState())

    fun onPestanaChange(nueva: PestanaSolicitudes) {
        pestana.value = nueva
    }

    /** Para las miniaturas: null si no se pudo descargar (se ve el dibujo de pigmentos). */
    suspend fun descargarFoto(ruta: String): ByteArray? =
        descargarFotoObra(ruta).getOrNull()

    private fun mensajeDe(e: Throwable): String = when (e) {
        RevisionError.SinConexion -> "Sin conexión a internet. Vuelve a abrir la pestaña cuando tengas señal"
        RevisionError.SinPermiso -> "Solo el Anfitrión puede ver las solicitudes de exhibición"
        else -> "No se pudieron cargar las solicitudes. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                SolicitudesViewModel(container.observarSolicitudes, container.descargarFotoObra)
            }
        }
    }
}