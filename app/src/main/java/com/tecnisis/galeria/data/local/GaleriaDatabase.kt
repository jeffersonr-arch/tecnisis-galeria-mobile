package com.tecnisis.galeria.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base de datos SQLite de la app (archivo galeria.db). En el Sprint 3 se le agregará la tabla de borradores de obras. */
@Database(entities = [SocioEntity::class], version = 1, exportSchema = false)
abstract class GaleriaDatabase : RoomDatabase() {

    abstract fun socioDao(): SocioDao

    companion object {
        @Volatile
        private var instancia: GaleriaDatabase? = null

        /** Una sola instancia para toda la app (abrir la base de datos es costoso). */
        fun obtener(contexto: Context): GaleriaDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    GaleriaDatabase::class.java,
                    "galeria.db"
                ).build().also { instancia = it }
            }
    }
}