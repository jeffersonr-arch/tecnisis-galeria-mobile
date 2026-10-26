package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.Socio
import kotlinx.coroutines.flow.Flow

interface SocioRepository {
    /** Lee la ficha desde el celular (Room): funciona sin internet. Emite null si aún no se descargó. */
    fun observarMiPerfil(): Flow<Socio?>
    /** Trae socios/{uid} de Firestore y lo guarda en el celular. */
    suspend fun sincronizar(): Result<Unit>
    /** Programa la sincronización con WorkManager: apenas haya internet y luego cada 6 horas. */
    fun programarSincronizacion()
    /** Al cerrar sesión: borra la ficha del celular y cancela la sincronización. */
    fun borrarPerfilLocal()
}