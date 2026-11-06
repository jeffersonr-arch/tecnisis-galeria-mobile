package com.tecnisis.galeria.presentation.detallesolicitud

import com.tecnisis.galeria.domain.model.EstadoAnalisis
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.OpcionExperto
import com.tecnisis.galeria.domain.model.SolicitudEnRevision
import com.tecnisis.galeria.domain.model.analisisVencido
import com.tecnisis.galeria.domain.model.expertoValido
import com.tecnisis.galeria.domain.model.opcionesPara
import com.tecnisis.galeria.domain.model.todasConExperto

data class DetalleSolicitudUiState(
    val cargando: Boolean = true,
    val solicitud: SolicitudEnRevision? = null,
    val expertos: List<PerfilEspecialista> = emptyList(),
    val seleccion: Map<String, String?> = emptyMap(),   // obraId → uid del Experto elegido (solo en la pantalla)
    val hayCambios: Boolean = false,                    // el Anfitrión cambió algún Experto
    val obraCambiando: String? = null,                  // obra con la hoja "Cambiar Experto" abierta
    val confirmarSalida: Boolean = false,
    val llamandoAnalisis: Boolean = false,              // la llamada Retrofit está en marcha
    val ahora: Long = System.currentTimeMillis(),       // reloj del celular, solo para mostrar "Reintentar"
    val errorCarga: String? = null,
    val aviso: String? = null
) {
    private val analisis: EstadoAnalisis
        get() = solicitud?.analisis ?: EstadoAnalisis.PENDIENTE

    val enRevision: Boolean
        get() = solicitud?.estado == EstadoSolicitud.ENVIADA

    /** "Analizando…": la función trabaja (y no venció) o la app acaba de llamarla. */
    val analizando: Boolean
        get() = enRevision && analisis != EstadoAnalisis.LISTO && (
                llamandoAnalisis ||
                        (analisis == EstadoAnalisis.EN_CURSO && !analisisVencido(solicitud?.analisisDesde, ahora))
                )

    /** "Reintentar": falló, lleva más de 3 minutos "en curso" o la primera llamada no salió (sin internet). */
    val mostrarReintentar: Boolean
        get() = enRevision && !analizando && analisis != EstadoAnalisis.LISTO

    val analisisListo: Boolean
        get() = analisis == EstadoAnalisis.LISTO

    val obras: List<ObraEnRevision>
        get() = solicitud?.obras.orEmpty()

    fun expertoDe(obra: ObraEnRevision): PerfilEspecialista? =
        expertoValido(obra, expertos, seleccion)

    fun opcionesDe(obra: ObraEnRevision): List<OpcionExperto> =
        opcionesPara(obra, expertos, seleccion)

    val obrasConExperto: Int
        get() = obras.count { expertoDe(it) != null }

    val puedeAprobar: Boolean
        get() = todasConExperto(obras, expertos, seleccion)

    val obraEnHoja: ObraEnRevision?
        get() = obras.firstOrNull { it.id == obraCambiando }
}