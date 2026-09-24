package com.kenisshop.logistica.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kenisshop.logistica.data.EstadoPedido
import com.kenisshop.logistica.data.Pedido
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Exportador {
    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_PDF = "application/pdf"

    private val encabezados = listOf(
        "Código", "Tipo", "Estado", "Empresa Miami", "Origen",
        "Marca de ingreso", "Fecha pedido", "Llegada Miami", "Fecha ingreso", "Días"
    )

    private fun fila(p: Pedido): List<String> = listOf(
        p.codigo,
        p.tipo.etiqueta,
        p.estado().etiqueta,
        p.empresaEnvio,
        p.origen,
        p.marcaIngreso,
        Fechas.formatear(p.fechaPedido),
        Fechas.formatear(p.fechaMiami),
        Fechas.formatear(p.fechaIngreso),
        p.diasTranscurridos().toString()
    )

    private fun archivo(context: Context, nombre: String): File {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        return File(dir, nombre)
    }

    private fun sello(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
    private fun generado(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

    // ---------------------------------------------------------------- EXCEL (.xlsx real)

    fun excel(context: Context, pedidos: List<Pedido>, filtros: String): File {
        val f = archivo(context, "reporte_pedidos_${sello()}.xlsx")
        ZipOutputStream(FileOutputStream(f)).use { zip ->
            fun poner(nombre: String, contenido: String) {
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            poner("[Content_Types].xml", CONTENT_TYPES)
            poner("_rels/.rels", RELS)
            poner("xl/workbook.xml", WORKBOOK)
            poner("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            poner("xl/styles.xml", STYLES)
            poner("xl/worksheets/sheet1.xml", hoja(pedidos, filtros))
        }
        return f
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun columna(i: Int): String = ('A' + i).toString()

    private fun hoja(pedidos: List<Pedido>, filtros: String): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<cols>")
        val anchos = listOf(16, 11, 13, 15, 18, 20, 13, 14, 14, 7)
        anchos.forEachIndexed { i, w ->
            sb.append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
        }
        sb.append("</cols><sheetData>")

        fun filaXml(r: Int, valores: List<String>, estilo: Int) {
            sb.append("<row r=\"$r\">")
            valores.forEachIndexed { i, v ->
                val s = if (estilo > 0) " s=\"$estilo\"" else ""
                sb.append("<c r=\"${columna(i)}$r\" t=\"inlineStr\"$s><is><t>${esc(v)}</t></is></c>")
            }
            sb.append("</row>")
        }

        filaXml(1, listOf("Keni's Shop — Reporte de pedidos"), 2)
        filaXml(2, listOf("Generado: ${generado()}  |  Filtros: $filtros  |  Total: ${pedidos.size}"), 0)
        filaXml(4, encabezados, 1)
        pedidos.forEachIndexed { idx, p -> filaXml(5 + idx, fila(p), 0) }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    // ---------------------------------------------------------------- PDF

    fun pdf(context: Context, pedidos: List<Pedido>, filtros: String): File {
        val f = archivo(context, "reporte_pedidos_${sello()}.pdf")
        val doc = PdfDocument()
        val ancho = 842   // A4 horizontal
        val alto = 595
        val margen = 30f
        val altoFila = 18f
        val columnas = floatArrayOf(80f, 55f, 65f, 80f, 100f, 100f, 70f, 70f, 70f, 40f)

        val pTitulo = Paint().apply { textSize = 18f; isFakeBoldText = true; isAntiAlias = true; color = 0xFF1E4E9A.toInt() }
        val pTexto = Paint().apply { textSize = 9f; isAntiAlias = true; color = 0xFF222222.toInt() }
        val pEncabezado = Paint(pTexto).apply { color = 0xFFFFFFFF.toInt(); isFakeBoldText = true }
        val pFondoEnc = Paint().apply { color = 0xFF1E4E9A.toInt() }
        val pFondoAlt = Paint().apply { color = 0xFFF1F4FA.toInt() }

        var numPagina = 0
        var pagina: PdfDocument.Page? = null
        var y = 0f

        fun nuevaPagina(): Canvas {
            pagina?.let { doc.finishPage(it) }
            numPagina++
            val pg = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numPagina).create())
            pagina = pg
            val c = pg.canvas
            y = margen + 14f
            if (numPagina == 1) {
                c.drawText("Keni's Shop — Reporte de pedidos", margen, y, pTitulo)
                y += 16f
                c.drawText("Generado: ${generado()}   ·   Filtros: $filtros   ·   Total: ${pedidos.size} pedidos", margen, y, pTexto)
                y += 12f
            }
            c.drawRect(margen, y, ancho - margen, y + altoFila, pFondoEnc)
            var x = margen
            encabezados.forEachIndexed { i, h ->
                c.drawText(recortar(h, columnas[i] - 6f, pEncabezado), x + 4f, y + 12.5f, pEncabezado)
                x += columnas[i]
            }
            y += altoFila
            c.drawText("Página $numPagina", ancho - margen - 45f, alto - 15f, pTexto)
            return c
        }

        var canvas = nuevaPagina()
        pedidos.forEachIndexed { idx, p ->
            if (y + altoFila > alto - margen) canvas = nuevaPagina()
            if (idx % 2 == 1) canvas.drawRect(margen, y, ancho - margen, y + altoFila, pFondoAlt)
            var x = margen
            val estado = p.estado()
            fila(p).forEachIndexed { i, v ->
                val paint = if (i == 2) Paint(pTexto).apply { isFakeBoldText = true; color = colorEstado(estado) } else pTexto
                canvas.drawText(recortar(v, columnas[i] - 6f, paint), x + 4f, y + 12.5f, paint)
                x += columnas[i]
            }
            y += altoFila
        }
        if (pedidos.isEmpty()) {
            canvas.drawText("No hay pedidos con los filtros seleccionados.", margen, y + 20f, pTexto)
        }
        pagina?.let { doc.finishPage(it) }
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        return f
    }

    private fun colorEstado(e: EstadoPedido): Int = when (e) {
        EstadoPedido.INGRESADO -> 0xFF2E9E44.toInt()
        EstadoPedido.EN_TRANSITO -> 0xFF1E6FD9.toInt()
        EstadoPedido.PENDIENTE -> 0xFFB8860B.toInt()
        EstadoPedido.VENCIDO -> 0xFFD93B30.toInt()
    }

    private fun recortar(s: String, max: Float, p: Paint): String {
        if (p.measureText(s) <= max) return s
        var t = s
        while (t.isNotEmpty() && p.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    // ---------------------------------------------------------------- COMPARTIR

    fun compartir(context: Context, archivo: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, archivo.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Guardar o compartir reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---------------------------------------------------------------- Plantillas XLSX

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private const val WORKBOOK = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Pedidos" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private const val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="3"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font><font><b/><sz val="14"/><color rgb="FF1E4E9A"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF1E4E9A"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""
}
