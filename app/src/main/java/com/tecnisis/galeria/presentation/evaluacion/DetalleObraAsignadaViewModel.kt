package com.tecnisis.galeria.presentation.evaluacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.EvaluacionError
import com.tecnisis.galeria.domain.model.TipoVeredicto
import com.tecnisis.galeria.domain.model.comentarioValido
import com.tecnisis.galeria.domain.usecase.DescargarFotoObraUseCase
import com.tecnisis.galeria.domain.usecase.EtiquetarFotoUseCase
import com.tecnisis.galeria.domain.usecase.ObservarMiFichaExpertoUseCase
import com.tecnisis.galeria.domain.usecase.ObservarObraAsignadaUseCase
import com.tecnisis.galeria.domain.usecase.RegistrarVeredictoUseCase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.usecase.DescargarDocumentoObraUseCase

/** HU08: el detalle de una obra asignada, las etiquetas de ML Kit y el veredicto del Especialista. */
class DetalleObraAsignadaViewModel(
    private val id: String,
    private val observarObra: ObservarObraAsignadaUseCase,
    private val observarMiFicha: ObservarMiFichaExpertoUseCase,
    private val descargarFotoObra: DescargarFotoObraUseCase,
    private val etiquetarFoto: EtiquetarFotoUseCase,
    private val registrarVeredicto: RegistrarVeredictoUseCase,
    private val descargarDocumentoObra: DescargarDocumentoObraUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleObraAsignadaUiState())
    val uiState: StateFlow<DetalleObraAsignadaUiState> = _uiState.asStateFlow()

    /** Una sola descarga por foto: la usan la imagen de la pantalla y ML Kit (mismos bytes). */
    private val fotos = mutableMapOf<String, Deferred<ByteArray?>>()

    /** ML Kit corre una vez al abrir; el resultado no se guarda en Firestore. */
    private var yaEtiqueto = false

    init {
        viewModelScope.launch {
            combine(observarObra(id), observarMiFicha().catch { emit(null) }) { obra, ficha -> obra to ficha }
                .catch { e -> _uiState.update { it.copy(cargando = false, errorCarga = mensajeCarga(e)) } }
                .collect { (obra, ficha) ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            obra = obra,
                            ficha = ficha,
                            errorCarga = if (obra == null) "La obra ya no existe" else null
                        )
                    }
                    if (obra != null && !yaEtiqueto) {
                        yaEtiqueto = true
                        etiquetar(obra.fotoRuta)
                    }
                }
        }
    }

    /** La pantalla la usa para mostrar la foto (rememberFotoRemota). */
    suspend fun descargarFoto(ruta: String): ByteArray? = foto(ruta)

    private suspend fun foto(ruta: String): ByteArray? {
        if (ruta.isBlank()) return null
        return fotos.getOrPut(ruta) { viewModelScope.async { descargarFotoObra(ruta).getOrNull() } }.await()
    }

    private fun etiquetar(ruta: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(etiquetas = EstadoEtiquetas.Analizando) }
            val bytes = foto(ruta)
            val resultado = if (bytes == null) {
                EstadoEtiquetas.Error
            } else {
                etiquetarFoto(bytes).fold(
                    onSuccess = { lista -> if (lista.isEmpty()) EstadoEtiquetas.SinResultados else EstadoEtiquetas.Listas(lista) },
                    onFailure = { EstadoEtiquetas.Error }
                )
            }
            _uiState.update { it.copy(etiquetas = resultado) }
        }
    }

    // ---------- Veredicto ----------

    fun onAceptar() = abrirDialogo(TipoVeredicto.ACEPTAR)
    fun onRechazar() = abrirDialogo(TipoVeredicto.RECHAZAR)

    private fun abrirDialogo(tipo: TipoVeredicto) {
        if (!_uiState.value.puedeEvaluar) return
        _uiState.update {
            it.copy(dialogo = tipo, comentario = "", mostrarErrorComentario = false, errorDialogo = null)
        }
    }

    fun onComentario(texto: String) = _uiState.update {
        it.copy(comentario = texto, mostrarErrorComentario = false, errorDialogo = null)
    }

    /** Mientras se guarda, el diálogo no se cierra. */
    fun onCerrarDialogo() {
        if (_uiState.value.guardando) return
        _uiState.update { it.copy(dialogo = null) }
    }

    fun onConfirmar() {
        val estado = _uiState.value
        val tipo = estado.dialogo ?: return
        if (estado.guardando) return
        // El comentario se revisa aquí para marcar el campo en rojo sin cerrar el diálogo
        if (!comentarioValido(tipo, estado.comentario)) {
            _uiState.update { it.copy(mostrarErrorComentario = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorDialogo = null) }
            registrarVeredicto(id, tipo, estado.comentario)
                .onSuccess {
                    val texto = if (tipo == TipoVeredicto.ACEPTAR) "Obra aceptada" else "Obra rechazada"
                    _uiState.update { it.copy(dialogo = null, aviso = "$texto. Se avisó al socio.") }
                }
                .onFailure { e -> _uiState.update { it.copy(errorDialogo = mensajeVeredicto(e)) } }
            _uiState.update { it.copy(guardando = false) }
        }
    }

    fun onAvisoMostrado() = _uiState.update { it.copy(aviso = null) }

    /** El Especialista abre la copia del archivo de la galería. */
    fun onVerDocumento() {
        if (_uiState.value.abriendoDocumento) return
        viewModelScope.launch {
            _uiState.update { it.copy(abriendoDocumento = true) }
            descargarDocumentoObra(id, CopiaDocumento.ARCHIVO)
                .onSuccess { ruta -> _uiState.update { it.copy(pdfParaAbrir = ruta) } }
                .onFailure { e -> _uiState.update { it.copy(aviso = mensajeDocumento(e)) } }
            _uiState.update { it.copy(abriendoDocumento = false) }
        }
    }

    /** La Route ya abrió el PDF (o avisó que no hay app de PDF). */
    fun onPdfAbierto(abierto: Boolean) = _uiState.update {
        it.copy(pdfParaAbrir = null, aviso = if (abierto) it.aviso else "Instala una app para ver PDF (por ejemplo, Google Drive)")
    }

    private fun mensajeDocumento(e: Throwable): String = when (e) {
        DecisionError.SinConexion -> "Necesitas internet para abrir el documento"
        DecisionError.NoEncontrada -> "Esta obra se rechazó antes de que existiera el documento"
        DecisionError.SinPermiso -> "Tu cuenta no puede abrir este documento"
        else -> "No se pudo abrir el documento. Inténtalo de nuevo"
    }

    private fun mensajeVeredicto(e: Throwable): String = when (e) {
        EvaluacionError.ComentarioInvalido -> "Escribe el motivo del rechazo (hasta 500 caracteres)"
        EvaluacionError.SinConexion -> "Necesitas internet para registrar el veredicto. No se guardó nada."
        EvaluacionError.SinPermiso -> "Tu sesión venció o tu cuenta no es de Especialista de Arte"
        EvaluacionError.NoAsignada -> "Esta obra ya no está asignada a ti"
        EvaluacionError.YaEvaluada -> "Esta obra ya tiene veredicto"
        EvaluacionError.NoEncontrada -> "La obra ya no existe"
        EvaluacionError.PdfNoGenerado -> "No se pudo generar el documento de rechazo. No se guardó nada."
        EvaluacionError.FaltanDocumentos -> "No se pudo subir el documento de rechazo. No se guardó nada; inténtalo de nuevo."
        is EvaluacionError.Servidor -> "No se pudo guardar el veredicto (${e.detalle})"
        else -> "No se pudo guardar el veredicto. Inténtalo de nuevo"
    }

    private fun mensajeCarga(e: Throwable): String = when (e) {
        EvaluacionError.SinConexion -> "Sin conexión a internet. Vuelve a abrir la obra cuando tengas señal"
        EvaluacionError.SinPermiso -> "Esta obra no está asignada a ti"
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
                DetalleObraAsignadaViewModel(
                    id,
                    container.observarObraAsignada,
                    container.observarMiFichaExperto,
                    container.descargarFotoObra,
                    container.etiquetarFoto,
                    container.registrarVeredicto,
                    container.descargarDocumentoObra
                )
            }
        }
    }
}