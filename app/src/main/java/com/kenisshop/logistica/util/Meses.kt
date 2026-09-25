package com.kenisshop.logistica.util

import java.text.Normalizer
import java.time.YearMonth

object Meses {
    private val nombres = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    fun clave(anio: Int, mes: Int): String = "%04d-%02d".format(anio, mes)

    fun actual(): String = YearMonth.now().let { clave(it.year, it.monthValue) }

    fun siguiente(clave: String): String = YearMonth.parse(clave).plusMonths(1).let { clave(it.year, it.monthValue) }

    fun nombre(mes: Int): String = nombres[(mes - 1).coerceIn(0, 11)]

    /** "2026-07" → "Julio 2026" */
    fun etiqueta(clave: String): String = try {
        val ym = YearMonth.parse(clave)
        "${nombres[ym.monthValue - 1]} ${ym.year}"
    } catch (e: Exception) {
        clave
    }

    /** "2026-07" → "Jul" */
    fun corto(clave: String): String = try {
        nombres[YearMonth.parse(clave).monthValue - 1].take(3)
    } catch (e: Exception) {
        clave
    }

    /** "2026-07" → "Jul 26" */
    fun cortoConAnio(clave: String): String = try {
        val ym = YearMonth.parse(clave)
        "${nombres[ym.monthValue - 1].take(3)} ${ym.year % 100}"
    } catch (e: Exception) {
        clave
    }

    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .uppercase()
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Reconoce el mes aunque tenga errores comunes (OBCTUBRE, SETIEMBRE, espacios). */
    fun desdeTexto(texto: String?): Int? {
        if (texto == null) return null
        val t = normalizar(texto).replace(Regex("[^A-Z]"), "")
        return when (t) {
            "ENERO" -> 1
            "FEBRERO" -> 2
            "MARZO" -> 3
            "ABRIL" -> 4
            "MAYO" -> 5
            "JUNIO" -> 6
            "JULIO" -> 7
            "AGOSTO" -> 8
            "SEPTIEMBRE", "SETIEMBRE", "SEPTIMEBRE" -> 9
            "OCTUBRE", "OBCTUBRE", "OTUBRE", "OCTUBRES" -> 10
            "NOVIEMBRE" -> 11
            "DICIEMBRE" -> 12
            else -> null
        }
    }
}
