package com.tecnisis.galeria.domain.repository

/** HU07: trae a Room (SQLite) lo que pasó con las solicitudes del socio en Firestore. */
interface SeguimientoRepository {
    /** Actualiza en Room las solicitudes resueltas y agrega las que falten (por ejemplo, si cambió de celular). */
    suspend fun sincronizar(): Result<Unit>

    /** Programa la sincronización con WorkManager: apenas haya internet. */
    fun programarSincronizacion()
}