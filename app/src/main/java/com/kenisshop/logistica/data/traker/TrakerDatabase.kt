package com.kenisshop.logistica.data.traker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base de datos aparte para el Traker: así los pedidos existentes no se tocan. */
@Database(
    entities = [GastoCategoria::class, CapitalMes::class, ItemLista::class],
    version = 1,
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
                ).build().also { instancia = it }
            }
    }
}
