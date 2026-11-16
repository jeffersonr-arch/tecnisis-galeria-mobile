package com.tecnisis.galeria.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.DashboardError
import com.tecnisis.galeria.domain.model.DashboardSolicitudes
import com.tecnisis.galeria.domain.model.FiltroFecha
import com.tecnisis.galeria.domain.model.FiltrosDashboard
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.usecase.ObservarDashboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Lo calculado con los datos de Firestore, o el error si no se pudo leer. */
private data class ResultadoDashboard(val dashboard: DashboardSolicitudes?, val error: String?)

/** Dashboard de solicitudes del Gerente, en tiempo real. Los filtros viven aquí. */
class DashboardViewModel(observarDashboard: ObservarDashboardUseCase) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosDashboard())
    private val eligiendoRango = MutableStateFlow(false)

    private val resultado = observarDashboard(filtros)
        .map { ResultadoDashboard(it, null) }
        .catch { e -> emit(ResultadoDashboard(null, mensajeDe(e))) }

    val uiState: StateFlow<DashboardUiState> = combine(resultado, filtros, eligiendoRango) { r, f, eligiendo ->
        DashboardUiState(
            cargando = false,
            filtros = f,
            dashboard = r.dashboard,
            mensajeError = r.error,
            eligiendoRango = eligiendo
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onFecha(fecha: FiltroFecha) = filtros.update { it.copy(fecha = fecha) }
    fun onTecnica(tecnica: Tecnica?) = filtros.update { it.copy(tecnica = tecnica) }
    fun onSocio(socio: String?) = filtros.update { it.copy(socio = socio) }
    fun onExperto(uid: String?) = filtros.update { it.copy(expertoUid = uid) }

    fun onAbrirRango() {
        eligiendoRango.value = true
    }

    /** Cancelar con la X: el filtro de fecha queda como estaba. */
    fun onCerrarRango() {
        eligiendoRango.value = false
    }

    fun onRangoElegido(inicioUtc: Long, finUtc: Long) {
        filtros.update { it.copy(fecha = FiltroFecha.Rango(inicioUtc, finUtc)) }
        eligiendoRango.value = false
    }

    private fun mensajeDe(e: Throwable): String = when (e) {
        DashboardError.SinConexion -> "Sin conexión. Revisa tu internet y vuelve a entrar."
        DashboardError.SinPermiso -> "Solo el Gerente puede ver el dashboard"
        else -> "No se pudo cargar el dashboard. Inténtalo de nuevo"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GaleriaApplication).container
                DashboardViewModel(container.observarDashboard)
            }
        }
    }
}