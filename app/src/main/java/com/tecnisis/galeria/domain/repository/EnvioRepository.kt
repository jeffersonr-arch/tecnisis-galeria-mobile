package com.tecnisis.galeria.domain.repository

/** HU06: sube las solicitudes "Pendiente de envío": fotos a Storage y datos a Firestore. Lo usa el worker. */
interface EnvioRepository {
    suspend fun subirPendientes(): Result<Unit>
}