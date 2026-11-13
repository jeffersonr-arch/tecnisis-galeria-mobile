package com.tecnisis.galeria.presentation.tasacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.TasacionError
import com.tecnisis.galeria.domain.usecase.DescargarFotoObraUseCase
import com.tecnisis.galeria.domain.usecase.ObservarObrasPorTasarUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Las obras aceptadas que faltan tasar, en tiempo real. */
class PorTasarViewModel(
    observarObrasPorTasar: ObservarObrasPorTasarUseCase,
    private val descargarFotoObra: DescargarFotoObraUseCase
) : ViewModel() {

    val uiState: StateFlow<PorTasarUiState> = observarObrasPorTasar()
        .map { obras -> PorTasarUiState(cargando = false, obras = obras) }
        .catch { e -> emit(PorTasarUiState(cargando = false, mensajeError = mensajeDe(e))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PorTasarUiState())

    /** Para las miniaturas: null si no se pudo descargar (se ve el dibujo de pigmentos). */
    suspend fun descargarFoto(ruta: String): ByteArray? =
        descargarFotoObra(ruta).getOrNull()

    private fun mensajeDe(e: Throwable): String = when (e) {
        TasacionError.SinConexion -> "Sin conexión a internet. Vuelve a abrir la pestaña cuando tengas señal"
        TasacionError.SinPermiso -> "Solo el Especialista de Ventas puede ver las obras por tasar"
        else -> "No se pudieron cargar las obras. Inténtalo de nuevo"
    }

    companion object {
        /** Clave con la que Tasar obra le deja a esta pantalla el aviso "Tasación guardada…". */
        const val ARG_AVISO = "avisoTasacion"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                PorTasarViewModel(container.observarObrasPorTasar, container.descargarFotoObra)
            }
        }
    }
}