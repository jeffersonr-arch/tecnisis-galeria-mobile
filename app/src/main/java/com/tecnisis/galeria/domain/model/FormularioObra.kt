package com.tecnisis.galeria.domain.model

import kotlin.math.roundToLong

const val TITULO_MAXIMO = 80
const val NOTA_MAXIMA = 1000
const val MEDIDA_MAXIMA_CM = 1000.0

/** Campos del formulario que pueden tener error (la pantalla pinta en rojo los que vengan aquí). */
enum class CampoObra { TITULO, TECNICA, ALTO, ANCHO, PROFUNDIDAD, FOTO, NOTA }

/** Lo que escribe el socio, tal cual: las medidas todavía son texto. */
data class FormularioObra(
    val id: String? = null,          // null = obra nueva
    val titulo: String = "",
    val tecnica: Tecnica? = null,
    val alto: String = "",
    val ancho: String = "",
    val profundidad: String = "",
    val fotoLocal: String? = null,
    val nota: String = ""
) {
    /** Todos los campos con error a la vez, para marcarlos juntos. Vacío = se puede guardar. */
    fun camposInvalidos(): Set<CampoObra> = buildSet {
        val tituloLimpio = titulo.trim()
        if (tituloLimpio.isEmpty() || tituloLimpio.length > TITULO_MAXIMO) add(CampoObra.TITULO)
        if (tecnica == null) add(CampoObra.TECNICA)
        if (leerMedida(alto) == null) add(CampoObra.ALTO)
        if (leerMedida(ancho) == null) add(CampoObra.ANCHO)
        if (profundidad.isNotBlank() && leerMedida(profundidad) == null) add(CampoObra.PROFUNDIDAD)
        if (fotoLocal.isNullOrBlank()) add(CampoObra.FOTO)
        if (nota.trim().length > NOTA_MAXIMA) add(CampoObra.NOTA)
    }

    /** Solo se llama cuando camposInvalidos() está vacío. */
    fun aDatos(): DatosObra = DatosObra(
        titulo = titulo.trim(),
        tecnica = requireNotNull(tecnica),
        altoCm = requireNotNull(leerMedida(alto)),
        anchoCm = requireNotNull(leerMedida(ancho)),
        profundidadCm = if (profundidad.isBlank()) null else leerMedida(profundidad),
        fotoLocal = requireNotNull(fotoLocal),
        nota = nota.trim()
    )
}

/** Ya validado y convertido: lo que se guarda en Room. */
data class DatosObra(
    val titulo: String,
    val tecnica: Tecnica,
    val altoCm: Double,
    val anchoCm: Double,
    val profundidadCm: Double?,
    val fotoLocal: String,
    val nota: String
)

/** "60,25" → 60.3. Devuelve null si no es un número mayor que 0 y hasta 1000 después de redondear a 1 decimal. */
fun leerMedida(texto: String): Double? {
    val numero = texto.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (!numero.isFinite()) return null // "NaN" o "Infinity" también se convierten a Double
    val redondeado = (numero * 10).roundToLong() / 10.0
    return redondeado.takeIf { it > 0 && it <= MEDIDA_MAXIMA_CM }
}