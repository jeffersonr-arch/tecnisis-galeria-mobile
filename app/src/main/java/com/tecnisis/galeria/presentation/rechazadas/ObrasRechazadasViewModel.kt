package com.tecnisis.galeria.presentation.rechazadas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.model.HistoricoError
import com.tecnisis.galeria.domain.model.ObraRechazada
import com.tecnisis.galeria.domain.usecase.DescargarDocumentoObraUseCase
import com.tecnisis.galeria.domain.usecase.ObservarObrasRechazadasUseCase
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
private data class ResultadoLista(val obras: List<ObraRechazada>, val error: String?)

/** Lo que maneja el propio ViewModel (lo demás llega de Firestore). */
private data class EstadoLocal(
    val abriendo: String? = null,
    val pdfParaAbrir: String? = null,
    val aviso: String? = null
)

/** Histórico de obras rechazadas del Gerente, en tiempo real, con el PDF de cada una. */
class ObrasRechazadasViewModel(
    observarObrasRechazadas: ObservarObrasRechazadasUseCase,
    private val descargarDocumentoObra: DescargarDocumentoObraUseCase
) : ViewModel() {

    private val local = MutableStateFlow(EstadoLocal())

    private val lista = observarObrasRechazadas()
        .map { obras -> ResultadoLista(obras, null) }
        .catch { e -> emit(ResultadoLista(emptyList(), mensajeDe(e))) }

    val uiState: StateFlow<ObrasRechazadasUiState> = combine(lista, local) { resultado, l ->
        ObrasRechazadasUiState(
            cargando = false,
            obras = resultado.obras,
            mensajeError = resultado.error,
            abriendo = l.abriendo,
            pdfParaAbrir = l.pdfParaAbrir,
            aviso = l.aviso
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObrasRechazadasUiState())

    /** El Gerente abre la copia del archivo de la galería. */
    fun onVerDocumento(obraId: String) {
        if (local.value.abriendo != null) return
        viewModelScope.launch {
            local.update { it.copy(abriendo = obraId) }
            descargarDocumentoObra(obraId, CopiaDocumento.ARCHIVO)
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
        HistoricoError.SinConexion -> "Sin conexión a internet. Vuelve a abrir la pantalla cuando tengas señal"
        HistoricoError.SinPermiso -> "Solo el Gerente puede ver el histórico de obras rechazadas"
        else -> "No se pudo cargar el histórico. Inténtalo de nuevo"
    }

    private fun mensajeDocumento(e: Throwable): String = when (e) {
        DecisionError.SinConexion -> "Necesitas internet para abrir el documento"
        DecisionError.NoEncontrada -> "Esta obra se rechazó antes de que existiera el documento"
        DecisionError.SinPermiso -> "Tu cuenta no puede abrir este documento"
        else -> "No se pudo abrir el documento. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                ObrasRechazadasViewModel(container.observarObrasRechazadas, container.descargarDocumentoObra)
            }
        }
    }
}