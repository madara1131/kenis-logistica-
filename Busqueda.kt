package com.kenisshop.logistica.data

import java.text.Normalizer

enum class OrdenBusqueda(val etiqueta: String) {
    FECHA("Fecha"),
    PESO("Peso"),
    COSTO("Costo"),
    ESTADO("Estado")
}

/** Criterios de la pestaña "Buscar pedidos". Se guardan para volver a ver los mismos resultados. */
data class FiltroBusqueda(
    /** Busca en cliente, código, origen/marca y empresa */
    val texto: String = "",
    val tipo: TipoMercaderia? = null,
    val desde: Long? = null,
    val hasta: Long? = null,
    val empresa: String? = null,
    val estado: EstadoPedido? = null,
    val pesoMin: Double? = null,
    val pesoMax: Double? = null,
    val soloSinPeso: Boolean = false,
    val orden: OrdenBusqueda = OrdenBusqueda.FECHA,
    val descendente: Boolean = true
) {
    /** Cuántos criterios están activos (el orden no cuenta). */
    val criterios: Int
        get() = listOf(
            texto.isNotBlank(), tipo != null, desde != null || hasta != null, empresa != null,
            estado != null, pesoMin != null || pesoMax != null, soloSinPeso
        ).count { it }

    val activo: Boolean get() = criterios > 0
}

private fun sinTildes(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().trim()

/** Prioridad para ordenar por estado: lo urgente primero. */
private fun prioridad(e: EstadoPedido): Int = when (e) {
    EstadoPedido.VENCIDO -> 0
    EstadoPedido.PENDIENTE -> 1
    EstadoPedido.EN_TRANSITO -> 2
    EstadoPedido.INGRESADO -> 3
}

fun buscarPedidos(pedidos: List<Pedido>, f: FiltroBusqueda, ahora: Long = System.currentTimeMillis()): List<Pedido> {
    val q = sinTildes(f.texto)
    val filtrados = pedidos.filter { p ->
        (q.isEmpty() || listOfNotNull(p.cliente, p.codigo, p.origen, p.empresaEnvio).any { sinTildes(it).contains(q) }) &&
            (f.tipo == null || p.tipo == f.tipo) &&
            (f.desde == null || p.fechaPedido >= f.desde) &&
            (f.hasta == null || p.fechaPedido <= f.hasta) &&
            (f.empresa == null || p.empresaEnvio.equals(f.empresa, ignoreCase = true)) &&
            (f.estado == null || p.estado(ahora) == f.estado) &&
            (!f.soloSinPeso || p.pesoLibras == null) &&
            (f.pesoMin == null || (p.pesoLibras != null && p.pesoLibras >= f.pesoMin)) &&
            (f.pesoMax == null || (p.pesoLibras != null && p.pesoLibras <= f.pesoMax))
    }
    // Los que no tienen el dato (peso o costo) siempre quedan al final
    val porFecha = compareBy<Pedido> { it.fechaPedido }.thenBy { it.id }
    val comparador: Comparator<Pedido> = when (f.orden) {
        OrdenBusqueda.FECHA -> if (f.descendente) porFecha.reversed() else porFecha
        OrdenBusqueda.PESO -> {
            val c = compareBy<Pedido> { it.pesoLibras ?: 0.0 }
            compareBy<Pedido> { it.pesoLibras == null }.then(if (f.descendente) c.reversed() else c)
        }
        OrdenBusqueda.COSTO -> {
            val c = compareBy<Pedido> { it.totalPagar ?: 0.0 }
            compareBy<Pedido> { it.totalPagar == null }.then(if (f.descendente) c.reversed() else c)
        }
        OrdenBusqueda.ESTADO -> {
            val c = compareBy<Pedido> { prioridad(it.estado(ahora)) }
            (if (f.descendente) c else c.reversed()).then(porFecha.reversed())
        }
    }
    return filtrados.sortedWith(comparador)
}
