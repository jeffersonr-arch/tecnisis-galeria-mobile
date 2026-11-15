package com.tecnisis.galeria.domain.model

import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** HU11: texto del filtro de socio para las obras sin nombreSocio. */
const val SOCIO_SIN_DATO = "Sin dato"

/**
 * HU11: una obra con veredicto, tal como la usa el dashboard del Gerente.
 * aceptada = estado "aceptada" o "lista_exhibicion"; false = "rechazada".
 */
data class ObraEvaluada(
    val id: String,
    val aceptada: Boolean,
    val tecnica: Tecnica?,
    val nombreSocio: String?,
    val numeroSocio: String?,
    val evaluadaPor: String?,      // uid del Experto que dio el veredicto
    val fechaVeredicto: Long?
) {
    /** "María Elena López Díaz · SOC-0002"; sin nombreSocio → "Sin dato". */
    val socio: String
        get() {
            val nombre = nombreSocio?.trim().orEmpty()
            if (nombre.isEmpty()) return SOCIO_SIN_DATO
            val numero = numeroSocio?.trim().orEmpty()
            return if (numero.isEmpty()) nombre else "$nombre · $numero"
        }
}

/** HU11: un Experto (colección expertos) con su carga actual. */
data class ExpertoDashboard(
    val uid: String,
    val nombre: String,
    val activo: Boolean,
    val carga: Int                 // obras asignadas sin veredicto (ahora)
)

/** HU11: los chips de fecha del dashboard. */
sealed interface FiltroFecha {
    data object Todo : FiltroFecha
    data object SieteDias : FiltroFecha
    data object TreintaDias : FiltroFecha
    /** Días elegidos en el DateRangePicker: medianoche UTC de cada día (así los entrega Material 3). */
    data class Rango(val inicioUtc: Long, val finUtc: Long) : FiltroFecha
}

/** HU11: filtros del dashboard; null = "Todos". */
data class FiltrosDashboard(
    val fecha: FiltroFecha = FiltroFecha.Todo,
    val tecnica: Tecnica? = null,
    val socio: String? = null,      // el mismo texto de ObraEvaluada.socio
    val expertoUid: String? = null
)

/** HU11: la dona. Los porcentajes suman siempre 100. */
data class ResumenDona(val aceptadas: Int, val rechazadas: Int) {
    val total: Int get() = aceptadas + rechazadas

    /** Redondeo normal de aceptadas × 100 ÷ total (0,5 sube), con enteros. */
    val porcentajeAceptadas: Int
        get() = if (total == 0) 0 else (aceptadas * 200 + total) / (2 * total)

    val porcentajeRechazadas: Int
        get() = if (total == 0) 0 else 100 - porcentajeAceptadas

    /** "1 obra" / "N obras". */
    val textoCentro: String
        get() = if (total == 1) "1 obra" else "$total obras"
}

/** HU11: una fila del ranking de Expertos. */
data class FilaRanking(
    val uid: String,
    val nombre: String,
    val activo: Boolean,
    val evaluadas: Int,            // con los filtros y el rango
    val pendientes: Int            // expertos.carga, sin filtros
)

/** HU11: todo lo que dibuja la pantalla. */
data class DashboardSolicitudes(
    val dona: ResumenDona,
    val ranking: List<FilaRanking>,
    val socios: List<String>,              // opciones del filtro de socio (sin "Todos")
    val expertos: List<ExpertoDashboard>   // opciones del filtro de Experto, A–Z (sin "Todos")
)

private val ordenEspanol: Collator = Collator.getInstance(Locale.forLanguageTag("es-PE"))

/** 00:00 (hora del celular) del día de [ahora] movido [dias] días. */
fun inicioDelDia(ahora: Long, zona: TimeZone, dias: Int = 0): Long =
    Calendar.getInstance(zona).apply {
        timeInMillis = ahora
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_MONTH, dias)
    }.timeInMillis

/** El día de hoy (hora del celular) como medianoche UTC: así compara fechas el DateRangePicker. */
fun hoyEnUtc(ahora: Long, zona: TimeZone): Long {
    val local = Calendar.getInstance(zona).apply { timeInMillis = ahora }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Un día del DateRangePicker (medianoche UTC) a las 00:00 de ese mismo día en la hora del celular. */
private fun diaUtcALocal(diaUtc: Long, zona: TimeZone): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = diaUtc }
    return Calendar.getInstance(zona).apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Desde (incluido) y hasta (excluido); null = "Todo", sin límite. */
fun FiltroFecha.intervalo(ahora: Long, zona: TimeZone): LongRange? = when (this) {
    FiltroFecha.Todo -> null
    FiltroFecha.SieteDias -> inicioDelDia(ahora, zona, -6) until Long.MAX_VALUE
    FiltroFecha.TreintaDias -> inicioDelDia(ahora, zona, -29) until Long.MAX_VALUE
    is FiltroFecha.Rango -> {
        val desde = diaUtcALocal(inicioUtc, zona)
        val hastaExcluido = Calendar.getInstance(zona).apply {
            timeInMillis = diaUtcALocal(finUtc, zona)
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        desde until hastaExcluido
    }
}

/** "2 nov – 13 nov" para el chip del rango. */
fun FiltroFecha.Rango.texto(): String {
    val formato = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    fun dia(utc: Long) = formato.format(Date(utc)).replace(".", "")
    return "${dia(inicioUtc)} – ${dia(finUtc)}"
}

/** HU11: aplica los filtros, cuenta la dona y arma el ranking. */
fun calcularDashboard(
    obras: List<ObraEvaluada>,
    expertos: List<ExpertoDashboard>,
    filtros: FiltrosDashboard,
    ahora: Long,
    zona: TimeZone
): DashboardSolicitudes {
    val intervalo = filtros.fecha.intervalo(ahora, zona)
    val filtradas = obras.filter { obra ->
        val pasaFecha = intervalo == null || (obra.fechaVeredicto != null && obra.fechaVeredicto in intervalo)
        pasaFecha &&
                (filtros.tecnica == null || obra.tecnica == filtros.tecnica) &&
                (filtros.socio == null || obra.socio == filtros.socio) &&
                (filtros.expertoUid == null || obra.evaluadaPor == filtros.expertoUid)
    }

    val dona = ResumenDona(
        aceptadas = filtradas.count { it.aceptada },
        rechazadas = filtradas.count { !it.aceptada }
    )

    val porExperto = filtradas.groupingBy { it.evaluadaPor }.eachCount()
    val ranking = expertos
        .filter { filtros.expertoUid == null || it.uid == filtros.expertoUid }
        .map { FilaRanking(it.uid, it.nombre, it.activo, porExperto[it.uid] ?: 0, it.carga) }
        .sortedWith(compareByDescending<FilaRanking> { it.evaluadas }.thenBy(ordenEspanol) { it.nombre })

    val socios = obras.map { it.socio }.distinct()
    val opcionesSocio = socios.filter { it != SOCIO_SIN_DATO }.sortedWith(ordenEspanol) +
            if (SOCIO_SIN_DATO in socios) listOf(SOCIO_SIN_DATO) else emptyList()

    return DashboardSolicitudes(
        dona = dona,
        ranking = ranking,
        socios = opcionesSocio,
        expertos = expertos.sortedWith(compareBy(ordenEspanol) { it.nombre })
    )
}