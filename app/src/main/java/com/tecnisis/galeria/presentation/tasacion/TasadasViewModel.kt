package com.tecnisis.galeria.presentation.tasacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.model.ObraTasada
import com.tecnisis.galeria.domain.model.TasacionError
import com.tecnisis.galeria.domain.usecase.DescargarDocumentoVentaUseCase
import com.tecnisis.galeria.domain.usecase.DescargarFotoObraUseCase
import com.tecnisis.galeria.domain.usecase.ObservarObrasTasadasUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** La lista que llega de Firestore, o el error si no se pudo leer. */
private data class ResultadoTasadas(val obras: List<ObraTasada>, val error: String?)

/** Lo que maneja el propio ViewModel (lo demás llega de Firestore). */
private data class EstadoLocalTasadas(
    val abriendo: String? = null,
    val pdfParaAbrir: String? = null,
    val aviso: String? = null
)

/** Los documentos de venta guardados, en tiempo real, con el PDF de cada uno (copia del archivo). */
class TasadasViewModel(
    observarObrasTasadas: ObservarObrasTasadasUseCase,
    private val descargarDocumentoVenta: DescargarDocumentoVentaUseCase,
    private val descargarFotoObra: DescargarFotoObraUseCase
) : ViewModel() {

    private val local = MutableStateFlow(EstadoLocalTasadas())

    private val lista = observarObrasTasadas()
        .map { obras -> ResultadoTasadas(obras, null) }
        .catch { e -> emit(ResultadoTasadas(emptyList(), mensajeDe(e))) }

    val uiState: StateFlow<TasadasUiState> = combine(lista, local) { resultado, l ->
        TasadasUiState(
            cargando = false,
            obras = resultado.obras,
            mensajeError = resultado.error,
            abriendo = l.abriendo,
            pdfParaAbrir = l.pdfParaAbrir,
            aviso = l.aviso
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasadasUiState())

    suspend fun descargarFoto(ruta: String): ByteArray? =
        descargarFotoObra(ruta).getOrNull()

    /** Ventas abre la copia del archivo de la galería. */
    fun onVerDocumento(obraId: String) {
        if (local.value.abriendo != null) return
        viewModelScope.launch {
            local.update { it.copy(abriendo = obraId) }
            descargarDocumentoVenta(obraId, CopiaDocumento.ARCHIVO)
                .onSuccess { ruta -> local.update { it.copy(pdfParaAbrir = ruta) } }
                .onFailure { e -> local.update { it.copy(aviso = mensajeDocumento(e)) } }
            local.update { it.copy(abriendo = null) }
        }
    }

    /** La Route ya abrió el PDF (o avisó que no hay app de PDF). */
    fun onPdfAbierto(abierto: Boolean) = local.update {
        it.copy(pdfParaAbrir = null, aviso = if (abierto) it.aviso else "Instala una app para ver PDF (por ejemplo, Google Drive)")
    }

    fun onAvisoMostrado() = local.update { it.copy(aviso = null) }

    private fun mensajeDe(e: Throwable): String = when (e) {
        TasacionError.SinConexion -> "Sin conexión a internet. Vuelve a abrir la pestaña cuando tengas señal"
        TasacionError.SinPermiso -> "Solo el Especialista de Ventas puede ver las obras tasadas"
        else -> "No se pudieron cargar las obras tasadas. Inténtalo de nuevo"
    }

    private fun mensajeDocumento(e: Throwable): String = when (e) {
        DecisionError.SinConexion -> "Necesitas internet para abrir el documento"
        DecisionError.NoEncontrada -> "No se encontró el documento de venta"
        DecisionError.SinPermiso -> "Tu cuenta no puede abrir este documento"
        else -> "No se pudo abrir el documento. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                TasadasViewModel(container.observarObrasTasadas, container.descargarDocumentoVenta, container.descargarFotoObra)
            }
        }
    }
}