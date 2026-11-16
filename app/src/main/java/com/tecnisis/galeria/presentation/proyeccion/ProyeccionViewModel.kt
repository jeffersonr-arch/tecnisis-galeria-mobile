package com.tecnisis.galeria.presentation.proyeccion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.AgruparPor
import com.tecnisis.galeria.domain.model.ArchivoReporte
import com.tecnisis.galeria.domain.model.FormatoReporte
import com.tecnisis.galeria.domain.model.ProyeccionEconomica
import com.tecnisis.galeria.domain.model.ProyeccionError
import com.tecnisis.galeria.domain.usecase.ExportarProyeccionUseCase
import com.tecnisis.galeria.domain.usecase.ObservarProyeccionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lo calculado con documentos_venta, o el error si no se pudo leer. */
private data class ResultadoProyeccion(val proyeccion: ProyeccionEconomica?, val error: String?)

/** Estado de la exportación: generando, archivo listo para compartir o aviso de error. */
private data class Exportacion(
    val exportando: Boolean = false,
    val archivo: ArchivoReporte? = null,
    val aviso: String? = null
)

/** Proyección económica del Gerente, en tiempo real. El chip elegido y la exportación viven aquí. */
class ProyeccionViewModel(
    observarProyeccion: ObservarProyeccionUseCase,
    private val exportarProyeccion: ExportarProyeccionUseCase
) : ViewModel() {

    private val agrupar = MutableStateFlow(AgruparPor.OBRA)
    private val exportacion = MutableStateFlow(Exportacion())

    private val resultado = observarProyeccion()
        .map { ResultadoProyeccion(it, null) }
        .catch { e -> emit(ResultadoProyeccion(null, mensajeDe(e))) }

    val uiState: StateFlow<ProyeccionUiState> = combine(resultado, agrupar, exportacion) { r, a, e ->
        ProyeccionUiState(
            cargando = false,
            proyeccion = r.proyeccion,
            mensajeError = r.error,
            agrupar = a,
            exportando = e.exportando,
            archivoParaCompartir = e.archivo,
            avisoExportar = e.aviso
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProyeccionUiState())

    fun onAgrupar(agrupar: AgruparPor) {
        this.agrupar.value = agrupar
    }

    /** Exporta la proyección que se ve ahora (foto del momento). Mientras genera, los botones quedan apagados. */
    fun onExportar(formato: FormatoReporte) {
        val proyeccion = uiState.value.proyeccion ?: return
        if (exportacion.value.exportando) return
        exportacion.value = Exportacion(exportando = true)
        viewModelScope.launch {
            exportarProyeccion(proyeccion, formato)
                .onSuccess { archivo -> exportacion.value = Exportacion(archivo = archivo) }
                .onFailure { exportacion.value = Exportacion(aviso = AVISO_NO_SE_GENERO) }
        }
    }

    /** La Route ya intentó abrir el menú Compartir; false = no se pudo abrir. */
    fun onCompartido(abierto: Boolean) {
        exportacion.value = Exportacion(aviso = if (abierto) null else AVISO_NO_SE_COMPARTIO)
    }

    private fun mensajeDe(e: Throwable): String = when (e) {
        ProyeccionError.SinConexion -> "Sin conexión. Revisa tu internet y vuelve a entrar."
        ProyeccionError.SinPermiso -> "Solo el Gerente puede ver la proyección"
        else -> "No se pudo cargar la proyección. Inténtalo de nuevo"
    }

    companion object {
        const val AVISO_NO_SE_GENERO = "No se pudo generar el reporte. Inténtalo de nuevo"
        const val AVISO_NO_SE_COMPARTIO = "No se pudo abrir el menú Compartir. Inténtalo de nuevo"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                ProyeccionViewModel(container.observarProyeccion, container.exportarProyeccion)
            }
        }
    }
}