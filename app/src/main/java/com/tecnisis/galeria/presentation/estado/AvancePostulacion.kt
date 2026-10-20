package com.tecnisis.galeria.presentation.estado

import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.presentation.componentes.EstadoPaso
import com.tecnisis.galeria.presentation.componentes.PasoAvance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM · h:mm a", Locale.forLanguageTag("es-PE"))

fun EstadoPostulacion.pasosCompletos(): Int = when (this) {
    EstadoPostulacion.PENDIENTE_VERIFICACION -> 1
    EstadoPostulacion.VERIFICADA, EstadoPostulacion.OBSERVADA -> 2
    EstadoPostulacion.RECHAZADA -> 3
    EstadoPostulacion.APROBADA -> 4
}

fun EstadoPostulacion.resumen(): String = when (this) {
    EstadoPostulacion.PENDIENTE_VERIFICACION -> "Paso 1 de 4 · Verificando tus datos con RENIEC"
    EstadoPostulacion.VERIFICADA -> "Paso 2 de 4 · Falta la aprobación del Anfitrión"
    EstadoPostulacion.OBSERVADA -> "Paso 2 de 4 · El Anfitrión revisará una observación"
    EstadoPostulacion.APROBADA -> "¡Listo! Ya eres Socio Activo"
    EstadoPostulacion.RECHAZADA -> "Tu postulación no fue aprobada"
}

fun Postulacion.pasosDeAvance(): List<PasoAvance> {
    val enviada = PasoAvance(
        "Postulación enviada",
        fechaRegistro?.let { formatoFecha.format(Date(it)) } ?: "Enviada",
        EstadoPaso.COMPLETO
    )
    val verificacion = when (estado) {
        EstadoPostulacion.PENDIENTE_VERIFICACION ->
            PasoAvance("Verificación con RENIEC", "En curso", EstadoPaso.ACTUAL)
        EstadoPostulacion.OBSERVADA ->
            PasoAvance("Verificación con RENIEC", "Con observación", EstadoPaso.COMPLETO)
        else -> PasoAvance("Verificación con RENIEC", "Datos revisados", EstadoPaso.COMPLETO)
    }
    val aprobacion = when (estado) {
        EstadoPostulacion.PENDIENTE_VERIFICACION ->
            PasoAvance("Aprobación del Anfitrión", "Pendiente", EstadoPaso.PENDIENTE)
        EstadoPostulacion.VERIFICADA, EstadoPostulacion.OBSERVADA ->
            PasoAvance("Aprobación del Anfitrión", "En revisión", EstadoPaso.ACTUAL)
        EstadoPostulacion.APROBADA ->
            PasoAvance("Aprobación del Anfitrión", "Aprobada", EstadoPaso.COMPLETO)
        EstadoPostulacion.RECHAZADA ->
            PasoAvance("Aprobación del Anfitrión", "No aprobada", EstadoPaso.COMPLETO)
    }
    val socio = when (estado) {
        EstadoPostulacion.APROBADA -> PasoAvance("Socio activo", "¡Bienvenido!", EstadoPaso.COMPLETO)
        EstadoPostulacion.RECHAZADA -> PasoAvance("Socio activo", "No aplica", EstadoPaso.PENDIENTE)
        else -> PasoAvance("Socio activo", "Pendiente", EstadoPaso.PENDIENTE)
    }
    return listOf(enviada, verificacion, aprobacion, socio)
}