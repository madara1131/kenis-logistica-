package com.kenisshop.logistica.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Formato en dólares para el cobro por libra. */
object Dolares {
    private val simbolos = DecimalFormatSymbols(Locale.US)
    private val dinero = DecimalFormat("#,##0.00", simbolos)
    private val num = DecimalFormat("#,##0.##", simbolos)

    fun fmt(v: Double?): String = if (v == null) "—" else "US$ " + dinero.format(v)
    fun numero(v: Double): String = num.format(v)
    fun libras(v: Double?): String = if (v == null) "—" else num.format(v) + " lb"
}

/** Texto de un campo → número (acepta 12.5, 12,5 y 1,250.75). */
fun numeroDe(texto: String): Double? =
    texto.trim().replace(" ", "").let { t ->
        if (t.count { it == ',' } == 1 && !t.contains('.')) t.replace(',', '.') else t.replace(",", "")
    }.toDoubleOrNull()
