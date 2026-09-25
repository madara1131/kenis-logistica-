package com.kenisshop.logistica.util

import com.kenisshop.logistica.data.traker.DatosTraker
import com.kenisshop.logistica.data.traker.EstadoGasto
import com.kenisshop.logistica.data.traker.GastoCategoria
import com.kenisshop.logistica.data.traker.clave
import com.kenisshop.logistica.data.traker.estado
import com.kenisshop.logistica.data.traker.plan
import com.kenisshop.logistica.data.traker.real

/** Detecta situaciones importantes y cambios entre dos versiones de los datos. */
object AnalizadorTraker {

    data class Alerta(val clave: String, val texto: String, val grave: Boolean)

    fun alertas(d: DatosTraker): List<Alerta> {
        val lista = ArrayList<Alerta>()
        for (c in d.categorias) {
            val donde = "${c.seccion.etiqueta} · ${Meses.etiqueta(c.mes)}"
            when (c.estado()) {
                EstadoGasto.EXCEDIDO -> lista.add(
                    Alerta(
                        "EXC|${c.clave}",
                        if (c.plan > 0) "🔴 ${c.categoria} ($donde): gastó ${Dinero.fmt(c.real)} de ${Dinero.fmt(c.plan)}"
                        else "🔴 ${c.categoria} ($donde): gasto de ${Dinero.fmt(c.real)} sin presupuesto",
                        true
                    )
                )
                EstadoGasto.CERCA -> lista.add(
                    Alerta(
                        "CER|${c.clave}",
                        "🟡 ${c.categoria} ($donde): va en ${(c.real / c.plan * 100).toInt()}% del presupuesto",
                        false
                    )
                )
                else -> Unit
            }
        }
        for (mes in d.meses) {
            for (s in d.secciones(mes)) {
                val capital = d.capital(mes, s) ?: continue
                val gasto = d.categorias.filter { it.mes == mes && it.seccion == s }.sumOf { it.real }
                if (gasto > capital) {
                    lista.add(
                        Alerta(
                            "SAL|$mes|${s.name}",
                            "⚠️ Saldo negativo en ${s.etiqueta} (${Meses.etiqueta(mes)}): ${Dinero.fmt(capital - gasto)}",
                            true
                        )
                    )
                }
            }
        }
        return lista
    }

    /** Alertas que aparecen en [despues] y no estaban en [antes]. */
    fun nuevas(antes: DatosTraker, despues: DatosTraker): List<Alerta> {
        val previas = alertas(antes).map { it.clave }.toHashSet()
        return alertas(despues).filter { it.clave !in previas }
    }

    fun cambios(antes: List<GastoCategoria>, despues: List<GastoCategoria>): List<String> {
        val a = antes.associateBy { it.clave }
        val d = despues.associateBy { it.clave }
        val salida = ArrayList<String>()
        for (n in despues) {
            val v = a[n.clave]
            val donde = "${n.seccion.etiqueta}, ${Meses.etiqueta(n.mes)}"
            if (v == null) {
                salida.add("➕ Nueva: ${n.categoria} ($donde)")
            } else {
                if (v.gastoReal != n.gastoReal) {
                    salida.add("✏️ ${n.categoria} ($donde): gasto ${Dinero.fmt(v.gastoReal)} → ${Dinero.fmt(n.gastoReal)}")
                }
                if (v.presupuesto != n.presupuesto) {
                    salida.add("✏️ ${n.categoria} ($donde): presupuesto ${Dinero.fmt(v.presupuesto)} → ${Dinero.fmt(n.presupuesto)}")
                }
            }
        }
        for (v in antes) {
            if (v.clave !in d) salida.add("➖ Ya no está: ${v.categoria} (${v.seccion.etiqueta}, ${Meses.etiqueta(v.mes)})")
        }
        return salida
    }
}
