package com.tecnisis.galeria.presentation.detallesolicitud

import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.EstadoAnalisis
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.OpcionExperto
import com.tecnisis.galeria.domain.model.SolicitudEnRevision
import com.tecnisis.galeria.domain.model.analisisVencido
import com.tecnisis.galeria.domain.model.expertoValido
import com.tecnisis.galeria.domain.model.motivoSugerido
import com.tecnisis.galeria.domain.model.opcionesPara
import com.tecnisis.galeria.domain.model.todasConExperto

enum class DialogoDecision { APROBAR, RECHAZAR }

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
    val aviso: String? = null,
    val dialogo: DialogoDecision? = null,
    val motivo: String = "",
    val mostrarErrorMotivo: Boolean = false,
    val decidiendo: Boolean = false,                    // generando PDF, subiendo y llamando a la función
    val abriendo: CopiaDocumento? = null,               // copia que se está descargando
    val pdfParaAbrir: String? = null,                    // ruta del PDF listo para abrir (lo consume la Route)
    val abriendoObra: String? = null                    // obra cuyo documento de rechazo se está descargando
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

    // ---------- Parte 2 ----------

    val aprobada: Boolean
        get() = solicitud?.estado == EstadoSolicitud.APROBADA

    /** Ya resuelta, el Experto se muestra aunque ahora esté ocupado (su carga subió al aprobar). */
    fun expertoAsignado(obra: ObraEnRevision): PerfilEspecialista? =
        expertos.firstOrNull { it.uid == obra.expertoAsignadoUid }

    /** Texto sobre los botones: por qué Aprobar está apagado, o qué está pasando. */
    val ayudaDecision: String?
        get() = when {
            decidiendo -> "Generando los documentos y guardando la decisión…"
            !analisisListo -> "Podrás aprobar cuando termine el análisis."
            !puedeAprobar -> "Para aprobar, todas las obras deben tener Experto."
            else -> null
        }

    /** Resumen del diálogo Aprobar: cada obra con el Experto elegido. */
    val filasAprobacion: List<FilaAprobacion>
        get() = obras.mapNotNull { obra -> expertoDe(obra)?.let { FilaAprobacion(obra.titulo, it.nombre) } }

    /** Texto que se ofrece en el diálogo Rechazar si falta Experto en alguna obra. */
    val sugerenciaMotivo: String?
        get() = if (analisisListo) motivoSugerido(obras, expertos, seleccion) else null
}