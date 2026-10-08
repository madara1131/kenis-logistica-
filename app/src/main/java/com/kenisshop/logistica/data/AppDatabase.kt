package com.kenisshop.logistica.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kenisshop.logistica.data.midia.Actividad
import com.kenisshop.logistica.data.midia.Cumplimiento
import com.kenisshop.logistica.data.midia.MiDiaDao
import com.kenisshop.logistica.data.midia.Premio
import com.kenisshop.logistica.data.midia.SQL_SEMILLAS_MI_DIA
import com.kenisshop.logistica.data.midia.SQL_TABLAS_MI_DIA
import com.kenisshop.logistica.data.midia.Temporada

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

/**
 * v3 → v4: tablas nuevas de "Mi Día" (temporadas, actividades, cumplimientos y premios).
 * Solo AGREGA tablas: no toca los pedidos ni ningún dato existente.
 */
val MIGRACION_PEDIDOS_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        SQL_TABLAS_MI_DIA.forEach { db.execSQL(it) }
        SQL_SEMILLAS_MI_DIA.forEach { db.execSQL(it) }
    }
}

/** En una instalación nueva Room crea las tablas; aquí solo se cargan los datos iniciales. */
private val DATOS_INICIALES = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        SQL_SEMILLAS_MI_DIA.forEach { db.execSQL(it) }
    }
}

@Database(
    entities = [Pedido::class, Temporada::class, Actividad::class, Cumplimiento::class, Premio::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pedidoDao(): PedidoDao
    abstract fun miDiaDao(): MiDiaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "logistica.db"
                ).addMigrations(MIGRACION_PEDIDOS_1_2, MIGRACION_PEDIDOS_2_3, MIGRACION_PEDIDOS_3_4)
                    .addCallback(DATOS_INICIALES)
                    .build().also { instancia = it }
            }
    }
}
