package com.kenisshop.logistica.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2: peso y tarifa por libra; la empresa BOFO pasa a llamarse GOFO. Conserva todos los pedidos. */
val MIGRACION_PEDIDOS_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pedidos ADD COLUMN pesoLibras REAL")
        db.execSQL("ALTER TABLE pedidos ADD COLUMN tarifaLibra REAL")
        db.execSQL("UPDATE pedidos SET empresaEnvio = 'GOFO' WHERE UPPER(empresaEnvio) = 'BOFO'")
    }
}

/** v2 → v3: campo opcional "cliente" para buscar pedidos por cliente. */
val MIGRACION_PEDIDOS_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pedidos ADD COLUMN cliente TEXT")
    }
}

@Database(entities = [Pedido::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pedidoDao(): PedidoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "logistica.db"
                ).addMigrations(MIGRACION_PEDIDOS_1_2, MIGRACION_PEDIDOS_2_3).build().also { instancia = it }
            }
    }
}
