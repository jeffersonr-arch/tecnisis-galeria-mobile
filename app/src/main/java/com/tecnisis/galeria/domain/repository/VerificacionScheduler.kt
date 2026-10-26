package com.tecnisis.galeria.domain.repository

/** Programa la verificación automática de una postulación (HU03). */
fun interface VerificacionScheduler {
    fun programar(uid: String)
}