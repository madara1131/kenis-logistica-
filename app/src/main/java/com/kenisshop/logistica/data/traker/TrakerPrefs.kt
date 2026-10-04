package com.kenisshop.logistica.data.traker

import android.content.Context

/** Guarda en el teléfono los límites, la meta de ahorro y las páginas de la libreta. */
class TrakerPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("traker_ajustes", Context.MODE_PRIVATE)

    private fun leerDouble(k: String): Double? =
        if (prefs.contains(k)) prefs.getString(k, null)?.toDoubleOrNull() else null

    fun leer(): AjustesTraker = AjustesTraker(
        limiteDeudas = leerDouble("limite_deudas"),
        limiteExtras = leerDouble("limite_extras"),
        metaAhorro = leerDouble("meta_ahorro"),
        paginas = (listOf("General") + (prefs.getString("paginas", "") ?: "")
            .split("\n").map { it.trim() }.filter { it.isNotEmpty() }).distinct()
    )

    fun guardar(a: AjustesTraker) {
        prefs.edit().apply {
            if (a.limiteDeudas == null) remove("limite_deudas") else putString("limite_deudas", a.limiteDeudas.toString())
            if (a.limiteExtras == null) remove("limite_extras") else putString("limite_extras", a.limiteExtras.toString())
            if (a.metaAhorro == null) remove("meta_ahorro") else putString("meta_ahorro", a.metaAhorro.toString())
            putString("paginas", a.paginas.filter { it != "General" }.joinToString("\n"))
        }.apply()
    }
}
