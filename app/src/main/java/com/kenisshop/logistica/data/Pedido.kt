package com.kenisshop.logistica.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kenisshop.logistica.util.Fechas

enum class TipoMercaderia(val etiqueta: String, val diasLimite: Int) {
    AEREA("Aérea", 15),
    MARITIMA("Marítima", 25)
}

enum class EstadoPedido(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    EN_TRANSITO("En tránsito"),
    INGRESADO("Ingresado"),
    VENCIDO("Vencido")
}

val EMPRESAS_ENVIO = listOf("USPS", "BOFO", "FedEx", "UPS", "DHL Express", "Shein")

@Entity(tableName = "pedidos", indices = [Index(value = ["codigo"], unique = true)])
data class Pedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String,
    val tipo: TipoMercaderia,
    val fotoPath: String?,
    val marcaIngreso: String,
    val origen: String,
    val empresaEnvio: String,
    /** Etapa 1: realización del pedido */
    val fechaPedido: Long,
    /** Etapa 2: llegada al casillero en Miami (en tránsito) */
    val fechaMiami: Long? = null,
    /** Etapa 3: ingreso final en nuestras manos */
    val fechaIngreso: Long? = null,
    val creadoEn: Long = System.currentTimeMillis()
) {
    /** Días desde el pedido hasta hoy (o hasta el ingreso si ya ingresó). */
    fun diasTranscurridos(ahora: Long = System.currentTimeMillis()): Long =
        Fechas.diasEntre(fechaPedido, fechaIngreso ?: ahora)

    /** El estado se calcula solo, según las fechas registradas. */
    fun estado(ahora: Long = System.currentTimeMillis()): EstadoPedido = when {
        fechaIngreso != null -> EstadoPedido.INGRESADO
        Fechas.diasEntre(fechaPedido, ahora) >= tipo.diasLimite -> EstadoPedido.VENCIDO
        fechaMiami != null -> EstadoPedido.EN_TRANSITO
        else -> EstadoPedido.PENDIENTE
    }
}
