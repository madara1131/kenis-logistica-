package com.kenisshop.logistica.ui.traker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenisshop.logistica.util.Dinero
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

val PALETA = listOf(
    Color(0xFF1E4E9A), Color(0xFFD9468F), Color(0xFF2E9E44), Color(0xFFF2A12E),
    Color(0xFF7B4FD6), Color(0xFF16A6B6), Color(0xFFD93B30), Color(0xFF8D6E63),
    Color(0xFF607D8B), Color(0xFFC0CA33)
)

data class Serie(val nombre: String, val color: Color, val valores: List<Double>)

@Composable
private fun Leyenda(series: List<Serie>) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                Spacer(Modifier.width(5.dp))
                Text(s.nombre, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SinDatos(texto: String = "Todavía no hay gastos para graficar") {
    Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Gráfico circular (dona) interactivo: toca un trozo o una fila de la leyenda. */
@Composable
fun GraficoDona(datos: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    val limpios = remember(datos) { datos.filter { it.second > 0 }.sortedByDescending { it.second } }
    val total = limpios.sumOf { it.second }
    var seleccion by remember(limpios) { mutableStateOf<Int?>(null) }
    val anim = remember(limpios) { Animatable(0f) }
    LaunchedEffect(limpios) { anim.animateTo(1f, tween(800)) }

    if (total <= 0.0) {
        SinDatos()
        return
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(190.dp)
                .pointerInput(limpios) {
                    detectTapGestures { p ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = p.x - cx
                        val dy = p.y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        if (r > size.width / 2f || r < size.width * 0.22f) {
                            seleccion = null
                            return@detectTapGestures
                        }
                        var ang = Math.toDegrees(atan2(dy, dx).toDouble()) + 90.0
                        if (ang < 0) ang += 360.0
                        var acum = 0.0
                        for (i in limpios.indices) {
                            acum += limpios[i].second / total * 360.0
                            if (ang <= acum) {
                                seleccion = if (seleccion == i) null else i
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val grosor = size.minDimension * 0.17f
                var inicio = -90f
                limpios.forEachIndexed { i, par ->
                    val barrido = (par.second / total * 360.0).toFloat() * anim.value
                    val sel = seleccion == i
                    val g = if (sel) grosor * 1.35f else grosor
                    drawArc(
                        color = PALETA[i % PALETA.size].copy(alpha = if (seleccion == null || sel) 1f else 0.35f),
                        startAngle = inicio,
                        sweepAngle = (barrido - 1.2f).coerceAtLeast(0.1f),
                        useCenter = false,
                        topLeft = Offset(grosor * 0.7f, grosor * 0.7f),
                        size = Size(size.width - grosor * 1.4f, size.height - grosor * 1.4f),
                        style = Stroke(width = g)
                    )
                    inicio += barrido
                }
            }
            val s = seleccion
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                Text(
                    if (s == null) "Total" else limpios[s].first,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    Dinero.fmt(if (s == null) total else limpios[s].second),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (s != null) {
                    Text("${(limpios[s].second / total * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        limpios.forEachIndexed { i, (nombre, valor) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (seleccion == i) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { seleccion = if (seleccion == i) null else i }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(PALETA[i % PALETA.size]))
                Spacer(Modifier.width(8.dp))
                Text(nombre, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Dinero.fmt(valor), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${(valor / total * 100).roundToInt()}%",
                    Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Barras verticales agrupadas (se puede deslizar si hay muchas y tocar para ver el detalle). */
@Composable
fun GraficoBarras(etiquetas: List<String>, series: List<Serie>, alto: Dp = 190.dp) {
    val maximo = series.flatMap { it.valores }.maxOrNull() ?: 0.0
    var seleccion by remember(etiquetas) { mutableStateOf<Int?>(null) }
    val anim = remember(series) { Animatable(0f) }
    LaunchedEffect(series) { anim.animateTo(1f, tween(700)) }

    if (maximo <= 0.0 || etiquetas.isEmpty()) {
        SinDatos()
        return
    }
    Column {
        Leyenda(series)
        Spacer(Modifier.height(6.dp))
        val s = seleccion
        Text(
            if (s == null) "Toca una barra para ver el detalle"
            else "${etiquetas[s]}: " + series.joinToString(" · ") { "${it.nombre} ${Dinero.fmt(it.valores[s])}" },
            style = MaterialTheme.typography.bodySmall,
            color = if (s == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            fontWeight = if (s == null) FontWeight.Normal else FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(alto)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            etiquetas.forEachIndexed { i, etiqueta ->
                Column(
                    Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (seleccion == i) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { seleccion = if (seleccion == i) null else i }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        series.forEach { serie ->
                            val frac = ((serie.valores.getOrElse(i) { 0.0 } / maximo).toFloat() * anim.value).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .width(if (series.size > 2) 10.dp else 16.dp)
                                    .fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(serie.color)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(etiqueta, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

/** Líneas por mes: toca o desliza el dedo sobre el gráfico para ver cada punto. */
@Composable
fun GraficoLineas(etiquetas: List<String>, series: List<Serie>, alto: Dp = 200.dp) {
    val maximo = series.flatMap { it.valores }.maxOrNull() ?: 0.0
    var seleccion by remember(etiquetas) { mutableStateOf<Int?>(null) }
    val anim = remember(series) { Animatable(0f) }
    LaunchedEffect(series) { anim.animateTo(1f, tween(900)) }
    val colorGrilla = MaterialTheme.colorScheme.outlineVariant
    val colorSel = MaterialTheme.colorScheme.primary
    val colorFondo = MaterialTheme.colorScheme.surface

    if (maximo <= 0.0 || etiquetas.isEmpty()) {
        SinDatos()
        return
    }
    val n = etiquetas.size
    Column {
        Leyenda(series)
        Spacer(Modifier.height(6.dp))
        val s = seleccion
        Text(
            if (s == null) "Toca el gráfico para ver cada mes · máximo ${Dinero.corto(maximo)}"
            else "${etiquetas[s]}: " + series.joinToString(" · ") { "${it.nombre} ${Dinero.fmt(it.valores[s])}" },
            style = MaterialTheme.typography.bodySmall,
            color = if (s == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            fontWeight = if (s == null) FontWeight.Normal else FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(alto)
                .pointerInput(n) {
                    detectTapGestures { p ->
                        val padH = 16.dp.toPx()
                        seleccion = if (n <= 1) 0 else {
                            (((p.x - padH) / (size.width - 2 * padH)) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                        }
                    }
                }
        ) {
            val padH = 16.dp.toPx()
            val padV = 10.dp.toPx()
            val ancho = size.width - 2 * padH
            val altoUtil = size.height - 2 * padV
            fun x(i: Int): Float = if (n <= 1) size.width / 2f else padH + i * ancho / (n - 1)
            fun y(v: Double): Float = size.height - padV - ((v / maximo).toFloat() * altoUtil * anim.value)

            for (k in 0..3) {
                val yy = padV + k * altoUtil / 3f
                drawLine(colorGrilla, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 1.dp.toPx())
            }
            seleccion?.let { i ->
                drawLine(colorSel.copy(alpha = 0.5f), Offset(x(i), 0f), Offset(x(i), size.height), strokeWidth = 2.dp.toPx())
            }
            series.forEach { serie ->
                val path = Path()
                serie.valores.forEachIndexed { i, v ->
                    if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
                }
                drawPath(path, serie.color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                serie.valores.forEachIndexed { i, v ->
                    val radio = if (seleccion == i) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(colorFondo, radius = radio + 2.dp.toPx(), center = Offset(x(i), y(v)))
                    drawCircle(serie.color, radius = radio, center = Offset(x(i), y(v)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
            etiquetas.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Barra de progreso simple (gasto real contra presupuesto). */
@Composable
fun BarraProgreso(fraccion: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraccion.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}
