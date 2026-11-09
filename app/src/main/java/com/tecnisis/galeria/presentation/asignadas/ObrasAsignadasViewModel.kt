package com.tecnisis.galeria.presentation.asignadas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.EvaluacionError
import com.tecnisis.galeria.domain.model.obrasEvaluadas
import com.tecnisis.galeria.domain.model.obrasPorEvaluar
import com.tecnisis.galeria.domain.usecase.DescargarFotoObraUseCase
import com.tecnisis.galeria.domain.usecase.ObservarMiFichaExpertoUseCase
import com.tecnisis.galeria.domain.usecase.ObservarObrasAsignadasUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** La pestaña Asignadas del Especialista de Arte, en tiempo real. */
class ObrasAsignadasViewModel(
    observarObrasAsignadas: ObservarObrasAsignadasUseCase,
    observarMiFicha: ObservarMiFichaExpertoUseCase,
    private val descargarFotoObra: DescargarFotoObraUseCase
) : ViewModel() {

    private val pestana = MutableStateFlow(PestanaAsignadas.POR_EVALUAR)

    val uiState: StateFlow<ObrasAsignadasUiState> = combine(
        observarObrasAsignadas(),
        // Si la ficha no se puede leer, la lista igual se muestra (solo falta la carga)
        observarMiFicha().catch { emit(null) },
        pestana
    ) { obras, ficha, pestanaActual ->
        ObrasAsignadasUiState(
            cargando = false,
            pestana = pestanaActual,
            porEvaluar = obrasPorEvaluar(obras),
            evaluadas = obrasEvaluadas(obras),
            carga = ficha?.carga
        )
    }
        .catch { e -> emit(ObrasAsignadasUiState(cargando = false, mensajeError = mensajeDe(e))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObrasAsignadasUiState())

    fun onPestanaChange(nueva: PestanaAsignadas) {
        pestana.value = nueva
    }

    /** Para las miniaturas: null si no se pudo descargar (se ve el dibujo de pigmentos). */
    suspend fun descargarFoto(ruta: String): ByteArray? =
        descargarFotoObra(ruta).getOrNull()

    private fun mensajeDe(e: Throwable): String = when (e) {
        EvaluacionError.SinConexion -> "Sin conexión a internet. Vuelve a abrir la pestaña cuando tengas señal"
        EvaluacionError.SinPermiso -> "Solo el Especialista de Arte puede ver las obras asignadas"
        else -> "No se pudieron cargar las obras. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                ObrasAsignadasViewModel(
                    container.observarObrasAsignadas,
                    container.observarMiFichaExperto,
                    container.descargarFotoObra
                )
            }
        }
    }
}