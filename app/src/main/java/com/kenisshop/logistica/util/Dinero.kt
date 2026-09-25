package com.kenisshop.logistica.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Dinero {
    private val formato = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    fun fmt(v: Double?): String = if (v == null) "—" else "C$ " + formato.format(v)

    /** Versión corta para gráficos: C$ 8.5k */
    fun corto(v: Double): String = when {
        kotlin.math.abs(v) >= 1_000_000 -> "C$ " + DecimalFormat("0.#", DecimalFormatSymbols(Locale.US)).format(v / 1_000_000) + "M"
        kotlin.math.abs(v) >= 1_000 -> "C$ " + DecimalFormat("0.#", DecimalFormatSymbols(Locale.US)).format(v / 1_000) + "k"
        else -> "C$ " + formato.format(v)
    }

    /** Texto para poner en un campo editable (sin separadores de miles). */
    fun editable(v: Double?): String = when {
        v == null -> ""
        v % 1.0 == 0.0 -> v.toLong().toString()
        else -> v.toString()
    }

    fun parsear(texto: String): Double? =
        texto.replace("C$", "").replace(",", "").replace(" ", "").trim().toDoubleOrNull()
}
