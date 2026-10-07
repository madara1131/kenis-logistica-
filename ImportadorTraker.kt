package com.kenisshop.logistica.util

import com.kenisshop.logistica.data.traker.CapitalMes
import com.kenisshop.logistica.data.traker.GastoCategoria
import com.kenisshop.logistica.data.traker.ItemLista
import com.kenisshop.logistica.data.traker.SeccionTraker
import com.kenisshop.logistica.data.traker.TipoLista

/**
 * Interpreta el Excel "TRAKER DE GASTO" aunque sea una hoja libre (no una tabla):
 * busca los bloques por sus títulos (mes, GASTOS PERSONALES, KENISSHOP, GASTOS NECESARIOS,
 * DEUDAS, GASTOS EXTRAS, AHORROS, DIEZMO, NOTAS) y reconstruye los datos.
 */
object ImportadorTraker {

    data class Resultado(
        val categorias: List<GastoCategoria>,
        val capitales: List<CapitalMes>,
        val items: List<ItemLista>
    ) {
        val meses: List<String> get() = (categorias.map { it.mes } + capitales.map { it.mes }).distinct().sorted()
        val vacio: Boolean get() = categorias.isEmpty() && items.isEmpty()
    }

    private val TITULOS = setOf(
        "GASTOS PERSONALES", "KENISSHOP", "KENIS SHOP", "KENI'S SHOP", "GASTOS NECESARIOS",
        "DEUDAS", "GASTOS EXTRAS", "AHORROS", "DIEZMO", "NOTAS", "GASTOS MENSUAL", "GASTOS MENSUALES"
    )

    private fun norm(s: String?): String = if (s == null) "" else Meses.normalizar(s)

    private fun limpiar(s: String): String = s.replace(Regex("\\s+"), " ").trim()

    private fun capitalizar(s: String): String =
        limpiar(s).lowercase().replaceFirstChar { it.uppercase() }

    private fun seccionDe(t: String): SeccionTraker? = when (t) {
        "GASTOS PERSONALES" -> SeccionTraker.PERSONAL
        "KENISSHOP", "KENIS SHOP", "KENI'S SHOP" -> SeccionTraker.KENISSHOP
        else -> null
    }

    private fun listaDe(t: String): TipoLista? = when (t) {
        "DEUDAS" -> TipoLista.DEUDA
        "GASTOS EXTRAS" -> TipoLista.GASTO_EXTRA
        "AHORROS" -> TipoLista.AHORRO
        "DIEZMO" -> TipoLista.DIEZMO
        else -> null
    }

    fun interpretar(hoja: LectorXlsx.Hoja, anioInicial: Int): Resultado {
        val categorias = ArrayList<GastoCategoria>()
        val capitales = ArrayList<CapitalMes>()
        val items = ArrayList<ItemLista>()

        // 1) Encontrar bloques de sección con su mes
        data class Bloque(val fila: Int, val col: Int, val seccion: SeccionTraker?, val mes: Int?)

        val bloques = ArrayList<Bloque>()
        for (c in hoja.todas) {
            val t = norm(c.texto)
            val sec = seccionDe(t)
            if (sec != null) {
                // el mes está justo arriba (misma columna)
                var mes: Int? = null
                for (d in 1..3) {
                    mes = Meses.desdeTexto(hoja.texto(c.fila - d, c.col))
                    if (mes != null) break
                }
                bloques.add(Bloque(c.fila, c.col, sec, mes))
            } else if (t == "GASTOS NECESARIOS") {
                // el mes está en la misma fila (a la izquierda)
                var mes: Int? = null
                for (col in 0 until c.col) {
                    mes = Meses.desdeTexto(hoja.texto(c.fila, col))
                    if (mes != null) break
                }
                if (mes == null) for (d in 1..3) {
                    mes = Meses.desdeTexto(hoja.texto(c.fila - d, c.col))
                    if (mes != null) break
                }
                bloques.add(Bloque(c.fila, c.col, null, mes))
            }
        }

        // 2) Asignar años: si el mes "retrocede" (Dic → Ene) se pasa al año siguiente
        val filasMes = bloques.filter { it.mes != null }.map { it.fila to it.mes!! }.distinctBy { it.first }.sortedBy { it.first }
        val anioPorFila = HashMap<Int, Int>()
        var anio = anioInicial
        var mesAnterior = 0
        for ((fila, mes) in filasMes) {
            if (mes < mesAnterior) anio++
            mesAnterior = mes
            anioPorFila[fila] = anio
        }
        val primerMes = filasMes.firstOrNull()?.let { Meses.clave(anioInicial, it.second) } ?: Meses.actual()
        fun claveMes(b: Bloque): String =
            if (b.mes == null) primerMes else Meses.clave(anioPorFila[b.fila] ?: anioInicial, b.mes)

        // 3) Leer cada bloque de categorías
        for (b in bloques.sortedWith(compareBy({ it.fila }, { it.col }))) {
            val mes = claveMes(b)
            val seccion = b.seccion ?: SeccionTraker.NECESARIOS
            val filaEnc = (b.fila..b.fila + 3).firstOrNull { norm(hoja.texto(it, b.col)).startsWith("CATEGORIA") } ?: continue
            // columnas de presupuesto y gasto real según el encabezado
            var colPres = b.col + 1
            var colReal = b.col + 2
            for (cc in b.col + 1..b.col + 5) {
                val h = norm(hoja.texto(filaEnc, cc))
                if (h.startsWith("PRESUPUESTO")) colPres = cc
                if (h.startsWith("GASTO REAL")) colReal = cc
            }
            var fila = filaEnc + 1
            var filaTotal = -1
            while (fila <= filaEnc + 25) {
                val crudo = hoja.texto(fila, b.col)
                val t = norm(crudo)
                if (t == "TOTAL") { filaTotal = fila; break }
                if (t in TITULOS || Meses.desdeTexto(crudo) != null) break
                if (crudo != null && t.isNotEmpty()) {
                    categorias.add(
                        GastoCategoria(
                            mes = mes,
                            seccion = seccion,
                            categoria = limpiar(crudo),
                            presupuesto = hoja.numero(fila, colPres),
                            gastoReal = hoja.numero(fila, colReal)
                        )
                    )
                }
                fila++
            }
            // capital
            val desde = if (filaTotal > 0) filaTotal else fila
            for (f in desde..desde + 3) {
                val t = norm(hoja.texto(f, b.col))
                if (t.startsWith("CAPITAL")) {
                    val valor = (b.col + 1..b.col + 3).firstNotNullOfOrNull { hoja.celda(f, it)?.numero }
                    capitales.add(CapitalMes(mes, seccion, valor))
                    break
                }
            }
        }

        // 4) Listas: deudas, gastos extras, ahorros, diezmo
        for (c in hoja.todas) {
            val t = norm(c.texto)
            val tipo = listaDe(t) ?: continue
            val filaEnc = (c.fila + 1..c.fila + 2).firstOrNull {
                val h = norm(hoja.texto(it, c.col)); h.startsWith("NOMBRE") || h.startsWith("MES")
            } ?: c.fila
            var orden = 0
            for (f in filaEnc + 1..filaEnc + 40) {
                val crudo = hoja.texto(f, c.col)
                val n = norm(crudo)
                if (n == "TOTAL" || (n in TITULOS)) break
                if (crudo == null || n.isEmpty()) continue
                val monto = hoja.celda(f, c.col + 1)?.numero
                if ((tipo == TipoLista.AHORRO || tipo == TipoLista.DIEZMO) && monto == null) continue
                val numMes = Meses.desdeTexto(crudo)
                val nombre = if (numMes != null) Meses.nombre(numMes) else capitalizar(crudo)
                items.add(ItemLista(lista = tipo, nombre = nombre, monto = monto, orden = orden++))
            }
        }

        // 5) Notas
        for (c in hoja.todas) {
            if (norm(c.texto) != "NOTAS") continue
            var orden = 0
            for (f in c.fila + 1..c.fila + 60) {
                val crudo = hoja.texto(f, c.col)
                val n = norm(crudo)
                if (n in TITULOS) break
                if (crudo == null) continue
                val texto = limpiar(crudo.replace(Regex("^\\s*\\d+\\s*[-.)]\\s*"), ""))
                if (texto.isNotEmpty()) items.add(ItemLista(lista = TipoLista.NOTA, nombre = texto, monto = null, orden = orden++))
            }
        }

        return Resultado(
            categorias = categorias.distinctBy { "${it.mes}|${it.seccion}|${it.categoria.lowercase()}" },
            capitales = capitales.distinctBy { "${it.mes}|${it.seccion}" },
            items = items
        )
    }
}
