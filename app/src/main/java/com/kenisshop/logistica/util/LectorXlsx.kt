package com.kenisshop.logistica.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Lector mínimo de archivos .xlsx (sin librerías): devuelve los valores de la primera hoja. */
object LectorXlsx {

    data class Celda(val fila: Int, val col: Int, val texto: String?, val numero: Double?)

    class Hoja(val celdas: Map<Long, Celda>) {
        private fun k(f: Int, c: Int): Long = f.toLong() * 100_000L + c
        fun celda(fila: Int, col: Int): Celda? = celdas[k(fila, col)]
        fun texto(fila: Int, col: Int): String? = celda(fila, col)?.let { it.texto ?: it.numero?.let { n -> Dinero.editable(n) } }
        fun numero(fila: Int, col: Int): Double? = celda(fila, col)?.let { it.numero ?: it.texto?.let { t -> Dinero.parsear(t) } }
        val todas: Collection<Celda> get() = celdas.values

        companion object {
            fun de(lista: List<Celda>): Hoja = Hoja(lista.associateBy { it.fila.toLong() * 100_000L + it.col })
        }
    }

    fun leer(entrada: InputStream): Hoja {
        val partes = HashMap<String, ByteArray>()
        ZipInputStream(entrada).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val n = e.name
                if (!e.isDirectory && (n == "xl/sharedStrings.xml" || n == "xl/workbook.xml" ||
                        n == "xl/_rels/workbook.xml.rels" || (n.startsWith("xl/worksheets/") && n.endsWith(".xml")))
                ) {
                    partes[n] = zip.readBytes()
                }
                e = zip.nextEntry
            }
        }
        if (partes.isEmpty()) throw IllegalArgumentException("El archivo no es un Excel .xlsx válido")
        val compartidos = partes["xl/sharedStrings.xml"]?.let { leerCompartidos(it) } ?: emptyList()
        val ruta = rutaPrimeraHoja(partes)
            ?: throw IllegalArgumentException("No se encontró ninguna hoja en el Excel")
        return Hoja.de(leerHoja(partes.getValue(ruta), compartidos))
    }

    private fun parser(bytes: ByteArray): XmlPullParser =
        Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(ByteArrayInputStream(bytes), "UTF-8")
        }

    private fun rutaPrimeraHoja(partes: Map<String, ByteArray>): String? {
        try {
            val wb = partes["xl/workbook.xml"]
            val rels = partes["xl/_rels/workbook.xml.rels"]
            if (wb != null && rels != null) {
                var rid: String? = null
                val p = parser(wb)
                while (p.eventType != XmlPullParser.END_DOCUMENT && rid == null) {
                    if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") rid = p.getAttributeValue(null, "r:id")
                    p.next()
                }
                if (rid != null) {
                    val pr = parser(rels)
                    while (pr.eventType != XmlPullParser.END_DOCUMENT) {
                        if (pr.eventType == XmlPullParser.START_TAG && pr.name == "Relationship" &&
                            pr.getAttributeValue(null, "Id") == rid
                        ) {
                            val target = pr.getAttributeValue(null, "Target") ?: break
                            val ruta = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
                            if (partes.containsKey(ruta)) return ruta
                            break
                        }
                        pr.next()
                    }
                }
            }
        } catch (_: Exception) {
        }
        return partes.keys.filter { it.startsWith("xl/worksheets/sheet") }.sortedWith(compareBy({ it.length }, { it })).firstOrNull()
            ?: partes.keys.firstOrNull { it.startsWith("xl/worksheets/") }
    }

    private fun leerCompartidos(bytes: ByteArray): List<String> {
        val lista = ArrayList<String>()
        val p = parser(bytes)
        val sb = StringBuilder()
        var enT = false
        var enFonetica = 0
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> sb.setLength(0)
                    "t" -> enT = true
                    "rPh" -> enFonetica++
                }
                XmlPullParser.TEXT -> if (enT && enFonetica == 0) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> enT = false
                    "rPh" -> enFonetica--
                    "si" -> lista.add(sb.toString())
                }
            }
            p.next()
        }
        return lista
    }

    private fun columnaIndice(ref: String): Pair<Int, Int>? {
        var col = 0
        var i = 0
        while (i < ref.length && ref[i].isLetter()) {
            col = col * 26 + (ref[i].uppercaseChar() - 'A' + 1)
            i++
        }
        val fila = ref.substring(i).toIntOrNull() ?: return null
        return fila to (col - 1)
    }

    private fun leerHoja(bytes: ByteArray, compartidos: List<String>): List<Celda> {
        val celdas = ArrayList<Celda>()
        val p = parser(bytes)
        var ref: String? = null
        var tipo: String? = null
        var valor: String? = null
        val inline = StringBuilder()
        var enInline = false
        var enT = false
        var filaActual = 0
        var colActual = -1
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> {
                        filaActual = p.getAttributeValue(null, "r")?.toIntOrNull() ?: (filaActual + 1)
                        colActual = -1
                    }
                    "c" -> {
                        ref = p.getAttributeValue(null, "r")
                        tipo = p.getAttributeValue(null, "t")
                        valor = null
                        inline.setLength(0)
                    }
                    "v" -> valor = p.nextText()
                    "is" -> enInline = true
                    "t" -> if (enInline) enT = true
                }
                XmlPullParser.TEXT -> if (enInline && enT) inline.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> enT = false
                    "is" -> enInline = false
                    "c" -> {
                        val pos = ref?.let { columnaIndice(it) } ?: (filaActual to colActual + 1)
                        colActual = pos.second
                        val celda = when (tipo) {
                            "s" -> valor?.trim()?.toIntOrNull()?.let { compartidos.getOrNull(it) }?.let { Celda(pos.first, pos.second, it, null) }
                            "inlineStr" -> Celda(pos.first, pos.second, inline.toString(), null)
                            "str" -> valor?.let { Celda(pos.first, pos.second, it, null) }
                            "b" -> valor?.let { Celda(pos.first, pos.second, if (it == "1") "SI" else "NO", null) }
                            "e" -> null
                            else -> valor?.trim()?.toDoubleOrNull()?.let { Celda(pos.first, pos.second, null, it) }
                        }
                        if (celda != null && (celda.numero != null || !celda.texto.isNullOrBlank())) celdas.add(celda)
                        ref = null; tipo = null; valor = null
                    }
                }
            }
            p.next()
        }
        return celdas
    }
}
