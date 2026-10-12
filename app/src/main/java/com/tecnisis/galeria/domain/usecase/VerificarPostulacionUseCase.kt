package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.PersonaReniec
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.repository.ConsultaDniRepository
import com.tecnisis.galeria.domain.repository.PostulacionRepository
import java.text.Normalizer

class VerificarPostulacionUseCase(
    private val postulaciones: PostulacionRepository,
    private val reniec: ConsultaDniRepository
) {

    /** Verifica y guarda el resultado. Si no hay red, lanza IOException para que el Worker reintente. */
    suspend operator fun invoke(uid: String): Result<EstadoPostulacion> {
        val p = postulaciones.obtener(uid).getOrElse { return Result.failure(it) }
        if (p.estado != EstadoPostulacion.PENDIENTE_VERIFICACION) return Result.success(p.estado)

        val problemaDeFormato = validarFormato(p)
        val (estado, motivo) = if (problemaDeFormato != null) {
            EstadoPostulacion.OBSERVADA to problemaDeFormato
        } else {
            val persona = reniec.consultar(p.dni)
            when {
                persona == null ->
                    EstadoPostulacion.OBSERVADA to "El DNI ${p.dni} no fue encontrado en RENIEC"
                !coincide(p, persona) ->
                    EstadoPostulacion.OBSERVADA to "Los nombres no coinciden con RENIEC " +
                            "(${persona.nombres} ${persona.apellidoPaterno} ${persona.apellidoMaterno})"
                else -> EstadoPostulacion.VERIFICADA to null
            }
        }
        return postulaciones.registrarVerificacion(uid, estado, motivo).map { estado }
    }

    /** Cuando no se pudo verificar automáticamente, la deja observada para que la revise el Anfitrión. */
    suspend fun enviarARevisionManual(uid: String, motivo: String) =
        postulaciones.registrarVerificacion(uid, EstadoPostulacion.OBSERVADA, motivo)

    private fun validarFormato(p: Postulacion): String? = when {
        !p.dni.matches(Regex("\\d{8}")) -> "El DNI debe tener 8 dígitos"
        !p.telefono.matches(Regex("9\\d{8}")) -> "El teléfono debe tener 9 dígitos y empezar con 9"
        listOf(p.nombres, p.apellidoPaterno, p.direccion, p.sexo, p.correo).any { it.isBlank() } ->
            "Hay datos obligatorios vacíos"
        else -> null
    }

    private fun coincide(p: Postulacion, r: PersonaReniec): Boolean =
        normalizar(p.nombres) == normalizar(r.nombres) &&
                normalizar(p.apellidoPaterno) == normalizar(r.apellidoPaterno) &&
                normalizar(p.apellidoMaterno) == normalizar(r.apellidoMaterno)

    /** Quita tildes, pasa a mayúsculas y une espacios: "José  Pérez" y "JOSE PEREZ" coinciden. */
    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .uppercase()
            .replace(Regex("\\s+"), " ")
            .trim()
}