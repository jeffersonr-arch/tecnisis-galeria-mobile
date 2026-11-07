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
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.model.motivoValido
import com.tecnisis.galeria.domain.usecase.AprobarSolicitudUseCase
import com.tecnisis.galeria.domain.usecase.DescargarDocumentoUseCase
import com.tecnisis.galeria.domain.usecase.RechazarSolicitudUseCase
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
    private val descargarFotoObra: DescargarFotoObraUseCase,
    private val aprobarSolicitud: AprobarSolicitudUseCase,
    private val rechazarSolicitud: RechazarSolicitudUseCase,
    private val descargarDocumento: DescargarDocumentoUseCase
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

    /**
     * Volver (flecha o botón Atrás): mientras se guarda la decisión no se sale (se cortaría a la mitad);
     * si cambió algún Experto, primero pregunta. Devuelve true si puede salir ya.
     */
    fun onVolverPedido(): Boolean {
        if (_uiState.value.decidiendo) return false
        if (!_uiState.value.hayCambios) return true
        _uiState.update { it.copy(confirmarSalida = true) }
        return false
    }

    fun onCancelarSalida() = _uiState.update { it.copy(confirmarSalida = false) }

    fun onAvisoMostrado() = _uiState.update { it.copy(aviso = null) }

    // ---------- Parte 2: aprobar, rechazar y abrir los documentos ----------

    fun onAprobar() {
        if (_uiState.value.puedeAprobar) _uiState.update { it.copy(dialogo = DialogoDecision.APROBAR) }
    }

    fun onRechazar() = _uiState.update {
        it.copy(dialogo = DialogoDecision.RECHAZAR, motivo = "", mostrarErrorMotivo = false)
    }

    fun onCerrarDialogo() = _uiState.update { it.copy(dialogo = null) }

    fun onMotivo(texto: String) = _uiState.update { it.copy(motivo = texto, mostrarErrorMotivo = false) }

    fun onConfirmarAprobar() {
        val estado = _uiState.value
        if (estado.decidiendo) return
        decidir { aprobarSolicitud(id, estado.obras, estado.expertos, estado.seleccion) }
    }

    fun onConfirmarRechazo() {
        val estado = _uiState.value
        if (estado.decidiendo) return
        // El motivo se revisa aquí para marcar el campo en rojo sin cerrar el diálogo
        if (!motivoValido(estado.motivo)) {
            _uiState.update { it.copy(mostrarErrorMotivo = true) }
            return
        }
        decidir { rechazarSolicitud(id, estado.motivo) }
    }

    /** Cierra el diálogo, muestra "Generando…" y espera a la función. El cambio llega solo por Firestore. */
    private fun decidir(accion: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(dialogo = null, decidiendo = true) }
            accion()
                .onSuccess { _uiState.update { it.copy(hayCambios = false, aviso = "Listo. Se avisó al socio.") } }
                .onFailure { e -> _uiState.update { it.copy(aviso = mensajeDecision(e)) } }
            _uiState.update { it.copy(decidiendo = false) }
        }
    }

    fun onAbrirDocumento(copia: CopiaDocumento) {
        if (_uiState.value.abriendo != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(abriendo = copia) }
            descargarDocumento(id, copia)
                .onSuccess { ruta -> _uiState.update { it.copy(pdfParaAbrir = ruta) } }
                .onFailure { e -> _uiState.update { it.copy(aviso = mensajeDecision(e)) } }
            _uiState.update { it.copy(abriendo = null) }
        }
    }

    /** La Route ya abrió el PDF (o avisó que no hay app de PDF). */
    fun onPdfAbierto(abierto: Boolean) = _uiState.update {
        it.copy(pdfParaAbrir = null, aviso = if (abierto) it.aviso else "Instala una app para ver PDF (por ejemplo, Google Drive)")
    }

    private fun mensajeDecision(e: Throwable): String = when (e) {
        DecisionError.FaltaExperto -> "Todas las obras deben tener Experto para aprobar"
        DecisionError.MotivoInvalido -> "Escribe el motivo del rechazo"
        DecisionError.SinConexion -> "Sin conexión. No se guardó nada: inténtalo de nuevo"
        DecisionError.SinPermiso -> "Tu sesión venció o tu cuenta no es de Anfitrión"
        DecisionError.YaResuelta -> "Esta solicitud ya fue resuelta"
        DecisionError.ExpertoNoDisponible -> "Un Experto elegido ya no está disponible. Revisa las obras y vuelve a intentarlo"
        DecisionError.NoEncontrada -> "La solicitud o el documento ya no existe"
        DecisionError.PdfNoGenerado -> "No se pudo generar el documento PDF"
        is DecisionError.Servidor -> "No se pudo guardar la decisión (${e.detalle})"
        else -> "No se pudo completar la acción. Inténtalo de nuevo"
    }

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
                    container.descargarFotoObra,
                    container.aprobarSolicitud,
                    container.rechazarSolicitud,
                    container.descargarDocumento
                )
            }
        }
    }
}