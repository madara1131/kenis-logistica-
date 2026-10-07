package com.kenisshop.logistica.data.traker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2: columnas nuevas para deudas, gastos extras y la libreta. Conserva todos los datos. */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE traker_listas ADD COLUMN pagado REAL")
        db.execSQL("ALTER TABLE traker_listas ADD COLUMN fecha INTEGER")
        db.execSQL("ALTER TABLE traker_listas ADD COLUMN pagina TEXT")
        db.execSQL("ALTER TABLE traker_listas ADD COLUMN etiquetas TEXT")
        db.execSQL("ALTER TABLE traker_listas ADD COLUMN recordatorio INTEGER")
        db.execSQL("ALTER TABLE traker_listas ADD COLUMN fijada INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE traker_listas ADD COLUMN color INTEGER")
    }
}

/** Base de datos aparte para el Traker: así los pedidos existentes no se tocan. */
@Database(
    entities = [GastoCategoria::class, CapitalMes::class, ItemLista::class],
    version = 2,
    exportSchema = false
)
abstract class TrakerDatabase : RoomDatabase() {
    abstract fun dao(): TrakerDao

    companion object {
        @Volatile
        private var instancia: TrakerDatabase? = null

        fun get(context: Context): TrakerDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrakerDatabase::class.java,
                    "traker.db"
                ).addMigrations(MIGRACION_1_2).build().also { instancia = it }
            }
    }
}
