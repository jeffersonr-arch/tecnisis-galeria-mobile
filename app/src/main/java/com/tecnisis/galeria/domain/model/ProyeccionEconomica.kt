package com.tecnisis.galeria.domain.model

import java.text.Collator
import java.util.Locale

/** HU12: máximo de barras del gráfico; con más, las 9 primeras + «Otras». */
const val MAXIMO_BARRAS = 10

/** HU12: largo máximo de la etiqueta de una barra (obra o socio), con «…» incluido. */
const val LARGO_ETIQUETA = 18

/**
 * HU12: un Documento de Venta (documentos_venta/{obraId}) tal como lo usa la proyección del Gerente.
 * Los montos van en céntimos.
 */
data class ObraProyectada(
    val obraId: String,
    val uid: String,                 // socio dueño de la obra
    val titulo: String,
    val tecnica: Tecnica?,
    val nombreSocio: String?,
    val numeroSocio: String?,
    val pvpCentimos: Long,
    val porcentaje: Int,
    val montoGaleriaCentimos: Long,
    val montoSocioCentimos: Long,
    val fechaTasacion: Long?
) {
    /** "María Elena López Díaz · SOC-0002" (PDF); sin nombre → "Sin dato". */
    val socio: String
        get() {
            val nombre = nombreSocio?.trim().orEmpty()
            if (nombre.isEmpty()) return SOCIO_SIN_DATO
            val numero = numeroSocio?.trim().orEmpty()
            return if (numero.isEmpty()) nombre else "$nombre · $numero"
        }

    /** "SOC-0002 · María…" (gráfico, chip Por socio); sin nombre → "Sin dato". */
    val etiquetaSocio: String
        get() {
            val nombre = nombreSocio?.trim().orEmpty()
            if (nombre.isEmpty()) return SOCIO_SIN_DATO
            val numero = numeroSocio?.trim().orEmpty()
            return etiquetaCorta(if (numero.isEmpty()) nombre else "$numero · $nombre")
        }
}

/** HU12: los chips del gráfico. */
enum class AgruparPor { OBRA, SOCIO }

/** HU12: una barra del gráfico. */
data class BarraProyeccion(
    val etiqueta: String,
    val gananciaCentimos: Long,
    val esOtras: Boolean = false
)

/** HU12: un socio con la suma de sus obras tasadas (tabla del PDF y chip Por socio). */
data class SocioProyectado(
    val uid: String,
    val socio: String,               // "nombre · número" o "Sin dato"
    val etiqueta: String,            // "SOC-0002 · María…" o "Sin dato"
    val obras: Int,
    val pvpCentimos: Long,
    val gananciaCentimos: Long,
    val socioRecibeCentimos: Long
)

/** HU12: todo lo que dibuja la pantalla y lo que se exporta. */
data class ProyeccionEconomica(
    val obras: List<ObraProyectada>,     // de mayor a menor ganancia; empate: título A–Z
    val socios: List<SocioProyectado>,   // de mayor a menor ganancia; empate: etiqueta A–Z
    val totalGaleriaCentimos: Long,      // ingreso total proyectado
    val totalPvpCentimos: Long,
    val totalSociosCentimos: Long
) {
    val cantidad: Int get() = obras.size

    /** "12 obras tasadas · PVP total S/ 22,450.50 · Socios reciben S/ 15,847.85". */
    val resumen: String
        get() {
            val obrasTexto = if (cantidad == 1) "1 obra tasada" else "$cantidad obras tasadas"
            return "$obrasTexto · PVP total ${textoSoles(totalPvpCentimos)} · " +
                    "Socios reciben ${textoSoles(totalSociosCentimos)}"
        }

    /** Las barras del chip elegido: máximo 10, «Otras» al final. */
    fun barras(agrupar: AgruparPor): List<BarraProyeccion> = when (agrupar) {
        AgruparPor.OBRA -> limitarBarras(obras.map { BarraProyeccion(etiquetaCorta(it.titulo), it.montoGaleriaCentimos) })
        AgruparPor.SOCIO -> limitarBarras(socios.map { BarraProyeccion(it.etiqueta, it.gananciaCentimos) })
    }
}

private val ordenEspanol: Collator = Collator.getInstance(Locale.forLanguageTag("es-PE"))

/**
 * HU12: una sola regla para obra y socio. Hasta 18 caracteres va completo; si es más largo,
 * los 17 primeros sin espacios al final + "…".
 */
fun etiquetaCorta(texto: String): String {
    val limpio = texto.trim()
    return if (limpio.length <= LARGO_ETIQUETA) limpio
    else limpio.take(LARGO_ETIQUETA - 1).trimEnd() + "…"
}

/** HU12: si hay más de 10 barras, quedan las 9 primeras y «Otras (N)» con la suma del resto, al final. */
fun limitarBarras(ordenadas: List<BarraProyeccion>): List<BarraProyeccion> {
    if (ordenadas.size <= MAXIMO_BARRAS) return ordenadas
    val visibles = ordenadas.take(MAXIMO_BARRAS - 1)
    val resto = ordenadas.drop(MAXIMO_BARRAS - 1)
    return visibles + BarraProyeccion("Otras (${resto.size})", resto.sumOf { it.gananciaCentimos }, esOtras = true)
}

/** HU12: ordena las obras, agrupa por socio (uid) y suma los totales. */
fun calcularProyeccion(obras: List<ObraProyectada>): ProyeccionEconomica {
    val ordenadas = obras.sortedWith(
        compareByDescending<ObraProyectada> { it.montoGaleriaCentimos }
            .thenComparator { a, b -> ordenEspanol.compare(a.titulo, b.titulo) }
    )
    val socios = ordenadas.groupBy { it.uid }.map { (uid, delSocio) ->
        val primera = delSocio.first()
        SocioProyectado(
            uid = uid,
            socio = primera.socio,
            etiqueta = primera.etiquetaSocio,
            obras = delSocio.size,
            pvpCentimos = delSocio.sumOf { it.pvpCentimos },
            gananciaCentimos = delSocio.sumOf { it.montoGaleriaCentimos },
            socioRecibeCentimos = delSocio.sumOf { it.montoSocioCentimos }
        )
    }.sortedWith(
        compareByDescending<SocioProyectado> { it.gananciaCentimos }
            .thenComparator { a, b -> ordenEspanol.compare(a.etiqueta, b.etiqueta) }
    )
    return ProyeccionEconomica(
        obras = ordenadas,
        socios = socios,
        totalGaleriaCentimos = obras.sumOf { it.montoGaleriaCentimos },
        totalPvpCentimos = obras.sumOf { it.pvpCentimos },
        totalSociosCentimos = obras.sumOf { it.montoSocioCentimos }
    )
}