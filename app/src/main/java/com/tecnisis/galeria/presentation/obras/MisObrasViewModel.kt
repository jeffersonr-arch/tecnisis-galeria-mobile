package com.tecnisis.galeria.presentation.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.usecase.DescargarDocumentoUseCase
import com.tecnisis.galeria.domain.usecase.DescargarDocumentoVentaUseCase
import com.tecnisis.galeria.domain.usecase.EnviarSolicitudUseCase
import com.tecnisis.galeria.domain.usecase.ObservarConexionUseCase
import com.tecnisis.galeria.domain.usecase.ObservarMiSolicitudUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tecnisis.galeria.domain.usecase.DescargarDocumentoObraUseCase

/** Lo que maneja el propio ViewModel (lo demás llega de Room y de la red). */
private data class EstadoLocal(
    val confirmandoEnvio: Boolean = false,
    val enviando: Boolean = false,
    val mensaje: String? = null,
    val abriendoDocumento: Boolean = false,
    val pdfParaAbrir: String? = null,
    val abriendoObra: String? = null
)

class MisObrasViewModel(
    observarMiSolicitud: ObservarMiSolicitudUseCase,
    observarConexion: ObservarConexionUseCase,
    private val enviarSolicitud: EnviarSolicitudUseCase,
    private val descargarDocumento: DescargarDocumentoUseCase,
    private val descargarDocumentoObra: DescargarDocumentoObraUseCase,
    private val descargarDocumentoVenta: DescargarDocumentoVentaUseCase
) : ViewModel() {

    private val local = MutableStateFlow(EstadoLocal())

    // Room avisa cada vez que cambia una obra o el estado de la solicitud (por ejemplo, cuando el worker la envía)
    val uiState: StateFlow<MisObrasUiState> = combine(
        local,
        observarMiSolicitud(),
        observarConexion()
    ) { l, solicitud, conexion ->
        MisObrasUiState(
            cargando = false,
            solicitud = solicitud,
            hayConexion = conexion,
            confirmandoEnvio = l.confirmandoEnvio,
            enviando = l.enviando,
            mensaje = l.mensaje,
            abriendoDocumento = l.abriendoDocumento,
            pdfParaAbrir = l.pdfParaAbrir,
            abriendoObra = l.abriendoObra
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MisObrasUiState())

    fun onEnviar() = local.update { it.copy(confirmandoEnvio = true) }

    fun onCancelarEnvio() = local.update { it.copy(confirmandoEnvio = false) }

    fun onConfirmarEnvio() {
        if (local.value.enviando) return
        viewModelScope.launch {
            local.update { it.copy(confirmandoEnvio = false, enviando = true) }
            val error = enviarSolicitud().exceptionOrNull()
            local.update { it.copy(enviando = false, mensaje = error?.let { e -> mensajeDe(e) }) }
        }
    }

    fun onMensajeMostrado() = local.update { it.copy(mensaje = null) }

    /** Descarga la copia del artista (artista.pdf) de la solicitud resuelta. */
    fun onVerDocumento() {
        val solicitud = uiState.value.solicitud ?: return
        if (local.value.abriendoDocumento) return
        viewModelScope.launch {
            local.update { it.copy(abriendoDocumento = true) }
            descargarDocumento(solicitud.id, CopiaDocumento.ARTISTA)
                .onSuccess { ruta -> local.update { it.copy(pdfParaAbrir = ruta) } }
                .onFailure { e -> local.update { it.copy(mensaje = mensajeDocumento(e)) } }
            local.update { it.copy(abriendoDocumento = false) }
        }
    }

    /** Descarga la copia del artista del documento de rechazo de una obra. */
    fun onVerDocumentoObra(obraId: String) {
        if (local.value.abriendoObra != null) return
        viewModelScope.launch {
            local.update { it.copy(abriendoObra = obraId) }
            descargarDocumentoObra(obraId, CopiaDocumento.ARTISTA)
                .onSuccess { ruta -> local.update { it.copy(pdfParaAbrir = ruta) } }
                .onFailure { e -> local.update { it.copy(mensaje = mensajeDocumentoObra(e)) } }
            local.update { it.copy(abriendoObra = null) }
        }
    }

    /** Descarga la copia del artista del documento de venta de una obra lista para exhibición. */
    fun onVerDocumentoVenta(obraId: String) {
        if (local.value.abriendoObra != null) return
        viewModelScope.launch {
            local.update { it.copy(abriendoObra = obraId) }
            descargarDocumentoVenta(obraId, CopiaDocumento.ARTISTA)
                .onSuccess { ruta -> local.update { it.copy(pdfParaAbrir = ruta) } }
                .onFailure { e -> local.update { it.copy(mensaje = mensajeDocumentoVenta(e)) } }
            local.update { it.copy(abriendoObra = null) }
        }
    }

    private fun mensajeDocumentoVenta(error: Throwable): String = when (error) {
        DecisionError.NoEncontrada -> "Tu documento de venta todavía no está disponible"
        DecisionError.SinConexion -> "Necesitas internet para descargar tu documento"
        DecisionError.SinPermiso -> "Tu documento todavía no está disponible. Inténtalo en unos minutos"
        else -> "No se pudo abrir tu documento. Inténtalo de nuevo"
    }

    /** Si el archivo no existe, la obra se rechazó antes de que existiera el documento. */
    private fun mensajeDocumentoObra(error: Throwable): String = when (error) {
        DecisionError.NoEncontrada -> "Esta obra se rechazó antes de que existiera el documento"
        DecisionError.SinConexion -> "Necesitas internet para descargar tu documento"
        DecisionError.SinPermiso -> "Tu documento todavía no está disponible. Inténtalo en unos minutos"
        else -> "No se pudo abrir tu documento. Inténtalo de nuevo"
    }

    /** La Route ya abrió el PDF (o avisó que no hay app de PDF). */
    fun onPdfAbierto(abierto: Boolean) = local.update {
        it.copy(pdfParaAbrir = null, mensaje = if (abierto) it.mensaje else "Instala una app para ver PDF (por ejemplo, Google Drive)")
    }

    private fun mensajeDocumento(error: Throwable): String = when (error) {
        DecisionError.SinConexion -> "Necesitas internet para descargar tu documento"
        DecisionError.SinPermiso, DecisionError.NoEncontrada -> "Tu documento todavía no está disponible. Inténtalo en unos minutos"
        else -> "No se pudo abrir tu documento. Inténtalo de nuevo"
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        ObraError.SinObras -> "Agrega al menos una obra antes de enviar"
        ObraError.SolicitudNoEditable -> "Tu solicitud ya se envió"
        ObraError.SinSesion -> "Tu sesión expiró. Vuelve a ingresar"
        else -> "No se pudo enviar. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                MisObrasViewModel(
                    container.observarMiSolicitud,
                    container.observarConexion,
                    container.enviarSolicitud,
                    container.descargarDocumento,
                    container.descargarDocumentoObra,
                    container.descargarDocumentoVenta
                )
            }
        }
    }
}