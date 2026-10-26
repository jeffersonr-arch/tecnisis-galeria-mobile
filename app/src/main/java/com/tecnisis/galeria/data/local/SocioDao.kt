package com.tecnisis.galeria.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Consultas SQL sobre la tabla "socio". Room genera el código a partir de estas anotaciones. */
@Dao
interface SocioDao {

    /** Flow: cada vez que cambia la fila, la pantalla recibe el valor nuevo. */
    @Query("SELECT * FROM socio WHERE uid = :uid LIMIT 1")
    fun observar(uid: String): Flow<SocioEntity?>

    /** Inserta la fila o, si ya existe ese uid, la actualiza. */
    @Upsert
    suspend fun guardar(socio: SocioEntity)

    @Query("DELETE FROM socio WHERE uid = :uid")
    suspend fun borrar(uid: String)
}