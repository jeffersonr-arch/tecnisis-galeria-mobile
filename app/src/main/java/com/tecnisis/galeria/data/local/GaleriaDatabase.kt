package com.tecnisis.galeria.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Base de datos SQLite de la app (archivo galeria.db). Versión 1: socio (HU05). Versión 2: + solicitud y obra (HU06). */
@Database(
    entities = [SocioEntity::class, SolicitudEntity::class, ObraEntity::class],
    version = 3,
    exportSchema = false
)
abstract class GaleriaDatabase : RoomDatabase() {

    abstract fun socioDao(): SocioDao
    abstract fun obraDao(): ObraDao

    companion object {
        @Volatile
        private var instancia: GaleriaDatabase? = null

        /**
         * HU06: crea las tablas nuevas sin tocar la de socio. Sin esto, la app se cierra en los celulares
         * que ya tienen galeria.db en versión 1. El SQL debe coincidir exactamente con las entidades.
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `solicitud` (`id` TEXT NOT NULL, `uid` TEXT NOT NULL, " +
                            "`estado` TEXT NOT NULL, `creadaEn` INTEGER NOT NULL, `enviadaEn` INTEGER, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `obra` (`id` TEXT NOT NULL, `solicitudId` TEXT NOT NULL, " +
                            "`uid` TEXT NOT NULL, `titulo` TEXT NOT NULL, `tecnica` TEXT NOT NULL, " +
                            "`altoCm` REAL NOT NULL, `anchoCm` REAL NOT NULL, `profundidadCm` REAL, " +
                            "`fotoLocal` TEXT NOT NULL, `fotoUrl` TEXT, `nota` TEXT NOT NULL, " +
                            "`estado` TEXT NOT NULL, `creadaEn` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_obra_solicitudId` ON `obra` (`solicitudId`)")
            }
        }
        /** HU07: dos columnas nuevas en solicitud. Las filas que ya existen quedan con NULL (no resueltas). */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `solicitud` ADD COLUMN `motivoRechazo` TEXT")
                db.execSQL("ALTER TABLE `solicitud` ADD COLUMN `resueltaEn` INTEGER")
            }
        }
        /** Una sola instancia para toda la app (abrir la base de datos es costoso). */
        fun obtener(contexto: Context): GaleriaDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    GaleriaDatabase::class.java,
                    "galeria.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .build()
                    .also { instancia = it }
            }
    }
}