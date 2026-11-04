package com.tecnisis.galeria.domain.repository

/** HU06: programa con WorkManager el envío de las solicitudes pendientes (corre cuando hay internet). */
fun interface EnvioSolicitudScheduler {
    fun programar()
}