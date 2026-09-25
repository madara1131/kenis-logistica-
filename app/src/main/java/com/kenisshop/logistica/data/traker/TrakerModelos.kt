package com.kenisshop.logistica.data.traker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SeccionTraker(val etiqueta: String, val emoji: String) {
    PERSONAL("Gastos personales", "👤"),
    KENISSHOP("Kenisshop", "🛍️"),
    NECESARIOS("Gastos necesarios", "🧾")
}

enum class TipoLista(val etiqueta: String, val emoji: String, val conMonto: Boolean, val etiquetaNombre: String) {
    DEUDA("Deudas", "💳", true, "Nombre"),
    GASTO_EXTRA("Gastos extras", "⚠️", true, "Nombre"),
    AHORRO("Ahorros", "🐷", true, "Mes"),
    DIEZMO("Diezmo", "🙏", true, "Mes"),
    NOTA("Notas", "📝", false, "Nota")
}

enum class EstadoGasto(val etiqueta: String) {
    SIN_GASTO("Sin gasto"),
    DENTRO("Dentro del presupuesto"),
    CERCA("Cerca del límite"),
    EXCEDIDO("Excedido")
}

/** Una fila de presupuesto: categoría de un mes y sección (lo que en el Excel era una fila de la tabla). */
@Entity(
    tableName = "traker_categorias",
    indices = [Index(value = ["mes", "seccion", "categoria"], unique = true)]
)
data class GastoCategoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Formato yyyy-MM */
    val mes: String,
    val seccion: SeccionTraker,
    val categoria: String,
    val presupuesto: Double?,
    val gastoReal: Double?,
    val actualizado: Long = System.currentTimeMillis()
)

// Cálculos fuera de la entidad (Room solo guarda los campos de arriba)
val GastoCategoria.real: Double get() = gastoReal ?: 0.0
val GastoCategoria.plan: Double get() = presupuesto ?: 0.0
val GastoCategoria.diferencia: Double get() = plan - real

/** Clave para comparar entre importaciones. */
val GastoCategoria.clave: String get() = "$mes|${seccion.name}|${categoria.lowercase()}"

fun GastoCategoria.estado(): EstadoGasto = when {
    real <= 0.0 -> EstadoGasto.SIN_GASTO
    plan <= 0.0 -> EstadoGasto.EXCEDIDO
    real > plan -> EstadoGasto.EXCEDIDO
    real >= plan * 0.9 -> EstadoGasto.CERCA
    else -> EstadoGasto.DENTRO
}

@Entity(tableName = "traker_capital", primaryKeys = ["mes", "seccion"])
data class CapitalMes(
    val mes: String,
    val seccion: SeccionTraker,
    val capital: Double?
)

@Entity(tableName = "traker_listas")
data class ItemLista(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lista: TipoLista,
    val nombre: String,
    val monto: Double?,
    val orden: Int = 0,
    val actualizado: Long = System.currentTimeMillis()
)

/** Foto completa de los datos del Traker. */
data class DatosTraker(
    val categorias: List<GastoCategoria>,
    val capitales: List<CapitalMes>,
    val listas: List<ItemLista>
) {
    val vacio: Boolean get() = categorias.isEmpty() && capitales.isEmpty() && listas.isEmpty()

    val meses: List<String> by lazy {
        (categorias.map { it.mes } + capitales.map { it.mes }).distinct().sorted()
    }

    fun capital(mes: String, seccion: SeccionTraker): Double? =
        capitales.firstOrNull { it.mes == mes && it.seccion == seccion }?.capital

    fun delMes(mes: String): List<GastoCategoria> = categorias.filter { it.mes == mes }

    fun secciones(mes: String): List<SeccionTraker> =
        SeccionTraker.entries.filter { s ->
            categorias.any { it.mes == mes && it.seccion == s } || capital(mes, s) != null
        }
}
