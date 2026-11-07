package com.tecnisis.galeria.presentation.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.CampoObra
import com.tecnisis.galeria.domain.model.FormularioObra
import com.tecnisis.galeria.domain.model.MAX_OBRAS_POR_SOLICITUD
import com.tecnisis.galeria.domain.model.NOTA_MAXIMA
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.model.TITULO_MAXIMO
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.usecase.CrearDestinoCamaraUseCase
import com.tecnisis.galeria.domain.usecase.DescartarFotoUseCase
import com.tecnisis.galeria.domain.usecase.EliminarObraUseCase
import com.tecnisis.galeria.domain.usecase.GuardarObraUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerObraUseCase
import com.tecnisis.galeria.domain.usecase.PrepararFotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrarObraViewModel(
    private val obraId: String?,   // null = obra nueva
    private val obtenerObra: ObtenerObraUseCase,
    private val guardarObra: GuardarObraUseCase,
    private val eliminarObra: EliminarObraUseCase,
    private val crearDestinoCamara: CrearDestinoCamaraUseCase,
    private val prepararFoto: PrepararFotoUseCase,
    private val descartarFoto: DescartarFotoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RegistrarObraUiState(esEdicion = obraId != null, cargando = obraId != null)
    )
    val uiState: StateFlow<RegistrarObraUiState> = _uiState.asStateFlow()

    private var fotoGuardada: String? = null     // la que ya está en Room (solo al editar)
    private var notaAntesDeDictar = ""
    private var fotosResueltas = false           // guardó o eliminó: ya no hay fotos sueltas que borrar

    init {
        if (obraId != null) cargar(obraId)
    }

    private fun cargar(id: String) {
        viewModelScope.launch {
            obtenerObra(id)
                .onSuccess { obra ->
                    fotoGuardada = obra.fotoLocal
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            titulo = obra.titulo,
                            tecnica = obra.tecnica,
                            alto = obra.altoCm.comoMedida(),
                            ancho = obra.anchoCm.comoMedida(),
                            profundidad = obra.profundidadCm?.comoMedida().orEmpty(),
                            fotoLocal = obra.fotoLocal,
                            nota = obra.nota
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(cargando = false, terminado = true) } }
        }
    }

    // ---------- Campos (cambiar un campo le quita su error) ----------

    fun onTituloChange(valor: String) =
        _uiState.update { it.copy(titulo = valor.take(TITULO_MAXIMO), errores = it.errores - CampoObra.TITULO) }

    fun onTecnicaChange(tecnica: Tecnica) =
        _uiState.update { it.copy(tecnica = tecnica, errores = it.errores - CampoObra.TECNICA) }

    fun onAltoChange(valor: String) =
        _uiState.update { it.copy(alto = soloMedida(valor), errores = it.errores - CampoObra.ALTO) }

    fun onAnchoChange(valor: String) =
        _uiState.update { it.copy(ancho = soloMedida(valor), errores = it.errores - CampoObra.ANCHO) }

    fun onProfundidadChange(valor: String) =
        _uiState.update { it.copy(profundidad = soloMedida(valor), errores = it.errores - CampoObra.PROFUNDIDAD) }

    fun onNotaChange(valor: String) =
        _uiState.update { it.copy(nota = valor.take(NOTA_MAXIMA), errores = it.errores - CampoObra.NOTA, avisoVoz = null) }

    // ---------- Foto ----------

    /** content:// donde la app de cámara guardará la foto. */
    fun crearDestinoFoto(): String = crearDestinoCamara()

    /** Llega de la cámara o de la galería: se copia reducida dentro de la app. */
    fun onFotoRecibida(origen: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(preparandoFoto = true) }
            prepararFoto(origen)
                .onSuccess { ruta ->
                    val anterior = _uiState.value.fotoLocal
                    if (anterior != null && anterior != fotoGuardada) descartarFoto(anterior)
                    _uiState.update { it.copy(preparandoFoto = false, fotoLocal = ruta, errores = it.errores - CampoObra.FOTO) }
                }
                .onFailure {
                    _uiState.update { it.copy(preparandoFoto = false, mensaje = "No se pudo leer la foto. Prueba con otra") }
                }
        }
    }

    fun onSinCamara() =
        _uiState.update { it.copy(mensaje = "No se encontró una app de cámara. Elige la foto de la galería") }

    // ---------- Nota de voz ----------

    fun onDictadoIniciado() {
        notaAntesDeDictar = _uiState.value.nota.trim()
        _uiState.update { it.copy(dictando = true, avisoVoz = null) }
    }

    /** Lo dictado se agrega después de lo que ya había escrito. */
    fun onDictadoTexto(texto: String) = _uiState.update {
        it.copy(nota = unir(notaAntesDeDictar, texto).take(NOTA_MAXIMA), errores = it.errores - CampoObra.NOTA)
    }

    fun onDictadoTerminado() = _uiState.update { it.copy(dictando = false) }

    fun onDictadoFallido(sinVoz: Boolean) = _uiState.update {
        it.copy(dictando = false, avisoVoz = if (sinVoz) AvisoVoz.SIN_VOZ else AvisoVoz.FALLO)
    }

    fun onAvisoVoz(aviso: AvisoVoz) = _uiState.update { it.copy(dictando = false, avisoVoz = aviso) }

    // ---------- Guardar y eliminar ----------

    fun onGuardar() {
        val e = _uiState.value
        if (e.guardando || e.preparandoFoto || e.cargando) return
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val formulario = FormularioObra(
                id = obraId, titulo = e.titulo, tecnica = e.tecnica, alto = e.alto, ancho = e.ancho,
                profundidad = e.profundidad, fotoLocal = e.fotoLocal, nota = e.nota
            )
            guardarObra(formulario)
                .onSuccess {
                    fotosResueltas = true
                    _uiState.update { it.copy(guardando = false, terminado = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        if (error is ObraError.DatosInvalidos)
                            it.copy(guardando = false, errores = error.campos)
                        else it.copy(guardando = false, mensaje = mensajeDe(error))
                    }
                }
        }
    }

    fun onPedirEliminar() = _uiState.update { it.copy(confirmandoEliminar = true) }

    fun onCancelarEliminar() = _uiState.update { it.copy(confirmandoEliminar = false) }

    fun onConfirmarEliminar() {
        val id = obraId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(confirmandoEliminar = false, guardando = true) }
            eliminarObra(id)
                .onSuccess {
                    // Si eligió una foto nueva y luego eliminó, esa foto quedó suelta
                    val actual = _uiState.value.fotoLocal
                    if (actual != null && actual != fotoGuardada) descartarFoto(actual)
                    fotosResueltas = true
                    _uiState.update { it.copy(guardando = false, terminado = true) }
                }
                .onFailure { error -> _uiState.update { it.copy(guardando = false, mensaje = mensajeDe(error)) } }
        }
    }

    fun onMensajeMostrado() = _uiState.update { it.copy(mensaje = null) }

    /** Si sale sin guardar, borra la foto nueva que eligió (la que ya estaba en Room no se toca). */
    override fun onCleared() {
        val actual = _uiState.value.fotoLocal
        if (!fotosResueltas && actual != null && actual != fotoGuardada) descartarFoto(actual)
    }

    private fun soloMedida(valor: String) = valor.filter { it.isDigit() || it == ',' || it == '.' }.take(7)

    private fun unir(base: String, dictado: String) =
        listOf(base, dictado.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    private fun mensajeDe(error: Throwable): String = when (error) {
        ObraError.SolicitudNoEditable -> "Tu solicitud ya se envió: no se puede cambiar"
        ObraError.LimiteDeObras -> "Una solicitud admite hasta $MAX_OBRAS_POR_SOLICITUD obras"
        ObraError.NoEncontrada -> "No se encontró la obra"
        ObraError.SinSesion -> "Tu sesión expiró. Vuelve a ingresar"
        ObraError.FotoNoLeida -> "No se pudo leer la foto. Prueba con otra"
        else -> "No se pudo guardar. Inténtalo de nuevo"
    }

    companion object {
        const val ARG_OBRA_ID = "obraId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                RegistrarObraViewModel(
                    obraId = createSavedStateHandle().get<String>(ARG_OBRA_ID),
                    obtenerObra = container.obtenerObra,
                    guardarObra = container.guardarObra,
                    eliminarObra = container.eliminarObra,
                    crearDestinoCamara = container.crearDestinoCamara,
                    prepararFoto = container.prepararFoto,
                    descartarFoto = container.descartarFoto
                )
            }
        }
    }
}