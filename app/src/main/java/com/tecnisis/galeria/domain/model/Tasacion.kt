package com.tecnisis.galeria.domain.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * HU10: reglas de la tasación. Los montos van siempre en céntimos (Long) para no tener errores de decimales.
 * La Cloud Function registrarTasacion repite estas mismas reglas: las dos deben coincidir.
 */
const val PVP_MAXIMO_CENTIMOS = 100_000_000L // S/ 1,000,000.00
const val PORCENTAJE_MAXIMO = 100
const val MENSAJE_PORCENTAJE = "Debe ser un número entero de 0 a 100"

/** Por qué no se acepta el PVP escrito (el texto es el que ve el Especialista de Ventas debajo del campo). */
enum class ErrorPvp(val mensaje: String) {
    FORMATO("Usa solo números, con coma o punto y hasta 2 decimales"),
    CERO("El precio debe ser mayor a S/ 0.00"),
    MAXIMO("El precio máximo es S/ 1,000,000.00")
}

/** Lo que resulta de leer el PVP escrito. */
sealed interface LecturaPvp {
    data object Vacio : LecturaPvp
    data class Valido(val centimos: Long) : LecturaPvp
    data class Invalido(val error: ErrorPvp) : LecturaPvp
}

/** Lo que resulta de leer el porcentaje escrito. */
sealed interface LecturaPorcentaje {
    data object Vacio : LecturaPorcentaje
    data class Valido(val valor: Int) : LecturaPorcentaje
    data object Invalido : LecturaPorcentaje
}

// Dígitos, y si hay decimales: una sola coma o un solo punto con 1 o 2 dígitos. Sin separador de miles.
private val FORMATO_PVP = Regex("""^\d+([.,]\d{1,2})?$""")
private val FORMATO_PORCENTAJE = Regex("""^\d{1,3}$""")

/**
 * "1250,50", "1250.50" y "1250" son válidos; "1,250.50", "12,345", "1250." y "-5" no.
 * Debe ser mayor a 0 y como máximo 1 000 000.
 */
fun leerPvp(texto: String): LecturaPvp {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return LecturaPvp.Vacio
    if (!FORMATO_PVP.matches(limpio)) return LecturaPvp.Invalido(ErrorPvp.FORMATO)
    val partes = limpio.split('.', ',')
    val entero = partes[0].trimStart('0').ifEmpty { "0" }
    // Más de 7 cifras enteras ya pasa el máximo (y así no se desborda el Long)
    if (entero.length > 7) return LecturaPvp.Invalido(ErrorPvp.MAXIMO)
    val decimales = partes.getOrNull(1).orEmpty().padEnd(2, '0')
    val centimos = entero.toLong() * 100 + decimales.toLong()
    return when {
        centimos <= 0 -> LecturaPvp.Invalido(ErrorPvp.CERO)
        centimos > PVP_MAXIMO_CENTIMOS -> LecturaPvp.Invalido(ErrorPvp.MAXIMO)
        else -> LecturaPvp.Valido(centimos)
    }
}

/** Entero de 0 a 100 (0 y 100 se aceptan). "18.5", "101" y "-1" no. */
fun leerPorcentaje(texto: String): LecturaPorcentaje {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return LecturaPorcentaje.Vacio
    if (!FORMATO_PORCENTAJE.matches(limpio)) return LecturaPorcentaje.Invalido
    val valor = limpio.toInt()
    return if (valor in 0..PORCENTAJE_MAXIMO) LecturaPorcentaje.Valido(valor) else LecturaPorcentaje.Invalido
}

/** Los tres montos de una tasación, en céntimos. galeria + socio == pvp siempre. */
data class MontosTasacion(
    val pvpCentimos: Long,
    val porcentaje: Int,
    val galeriaCentimos: Long,
    val socioCentimos: Long
)

/**
 * galería = PVP × % ÷ 100, redondeado a 2 decimales (0,005 sube); socio = PVP − galería.
 * En céntimos: (pvpCentimos × % + 50) ÷ 100 con división entera.
 */
fun calcularMontos(pvpCentimos: Long, porcentaje: Int): MontosTasacion {
    require(pvpCentimos in 1..PVP_MAXIMO_CENTIMOS) { "PVP fuera de rango" }
    require(porcentaje in 0..PORCENTAJE_MAXIMO) { "Porcentaje fuera de rango" }
    val galeria = (pvpCentimos * porcentaje + 50) / 100
    return MontosTasacion(pvpCentimos, porcentaje, galeria, pvpCentimos - galeria)
}

/** Los montos si los dos textos son válidos; si no, null (la pantalla muestra "—"). */
fun montosDe(pvp: String, porcentaje: String): MontosTasacion? {
    val precio = leerPvp(pvp) as? LecturaPvp.Valido ?: return null
    val pct = leerPorcentaje(porcentaje) as? LecturaPorcentaje.Valido ?: return null
    return calcularMontos(precio.centimos, pct.valor)
}

/**
 * 125050 → "S/ 1,250.50"; 0 → "S/ 0.00". Formato peruano (es-PE) con punto decimal y coma de miles,
 * fijados a mano para que salga igual en todos los celulares.
 */
fun textoSoles(centimos: Long): String {
    val simbolos = DecimalFormatSymbols(Locale.forLanguageTag("es-PE")).apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    }
    return "S/ " + DecimalFormat("#,##0.00", simbolos).format(BigDecimal.valueOf(centimos, 2))
}