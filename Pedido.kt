package com.kenisshop.logistica.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kenisshop.logistica.util.Fechas

/** [tarifaLibra] = dólares que se pagan por cada libra según el tipo de envío. */
enum class TipoMercaderia(val etiqueta: String, val diasLimite: Int, val tarifaLibra: Double) {
    AEREA("Aérea", 15, 5.5),
    MARITIMA("Marítima", 25, 2.0)
}

enum class EstadoPedido(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    EN_TRANSITO("En tránsito"),
    INGRESADO("Ingresado"),
    VENCIDO("Vencido")
}

val EMPRESAS_ENVIO = listOf("USPS", "GOFO", "FedEx", "UPS", "DHL Express", "Shein", "Speedx")

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
    val creadoEn: Long = System.currentTimeMillis(),
    /** Peso total del pedido en libras */
    val pesoLibras: Double? = null,
    /** Dólares a pagar por libra (5.5 aéreo, 2 marítimo por defecto) */
    val tarifaLibra: Double? = null,
    /** Cliente para quien es el pedido (opcional) */
    val cliente: String? = null
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

// ------------------------------------------------ Peso y cobro por libra

/** Calcula lo que se paga: peso × tarifa por libra (redondeado a centavos). */
fun calcularCobro(pesoLibras: Double?, tarifaLibra: Double?): Double? =
    if (pesoLibras == null || tarifaLibra == null) null
    else Math.round(pesoLibras * tarifaLibra * 100.0) / 100.0

val Pedido.tarifaAplicada: Double get() = tarifaLibra ?: tipo.tarifaLibra
val Pedido.totalPagar: Double? get() = calcularCobro(pesoLibras, tarifaAplicada)

data class ResumenPeso(val origen: String, val pedidos: Int, val libras: Double, val dolares: Double, val sinPeso: Int)

/** Libras y dólares por cada tienda/marca de origen, y el total general (origen = "Total"). */
fun resumenPorOrigen(pedidos: List<Pedido>): Pair<ResumenPeso, List<ResumenPeso>> {
    fun resumir(nombre: String, lista: List<Pedido>) = ResumenPeso(
        origen = nombre,
        pedidos = lista.size,
        libras = Math.round(lista.sumOf { it.pesoLibras ?: 0.0 } * 100.0) / 100.0,
        dolares = Math.round(lista.sumOf { it.totalPagar ?: 0.0 } * 100.0) / 100.0,
        sinPeso = lista.count { it.pesoLibras == null }
    )
    val porOrigen = pedidos.groupBy { it.origen.trim().ifEmpty { "Sin origen" } }
        .map { (k, v) -> resumir(k, v) }
        .sortedByDescending { it.dolares }
    return resumir("Total", pedidos) to porOrigen
}
