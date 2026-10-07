package com.kenisshop.logistica.data.midia

import android.content.Context

/** Ajustes del calendario de temporadas y registro de avisos ya enviados (todo local). */
class MiDiaPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("mi_dia", Context.MODE_PRIVATE)

    fun ajustes(): AjustesTemporadas = AjustesTemporadas(
        diasAerea = prefs.getInt("dias_aerea", 15),
        diasMaritima = prefs.getInt("dias_maritima", 25)
    )

    fun guardarAjustes(a: AjustesTemporadas) {
        prefs.edit().putInt("dias_aerea", a.diasAerea).putInt("dias_maritima", a.diasMaritima).apply()
    }

    fun avisosEnviados(): Set<String> = prefs.getStringSet("avisos", emptySet())?.toSet() ?: emptySet()

    /** Guarda las claves nuevas y limpia las de temporadas que ya pasaron hace más de un año. */
    fun marcarEnviados(claves: Collection<String>, anioActual: Int) {
        val todas = (avisosEnviados() + claves).filter { k ->
            val anio = k.substringAfter('|', "").take(4).toIntOrNull()
            anio == null || anio >= anioActual - 1
        }.toSet()
        prefs.edit().putStringSet("avisos", todas).apply()
    }
}
