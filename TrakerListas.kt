@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.kenisshop.logistica.ui.traker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kenisshop.logistica.data.traker.AjustesTraker
import com.kenisshop.logistica.data.traker.DatosTraker
import com.kenisshop.logistica.data.traker.ItemLista
import com.kenisshop.logistica.data.traker.TipoLista
import com.kenisshop.logistica.data.traker.listaEtiquetas
import com.kenisshop.logistica.data.traker.pagada
import com.kenisshop.logistica.data.traker.paginaNombre
import com.kenisshop.logistica.data.traker.pendiente
import com.kenisshop.logistica.ui.CampoFecha
import com.kenisshop.logistica.ui.ChipFiltro
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.EstadoVerde
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.util.AnalizadorTraker
import com.kenisshop.logistica.util.Dinero
import com.kenisshop.logistica.util.Fechas
import com.kenisshop.logistica.util.Meses
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ====================================================================== Identidad visual por tipo

val TipoLista.colorLista: Color
    get() = when (this) {
        TipoLista.DEUDA -> Color(0xFFD93B30)
        TipoLista.GASTO_EXTRA -> Color(0xFFEF8A17)
        TipoLista.AHORRO -> Color(0xFF2E9E44)
        TipoLista.DIEZMO -> Color(0xFF7B4FD6)
        TipoLista.NOTA -> Color(0xFF16A6B6)
    }

val TipoLista.icono: ImageVector
    get() = when (this) {
        TipoLista.DEUDA -> Icons.Default.CreditCard
        TipoLista.GASTO_EXTRA -> Icons.Default.MoneyOff
        TipoLista.AHORRO -> Icons.Default.Savings
        TipoLista.DIEZMO -> Icons.Default.VolunteerActivism
        TipoLista.NOTA -> Icons.Default.EditNote
    }

private val TipoLista.tituloPestana: String
    get() = when (this) {
        TipoLista.NOTA -> "Libreta"
        else -> etiqueta
    }

/** Colores de hoja para la libreta. */
private val COLORES_HOJA = listOf(
    Color(0xFFFFF8DC), Color(0xFFFFE4EC), Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFFF3E0), Color(0xFFEDE7F6)
)

private val formatoRecordatorio: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy h:mm a")
private fun textoRecordatorio(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatoRecordatorio)

private fun ordenMes(nombre: String): Int = Meses.desdeTexto(nombre) ?: 99

// ====================================================================== Pantalla principal de Listas

@Composable
fun ListasTraker(d: DatosTraker, vm: TrakerViewModel) {
    val pestanas: List<TipoLista?> = listOf(null) + TipoLista.entries
    val pager = rememberPagerState(pageCount = { pestanas.size })
    val scope = rememberCoroutineScope()
    var editar by remember { mutableStateOf<Pair<ItemLista?, TipoLista>?>(null) }
    var paginaNueva by remember { mutableStateOf("General") }
    var ajustesAbiertos by remember { mutableStateOf<TipoLista?>(null) }
    val totales = remember(d.listas) { AnalizadorTraker.totales(d.listas) }

    Column(Modifier.fillMaxSize()) {
        // Pestañas de colores con su total (toque rápido o deslizar)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(pestanas) { i, tipo ->
                val sel = pager.currentPage == i
                val color = tipo?.colorLista ?: MaterialTheme.colorScheme.primary
                val subtitulo = when (tipo) {
                    null -> "Panel"
                    TipoLista.DEUDA -> Dinero.corto(totales.deudaPendiente)
                    TipoLista.GASTO_EXTRA -> Dinero.corto(totales.extras)
                    TipoLista.AHORRO -> Dinero.corto(totales.ahorro)
                    TipoLista.DIEZMO -> Dinero.corto(totales.diezmo)
                    TipoLista.NOTA -> "${d.listas.count { it.lista == TipoLista.NOTA }} notas"
                }
                Surface(
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    shape = RoundedCornerShape(14.dp),
                    color = if (sel) color else color.copy(alpha = 0.12f),
                    border = if (sel) null else BorderStroke(1.dp, color.copy(alpha = 0.4f))
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(tipo?.icono ?: Icons.Default.Dashboard, null, tint = if (sel) Color.White else color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                tipo?.tituloPestana ?: "Resumen",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                subtitulo,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { p ->
            val irA: (TipoLista) -> Unit = { t -> scope.launch { pager.animateScrollToPage(pestanas.indexOf(t)) } }
            val tipo = pestanas[p]
            if (tipo == null) {
                PaginaResumen(d, vm.ajustes, irA)
            } else if (tipo == TipoLista.NOTA) {
                Libreta(
                    d, vm,
                    onEditar = { editar = it to TipoLista.NOTA },
                    onNueva = { pag -> paginaNueva = pag; editar = null to TipoLista.NOTA }
                )
            } else {
                PaginaFinanzas(
                    tipo, d, vm,
                    onEditar = { editar = it to tipo },
                    onNuevo = { editar = null to tipo },
                    onAjustes = { ajustesAbiertos = tipo }
                )
            }
        }
    }

    editar?.let { (item, tipo) ->
        if (tipo == TipoLista.NOTA) {
            EditorNota(
                existente = item,
                paginaInicial = item?.paginaNombre ?: paginaNueva,
                paginas = (vm.ajustes.paginas + d.listas.filter { it.lista == TipoLista.NOTA }.map { it.paginaNombre }).distinct(),
                onGuardar = { vm.guardarItem(it) },
                onBorrar = { vm.borrarItem(it) },
                onCerrar = { editar = null }
            )
        } else {
            EditorFinanza(
                existente = item,
                tipo = tipo,
                onGuardar = { vm.guardarItem(it) },
                onBorrar = { vm.borrarItem(it) },
                onCerrar = { editar = null }
            )
        }
    }
    ajustesAbiertos?.let { tipo ->
        DialogoLimites(tipo, vm.ajustes, onGuardar = { vm.guardarAjustes(it) }, onCerrar = { ajustesAbiertos = null })
    }
}

// ====================================================================== Arrastrar y soltar

/**
 * Lista que se reordena arrastrando el ícono ⋮⋮ de cada fila.
 * Al soltar, se guarda el nuevo orden.
 */
@Composable
fun ListaReordenable(
    items: List<ItemLista>,
    onReordenar: (List<ItemLista>) -> Unit,
    fila: @Composable (item: ItemLista, asa: Modifier, arrastrando: Boolean) -> Unit
) {
    val actuales by rememberUpdatedState(items)
    val orden = remember { mutableStateListOf<ItemLista>() }
    val alturas = remember { mutableStateMapOf<Long, Int>() }
    var arrastrandoId by remember { mutableStateOf<Long?>(null) }
    var desplazamiento by remember { mutableStateOf(0f) }
    var ultimoOrden by remember { mutableStateOf<List<Long>?>(null) }

    val idsActuales = items.map { it.id }
    val guardado = ultimoOrden
    val esperandoGuardado = guardado != null && guardado != idsActuales && guardado.toSet() == idsActuales.toSet()
    val mostrar: List<ItemLista> = if (arrastrandoId != null || esperandoGuardado) {
        val porId = items.associateBy { it.id }
        orden.mapNotNull { porId[it.id] }
    } else items

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        mostrar.forEach { item ->
            key(item.id) {
                val esEste = arrastrandoId == item.id
                val asa = Modifier.pointerInput(item.id) {
                    val espacio = 8.dp.toPx()
                    detectDragGestures(
                        onDragStart = {
                            orden.clear(); orden.addAll(actuales)
                            arrastrandoId = item.id
                            desplazamiento = 0f
                        },
                        onDrag = { cambio, arrastre ->
                            cambio.consume()
                            desplazamiento += arrastre.y
                            val i = orden.indexOfFirst { it.id == item.id }
                            if (i >= 0) {
                                if (desplazamiento > 0 && i < orden.lastIndex) {
                                    val h = (alturas[orden[i + 1].id] ?: 0) + espacio
                                    if (desplazamiento > h / 2) {
                                        orden.add(i + 1, orden.removeAt(i)); desplazamiento -= h
                                    }
                                } else if (desplazamiento < 0 && i > 0) {
                                    val h = (alturas[orden[i - 1].id] ?: 0) + espacio
                                    if (-desplazamiento > h / 2) {
                                        orden.add(i - 1, orden.removeAt(i)); desplazamiento += h
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            val nuevo = orden.toList()
                            ultimoOrden = nuevo.map { it.id }
                            arrastrandoId = null
                            desplazamiento = 0f
                            if (nuevo.map { it.id } != actuales.map { it.id }) onReordenar(nuevo)
                        },
                        onDragCancel = {
                            arrastrandoId = null
                            desplazamiento = 0f
                        }
                    )
                }
                Box(
                    Modifier
                        .onSizeChanged { alturas[item.id] = it.height }
                        .zIndex(if (esEste) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (esEste) desplazamiento else 0f
                            scaleX = if (esEste) 1.03f else 1f
                            scaleY = if (esEste) 1.03f else 1f
                            shadowElevation = if (esEste) 16f else 0f
                        }
                ) {
                    fila(item, asa, esEste)
                }
            }
        }
    }
}

// ====================================================================== Piezas comunes

@Composable
private fun EncabezadoColor(
    tipo: TipoLista?,
    titulo: String,
    valor: String,
    detalle: String,
    acciones: @Composable () -> Unit = {}
) {
    val color = tipo?.colorLista ?: MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.72f))))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) { Icon(tipo?.icono ?: Icons.Default.Dashboard, null, tint = Color.White) }
                Spacer(Modifier.width(10.dp))
                Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                acciones()
            }
            Spacer(Modifier.height(10.dp))
            Text(valor, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
            Text(detalle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun IndicadorLimite(titulo: String, actual: Double, limite: Double?, color: Color, invertido: Boolean = false, onDefinir: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDefinir) {
                    Icon(Icons.Default.Tune, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                    Text(if (limite == null) "Definir" else "Cambiar")
                }
            }
            if (limite == null || limite <= 0) {
                Text("Sin definir. Te avisaremos cuando lo superes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val frac = (actual / limite).toFloat()
                // invertido = meta (más es mejor); normal = límite (más es peor)
                val colorBarra = if (invertido) color else when {
                    frac > 1f -> EstadoRojo
                    frac >= 0.9f -> Color(0xFFF2C12E)
                    else -> color
                }
                BarraProgreso(frac, colorBarra)
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("${Dinero.fmt(actual)} de ${Dinero.fmt(limite)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${(frac * 100).toInt()}%", fontWeight = FontWeight.Bold, color = colorBarra)
                }
                if (!invertido && frac > 1f) {
                    Text("⚠️ Superaste el límite por ${Dinero.fmt(actual - limite)}", color = EstadoRojo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
                if (invertido && frac >= 1f) {
                    Text("🎉 ¡Meta cumplida!", color = EstadoVerde, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TarjetaGrafico(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            contenido()
        }
    }
}

@Composable
private fun Kpi(tipo: TipoLista, titulo: String, valor: String, detalle: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = tipo.colorLista.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tipo.colorLista.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(tipo.icono, null, tint = tipo.colorLista, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(valor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = tipo.colorLista)
            Text(detalle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ====================================================================== Resumen financiero

@Composable
private fun PaginaResumen(d: DatosTraker, ajustes: AjustesTraker, irA: (TipoLista) -> Unit) {
    val t = AnalizadorTraker.totales(d.listas)
    val alertas = AnalizadorTraker.alertasListas(d.listas, ajustes)
    val notas = d.listas.filter { it.lista == TipoLista.NOTA }
    val ahora = System.currentTimeMillis()
    val proximos = notas.filter { (it.recordatorio ?: 0L) > ahora }.sortedBy { it.recordatorio }.take(3)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EncabezadoColor(
            null, "Estado financiero",
            Dinero.fmt(t.ahorro - t.deudaPendiente),
            "Ahorrado ${Dinero.fmt(t.ahorro)} − deudas pendientes ${Dinero.fmt(t.deudaPendiente)}"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi(TipoLista.DEUDA, "Deudas", Dinero.fmt(t.deudaPendiente), "pagado ${Dinero.fmt(t.deudaPagada)}", Modifier.weight(1f)) { irA(TipoLista.DEUDA) }
            Kpi(TipoLista.AHORRO, "Ahorros", Dinero.fmt(t.ahorro), ajustes.metaAhorro?.let { "meta ${Dinero.fmt(it)}" } ?: "sin meta", Modifier.weight(1f)) { irA(TipoLista.AHORRO) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi(TipoLista.GASTO_EXTRA, "Gastos extras", Dinero.fmt(t.extras), ajustes.limiteExtras?.let { "límite ${Dinero.fmt(it)}" } ?: "sin límite", Modifier.weight(1f)) { irA(TipoLista.GASTO_EXTRA) }
            Kpi(TipoLista.DIEZMO, "Diezmo", Dinero.fmt(t.diezmo), "${d.listas.count { it.lista == TipoLista.DIEZMO }} registros", Modifier.weight(1f)) { irA(TipoLista.DIEZMO) }
        }
        if (alertas.isNotEmpty()) {
            TarjetaGrafico("Alertas") {
                alertas.forEach { a ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (a.grave) Icons.Default.Warning else Icons.Default.CheckCircle, null,
                            tint = if (a.grave) EstadoRojo else EstadoVerde, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(a.texto, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        TarjetaGrafico("¿Cómo se reparte tu dinero?") {
            GraficoDona(
                listOf(
                    "Ahorros" to t.ahorro,
                    "Deudas pendientes" to t.deudaPendiente,
                    "Gastos extras" to t.extras,
                    "Diezmo" to t.diezmo
                )
            )
        }
        val meses = (d.listas.filter { it.lista == TipoLista.AHORRO || it.lista == TipoLista.DIEZMO }
            .map { it.nombre }).distinct().sortedBy { ordenMes(it) }
        if (meses.isNotEmpty()) {
            TarjetaGrafico("Ahorro y diezmo por mes") {
                GraficoBarras(
                    meses.map { it.take(3) },
                    listOf(
                        Serie("Ahorro", TipoLista.AHORRO.colorLista, meses.map { m -> d.listas.filter { it.lista == TipoLista.AHORRO && it.nombre == m }.sumOf { it.monto ?: 0.0 } }),
                        Serie("Diezmo", TipoLista.DIEZMO.colorLista, meses.map { m -> d.listas.filter { it.lista == TipoLista.DIEZMO && it.nombre == m }.sumOf { it.monto ?: 0.0 } })
                    )
                )
            }
        }
        TarjetaGrafico("📒 Libreta") {
            Text("${notas.size} notas · ${notas.count { it.fijada }} fijadas", style = MaterialTheme.typography.bodyMedium)
            if (proximos.isEmpty()) {
                Text("Sin recordatorios próximos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            proximos.forEach { n ->
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, null, tint = TipoLista.NOTA.colorLista, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${textoRecordatorio(n.recordatorio!!)} · ${n.nombre}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = { irA(TipoLista.NOTA) }) { Text("Abrir libreta") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ====================================================================== Deudas, gastos extras, ahorros y diezmo

@Composable
private fun PaginaFinanzas(
    tipo: TipoLista,
    d: DatosTraker,
    vm: TrakerViewModel,
    onEditar: (ItemLista) -> Unit,
    onNuevo: () -> Unit,
    onAjustes: () -> Unit
) {
    val esTablet = LocalConfiguration.current.screenWidthDp >= 600
    val todos = d.listas.filter { it.lista == tipo }
    var filtro by rememberSaveable(tipo) { mutableStateOf(0) }
    var abonar by remember { mutableStateOf<ItemLista?>(null) }
    val ajustes = vm.ajustes
    val total = todos.sumOf { it.monto ?: 0.0 }

    val filtros: List<Pair<String, (ItemLista) -> Boolean>> = when (tipo) {
        TipoLista.DEUDA -> listOf("Todas" to { _: ItemLista -> true }, "Pendientes" to { it: ItemLista -> !it.pagada }, "Pagadas" to { it: ItemLista -> it.pagada })
        TipoLista.GASTO_EXTRA -> {
            val mesActual = Meses.actual()
            listOf(
                "Todos" to { _: ItemLista -> true },
                "Este mes" to { it: ItemLista -> it.fecha?.let { f -> Fechas.aLocalDate(f).let { ld -> Meses.clave(ld.year, ld.monthValue) } } == mesActual },
                "Sin fecha" to { it: ItemLista -> it.fecha == null }
            )
        }
        else -> listOf("Todos" to { _: ItemLista -> true }, "Con monto" to { it: ItemLista -> (it.monto ?: 0.0) > 0 })
    }
    val visibles = todos.filter(filtros[filtro.coerceIn(0, filtros.lastIndex)].second)

    val encabezado: @Composable () -> Unit = {
        when (tipo) {
            TipoLista.DEUDA -> {
                val t = AnalizadorTraker.totales(d.listas)
                EncabezadoColor(tipo, "Deudas pendientes", Dinero.fmt(t.deudaPendiente), "Total ${Dinero.fmt(t.deudaTotal)} · pagado ${Dinero.fmt(t.deudaPagada)}") {
                    IconButton(onClick = onNuevo) { Icon(Icons.Default.Add, "Agregar", tint = Color.White) }
                }
            }
            TipoLista.AHORRO -> EncabezadoColor(tipo, "Total ahorrado", Dinero.fmt(total), "${todos.size} registro(s)") {
                IconButton(onClick = onNuevo) { Icon(Icons.Default.Add, "Agregar", tint = Color.White) }
            }
            else -> EncabezadoColor(tipo, tipo.etiqueta, Dinero.fmt(total), "${todos.size} registro(s)") {
                IconButton(onClick = onNuevo) { Icon(Icons.Default.Add, "Agregar", tint = Color.White) }
            }
        }
    }

    val indicadores: @Composable () -> Unit = {
        when (tipo) {
            TipoLista.DEUDA -> {
                val t = AnalizadorTraker.totales(d.listas)
                TarjetaGrafico("Avance de pagos") {
                    BarraProgreso(if (t.deudaTotal > 0) (t.deudaPagada / t.deudaTotal).toFloat() else 0f, EstadoVerde)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (t.deudaTotal > 0) "Has pagado ${(t.deudaPagada / t.deudaTotal * 100).toInt()}% de tus deudas" else "Sin deudas registradas",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IndicadorLimite("Límite de deuda pendiente", t.deudaPendiente, ajustes.limiteDeudas, tipo.colorLista, onDefinir = onAjustes)
            }
            TipoLista.GASTO_EXTRA -> IndicadorLimite("Límite de gastos extras", total, ajustes.limiteExtras, tipo.colorLista, onDefinir = onAjustes)
            TipoLista.AHORRO -> IndicadorLimite("Meta de ahorro", total, ajustes.metaAhorro, tipo.colorLista, invertido = true, onDefinir = onAjustes)
            else -> Unit
        }
    }

    val graficos: @Composable () -> Unit = {
        when (tipo) {
            TipoLista.DEUDA -> TarjetaGrafico("Pendiente por deuda") {
                GraficoDona(todos.map { it.nombre to it.pendiente })
            }
            TipoLista.GASTO_EXTRA -> {
                val grupos = todos.groupBy { it.fecha?.let { f -> Fechas.aLocalDate(f).let { ld -> Meses.clave(ld.year, ld.monthValue) } } ?: "Sin fecha" }
                val claves = grupos.keys.sortedWith(compareBy { if (it == "Sin fecha") "0" else it })
                TarjetaGrafico("Gastos extras por mes") {
                    GraficoBarras(
                        claves.map { if (it == "Sin fecha") "S/F" else Meses.cortoConAnio(it) },
                        listOf(Serie("Gasto", tipo.colorLista, claves.map { k -> grupos.getValue(k).sumOf { it.monto ?: 0.0 } }))
                    )
                }
                TarjetaGrafico("¿En qué se fueron?") { GraficoDona(todos.map { it.nombre to (it.monto ?: 0.0) }) }
            }
            TipoLista.AHORRO, TipoLista.DIEZMO -> {
                val ordenados = todos.sortedBy { ordenMes(it.nombre) }
                var acumulado = 0.0
                val acumulados = ordenados.map { acumulado += it.monto ?: 0.0; acumulado }
                TarjetaGrafico(if (tipo == TipoLista.AHORRO) "Crecimiento del ahorro" else "Diezmo acumulado") {
                    GraficoLineas(ordenados.map { it.nombre.take(3) }, listOf(Serie("Acumulado", tipo.colorLista, acumulados)))
                }
                TarjetaGrafico("Por mes") {
                    GraficoBarras(ordenados.map { it.nombre.take(3) }, listOf(Serie(tipo.etiqueta, tipo.colorLista, ordenados.map { it.monto ?: 0.0 })))
                }
            }
            else -> Unit
        }
    }

    val lista: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filtros.forEachIndexed { i, (nombre, cond) ->
                    ChipFiltro("$nombre (${todos.count(cond)})", filtro == i, tipo.colorLista) { filtro = i }
                }
            }
            if (visibles.isEmpty()) {
                Text("Sin registros", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
            Text(
                "Mantén y arrastra ⋮⋮ para ordenar · toca para editar · desliza ← para eliminar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ListaReordenable(visibles, onReordenar = { vm.reordenar(it) }) { item, asa, arrastrando ->
                DeslizarParaBorrar("Se eliminará \"${item.nombre}\".", { vm.borrarItem(item) }) {
                    FilaFinanza(item, tipo, asa, arrastrando, onClick = { onEditar(item) }, onAbonar = { abonar = item })
                }
            }
            OutlinedButton(onClick = onNuevo, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Agregar a ${tipo.etiqueta.lowercase()}")
            }
        }
    }

    if (esTablet) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                encabezado(); indicadores(); lista(); Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                graficos(); Spacer(Modifier.height(24.dp))
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            encabezado(); indicadores(); lista(); graficos(); Spacer(Modifier.height(24.dp))
        }
    }

    abonar?.let { deuda ->
        DialogoMonto(
            titulo = "Abonar a ${deuda.nombre}",
            detalle = "Pendiente: ${Dinero.fmt(deuda.pendiente)}",
            onAceptar = { vm.abonar(deuda, it) },
            onCerrar = { abonar = null }
        )
    }
}

@Composable
private fun FilaFinanza(
    item: ItemLista,
    tipo: TipoLista,
    asa: Modifier,
    arrastrando: Boolean,
    onClick: () -> Unit,
    onAbonar: () -> Unit
) {
    val color = tipo.colorLista
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (arrastrando) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = if (arrastrando) 0.8f else 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(asa.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DragIndicator, "Arrastrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(if (item.pagada) Icons.Default.CheckCircle else tipo.icono, null, tint = if (item.pagada) EstadoVerde else color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                when (tipo) {
                    TipoLista.DEUDA -> {
                        val monto = item.monto ?: 0.0
                        val pagado = item.pagado ?: 0.0
                        Spacer(Modifier.height(4.dp))
                        BarraProgreso(if (monto > 0) (pagado / monto).toFloat() else 0f, if (item.pagada) EstadoVerde else color)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (item.pagada) "✅ Pagada" else "Pagado ${Dinero.fmt(pagado)} · falta ${Dinero.fmt(item.pendiente)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.pagada) EstadoVerde else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TipoLista.GASTO_EXTRA -> Text(
                        item.fecha?.let { Fechas.formatear(it) } ?: "Sin fecha",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Unit
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(Dinero.fmt(item.monto), fontWeight = FontWeight.Bold, color = color)
                if (tipo == TipoLista.DEUDA && !item.pagada && (item.monto ?: 0.0) > 0) {
                    FilledTonalButton(onClick = onAbonar, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                        Icon(Icons.Default.Payments, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                        Text("Abonar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ====================================================================== Libreta interactiva

@Composable
private fun Libreta(d: DatosTraker, vm: TrakerViewModel, onEditar: (ItemLista) -> Unit, onNueva: (String) -> Unit) {
    val notas = d.listas.filter { it.lista == TipoLista.NOTA }
    val paginas = (vm.ajustes.paginas + notas.map { it.paginaNombre }).distinct()
    var pagina by rememberSaveable { mutableStateOf<String?>(null) }   // null = todas
    var busqueda by rememberSaveable { mutableStateOf("") }
    var etiqueta by rememberSaveable { mutableStateOf<String?>(null) }
    var soloRecordatorio by rememberSaveable { mutableStateOf(false) }
    var rapida by rememberSaveable { mutableStateOf("") }
    var rapidaAviso by rememberSaveable { mutableStateOf<Long?>(null) }
    val contexto = LocalContext.current
    var nuevaPagina by remember { mutableStateOf(false) }
    var borrarPagina by remember { mutableStateOf<String?>(null) }
    val esTablet = LocalConfiguration.current.screenWidthDp >= 600

    val etiquetas = notas.flatMap { it.listaEtiquetas }.distinct().sorted()
    val visibles = notas.filter { n ->
        (pagina == null || n.paginaNombre == pagina) &&
            (etiqueta == null || etiqueta in n.listaEtiquetas) &&
            (!soloRecordatorio || n.recordatorio != null) &&
            (busqueda.isBlank() || n.nombre.contains(busqueda.trim(), true) || n.listaEtiquetas.any { it.contains(busqueda.trim(), true) })
    }.sortedWith(compareByDescending<ItemLista> { it.fijada }.thenBy { it.orden }.thenByDescending { it.id })

    fun guardarRapida() {
        val texto = rapida.trim()
        if (texto.isEmpty()) return
        // #etiquetas escritas en el texto se guardan como etiquetas
        val tags = Regex("#([\\p{L}\\p{N}_]+)").findAll(texto).map { it.groupValues[1] }.toList()
        vm.guardarItem(
            ItemLista(
                lista = TipoLista.NOTA,
                nombre = texto,
                monto = null,
                orden = (notas.minOfOrNull { it.orden } ?: 0) - 1,
                pagina = pagina?.takeIf { it != "General" },
                etiquetas = tags.joinToString(",").ifEmpty { null },
                recordatorio = rapidaAviso
            )
        )
        rapida = ""
        rapidaAviso = null
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Páginas (secciones) de la libreta
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ChipFiltro("📚 Todas (${notas.size})", pagina == null, TipoLista.NOTA.colorLista) { pagina = null }
            paginas.forEach { p ->
                ChipFiltro("📄 $p (${notas.count { it.paginaNombre == p }})", pagina == p, TipoLista.NOTA.colorLista) { pagina = p }
            }
            AssistChip(onClick = { nuevaPagina = true }, label = { Text("Página") }, leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
            val actual = pagina
            if (actual != null && actual != "General") {
                IconButton(onClick = { borrarPagina = actual }) { Icon(Icons.Default.Delete, "Eliminar página", tint = EstadoRojo) }
            }
        }

        // Escritura rápida estilo hoja de libreta
        Surface(shape = RoundedCornerShape(16.dp), color = COLORES_HOJA[0], shadowElevation = 2.dp) {
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditNote, null, tint = Color(0xFF8D6E63))
                OutlinedTextField(
                    value = rapida,
                    onValueChange = { rapida = it },
                    placeholder = { Text("Escribe rápido en ${pagina ?: "General"}… usa #etiqueta", color = Color(0xFF8D6E63)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { guardarRapida() }),
                    maxLines = 4,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFF3E2723),
                        unfocusedTextColor = Color(0xFF3E2723)
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { elegirFechaHora(contexto, rapidaAviso) { rapidaAviso = it } }) {
                    Icon(Icons.Default.Alarm, "Poner fecha y hora", tint = if (rapidaAviso != null) TipoLista.NOTA.colorLista else Color(0xFF8D6E63))
                }
                IconButton(onClick = { guardarRapida() }, enabled = rapida.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Guardar nota", tint = TipoLista.NOTA.colorLista)
                }
            }
        }
        rapidaAviso?.let { aviso ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = TipoLista.NOTA.colorLista) {
                    Row(Modifier.padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Aviso: ${textoRecordatorio(aviso)}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { rapidaAviso = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Quitar aviso", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("Te llegará una notificación", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Filtros rápidos
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar en la libreta") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChipFiltro("⏰ Con recordatorio", soloRecordatorio, TipoLista.NOTA.colorLista) { soloRecordatorio = !soloRecordatorio }
            etiquetas.forEach { e ->
                ChipFiltro("#$e", etiqueta == e, MaterialTheme.colorScheme.secondary) { etiqueta = if (etiqueta == e) null else e }
            }
        }
        Text(
            "Toca para editar · mantén presionado para fijar 📌 · arrastra ⋮⋮ para ordenar · desliza ← para borrar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (visibles.isEmpty()) {
            Text(
                if (notas.isEmpty()) "La libreta está vacía. Escribe tu primera nota arriba ✍️" else "Ninguna nota coincide",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        }
        ListaReordenable(visibles, onReordenar = { vm.reordenar(it) }) { nota, asa, arrastrando ->
            DeslizarParaBorrar("Se eliminará esta nota.", { vm.borrarItem(nota) }) {
                HojaNota(nota, asa, arrastrando, onClick = { onEditar(nota) }, onFijar = { vm.alternarFijada(nota) }, mostrarPagina = pagina == null || esTablet)
            }
        }
        Button(
            onClick = { onNueva(pagina ?: "General") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TipoLista.NOTA.colorLista, contentColor = Color.White)
        ) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Nota completa (con recordatorio y etiquetas)")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (nuevaPagina) {
        var nombre by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { nuevaPagina = false },
            title = { Text("Nueva página") },
            text = {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre (ej: Proveedores, Ideas)") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre.isNotBlank()) { vm.agregarPagina(nombre); pagina = nombre.trim() }
                    nuevaPagina = false
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { nuevaPagina = false }) { Text("Cancelar") } }
        )
    }
    borrarPagina?.let { p ->
        AlertDialog(
            onDismissRequest = { borrarPagina = null },
            title = { Text("¿Eliminar la página \"$p\"?") },
            text = { Text("Las notas de esta página pasarán a General. No se borra ninguna nota.") },
            confirmButton = {
                TextButton(onClick = { vm.borrarPagina(p); if (pagina == p) pagina = null; borrarPagina = null }) { Text("Eliminar", color = EstadoRojo) }
            },
            dismissButton = { TextButton(onClick = { borrarPagina = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun HojaNota(nota: ItemLista, asa: Modifier, arrastrando: Boolean, onClick: () -> Unit, onFijar: () -> Unit, mostrarPagina: Boolean) {
    val fondo = nota.color?.let { Color(it) } ?: COLORES_HOJA[0]
    val linea = Color(0xFF90CAF9).copy(alpha = 0.45f)
    val margen = Color(0xFFEF9A9A).copy(alpha = 0.6f)
    val tinta = Color(0xFF3E2723)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .border(1.dp, if (arrastrando) TipoLista.NOTA.colorLista else Color.Black.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .drawBehind {
                // Líneas de cuaderno y margen rojo
                val paso = 26.dp.toPx()
                var y = 34.dp.toPx()
                while (y < size.height) {
                    drawLine(linea, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    y += paso
                }
                drawLine(margen, Offset(36.dp.toPx(), 0f), Offset(36.dp.toPx(), size.height), strokeWidth = 1.5.dp.toPx())
            }
            .combinedClickable(onClick = onClick, onLongClick = onFijar)
    ) {
        Row(Modifier.padding(start = 2.dp, end = 4.dp, top = 6.dp, bottom = 8.dp)) {
            Box(asa.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DragIndicator, "Arrastrar", tint = tinta.copy(alpha = 0.4f))
            }
            Column(Modifier.weight(1f).padding(start = 6.dp, top = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mostrarPagina) {
                        Text("📄 ${nota.paginaNombre}", style = MaterialTheme.typography.labelSmall, color = tinta.copy(alpha = 0.55f), modifier = Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f))
                    if (nota.fijada) Icon(Icons.Default.PushPin, "Fijada", tint = Color(0xFFD93B30), modifier = Modifier.size(18.dp))
                }
                Text(nota.nombre, color = tinta, style = MaterialTheme.typography.bodyLarge, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight, maxLines = 8, overflow = TextOverflow.Ellipsis)
                val tags = nota.listaEtiquetas
                if (tags.isNotEmpty() || nota.recordatorio != null) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        nota.recordatorio?.let { r ->
                            val vencido = r < System.currentTimeMillis()
                            Surface(shape = RoundedCornerShape(8.dp), color = if (vencido) Color.Black.copy(alpha = 0.08f) else TipoLista.NOTA.colorLista) {
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Alarm, null, Modifier.size(14.dp), tint = if (vencido) tinta else Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text(textoRecordatorio(r), style = MaterialTheme.typography.labelSmall, color = if (vencido) tinta else Color.White)
                                }
                            }
                        }
                        tags.forEach { t ->
                            Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.07f)) {
                                Text("#$t", Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = tinta)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================== Editores

/** Elige fecha y luego hora; solo acepta momentos en el futuro. */
private fun elegirFechaHora(context: Context, inicial: Long?, onElegido: (Long) -> Unit) {
    val base = inicial?.takeIf { it > System.currentTimeMillis() }
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        ?: LocalDateTime.now().plusHours(1).withMinute(0)
    DatePickerDialog(context, { _, a, m, dia ->
        TimePickerDialog(context, { _, h, min ->
            val elegido = LocalDateTime.of(a, m + 1, dia, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (elegido <= System.currentTimeMillis()) {
                android.widget.Toast.makeText(context, "Esa hora ya pasó, elige una hora futura", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                onElegido(elegido)
            }
        }, base.hour, base.minute, false).show()
    }, base.year, base.monthValue - 1, base.dayOfMonth).apply {
        datePicker.minDate = System.currentTimeMillis() - 1000
    }.show()
}

@Composable
private fun EditorNota(
    existente: ItemLista?,
    paginaInicial: String,
    paginas: List<String>,
    onGuardar: (ItemLista) -> Unit,
    onBorrar: (ItemLista) -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val hoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var texto by remember { mutableStateOf(existente?.nombre ?: "") }
    var pagina by remember { mutableStateOf(paginaInicial) }
    val tags = remember { mutableStateListOf<String>().apply { addAll(existente?.listaEtiquetas ?: emptyList()) } }
    var nuevaTag by remember { mutableStateOf("") }
    var recordatorio by remember { mutableStateOf(existente?.recordatorio) }
    var fijada by remember { mutableStateOf(existente?.fijada ?: false) }
    var color by remember { mutableStateOf(existente?.color) }

    fun agregarTag() {
        val t = nuevaTag.trim().removePrefix("#").replace(",", "")
        if (t.isNotEmpty() && t !in tags) tags.add(t)
        nuevaTag = ""
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = hoja, containerColor = color?.let { Color(it) } ?: COLORES_HOJA[0]) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (existente == null) "📝 Nueva nota" else "📝 Editar nota", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), modifier = Modifier.weight(1f))
                Icon(Icons.Default.PushPin, null, tint = if (fijada) Color(0xFFD93B30) else Color(0xFF8D6E63))
                Spacer(Modifier.width(4.dp))
                Switch(checked = fijada, onCheckedChange = { fijada = it })
            }
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                placeholder = { Text("Escribe aquí…") },
                minLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF3E2723), unfocusedTextColor = Color(0xFF3E2723)),
                modifier = Modifier.fillMaxWidth()
            )
            // Fecha y hora del aviso (llega como notificación al teléfono)
            Surface(shape = RoundedCornerShape(14.dp), color = TipoLista.NOTA.colorLista.copy(alpha = 0.12f)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, null, tint = TipoLista.NOTA.colorLista)
                        Spacer(Modifier.width(6.dp))
                        Text("Fecha y hora del aviso", fontWeight = FontWeight.SemiBold, color = Color(0xFF3E2723), modifier = Modifier.weight(1f))
                        if (recordatorio != null) {
                            IconButton(onClick = { recordatorio = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Quitar aviso", tint = Color(0xFF3E2723))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { elegirFechaHora(context, recordatorio) { recordatorio = it } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TipoLista.NOTA.colorLista, contentColor = Color.White)
                    ) {
                        Text(recordatorio?.let { "📅 " + textoRecordatorio(it) + " · cambiar" } ?: "Elegir fecha y hora")
                    }
                    Text(
                        if (recordatorio != null) "Te llegará una notificación al teléfono a esa hora, aunque la app esté cerrada."
                        else "Opcional: si eliges fecha y hora, te llega una notificación.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5D4037),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Text("Página", fontWeight = FontWeight.SemiBold, color = Color(0xFF3E2723))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (paginas + paginaInicial).distinct().forEach { p ->
                    ChipFiltro(p, pagina == p, TipoLista.NOTA.colorLista) { pagina = p }
                }
            }
            Text("Etiquetas", fontWeight = FontWeight.SemiBold, color = Color(0xFF3E2723))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { t ->
                    InputChip(
                        selected = true,
                        onClick = { tags.removeAll { it == t } },
                        label = { Text("#$t") },
                        trailingIcon = { Icon(Icons.Default.Close, "Quitar", Modifier.size(16.dp)) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nuevaTag,
                    onValueChange = { nuevaTag = it },
                    placeholder = { Text("nueva etiqueta") },
                    leadingIcon = { Icon(Icons.Default.Tag, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { agregarTag() }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { agregarTag() }) { Icon(Icons.Default.Add, "Agregar etiqueta") }
            }
            Text("Color de la hoja", fontWeight = FontWeight.SemiBold, color = Color(0xFF3E2723))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                COLORES_HOJA.forEach { c ->
                    val sel = (color ?: COLORES_HOJA[0].toArgb()) == c.toArgb()
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c)
                            .border(if (sel) 3.dp else 1.dp, if (sel) TipoLista.NOTA.colorLista else Color.Black.copy(alpha = 0.2f), CircleShape)
                            .clickable { color = c.toArgb() }
                    )
                }
            }
            Button(
                onClick = {
                    if (texto.isNotBlank()) {
                        val base = existente ?: ItemLista(lista = TipoLista.NOTA, nombre = texto, monto = null, orden = -1)
                        onGuardar(
                            base.copy(
                                nombre = texto.trim(),
                                pagina = pagina.takeIf { it != "General" },
                                etiquetas = tags.joinToString(",").ifEmpty { null },
                                recordatorio = recordatorio,
                                fijada = fijada,
                                color = color
                            )
                        )
                        onCerrar()
                    }
                },
                enabled = texto.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                Text(if (existente == null) "GUARDAR NOTA" else "ACTUALIZAR", fontWeight = FontWeight.Bold)
            }
            if (existente != null) {
                OutlinedButton(
                    onClick = { onBorrar(existente); onCerrar() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EstadoRojo)
                ) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("ELIMINAR NOTA")
                }
            }
        }
    }
}

@Composable
private fun EditorFinanza(
    existente: ItemLista?,
    tipo: TipoLista,
    onGuardar: (ItemLista) -> Unit,
    onBorrar: (ItemLista) -> Unit,
    onCerrar: () -> Unit
) {
    val hoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var monto by remember { mutableStateOf(Dinero.editable(existente?.monto)) }
    var pagado by remember { mutableStateOf(Dinero.editable(existente?.pagado)) }
    var fecha by remember { mutableStateOf(existente?.fecha ?: if (existente == null && tipo == TipoLista.GASTO_EXTRA) Fechas.hoy() else null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = hoja) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(tipo.colorLista), contentAlignment = Alignment.Center) {
                    Icon(tipo.icono, null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "${if (existente == null) "Agregar a" else "Editar en"} ${tipo.etiqueta}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = tipo.colorLista
                )
            }
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; error = null },
                label = { Text(tipo.etiquetaNombre) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            if (tipo == TipoLista.AHORRO || tipo == TipoLista.DIEZMO) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..12).forEach { m ->
                        ChipFiltro(Meses.nombre(m).take(3), nombre == Meses.nombre(m), tipo.colorLista) { nombre = Meses.nombre(m) }
                    }
                }
            }
            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it; error = null },
                label = { Text(if (tipo == TipoLista.DEUDA) "Monto total de la deuda" else "Monto") },
                prefix = { Text("C$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            if (tipo == TipoLista.DEUDA) {
                OutlinedTextField(
                    value = pagado,
                    onValueChange = { pagado = it; error = null },
                    label = { Text("Ya pagado / abonado") },
                    prefix = { Text("C$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (tipo == TipoLista.GASTO_EXTRA) {
                CampoFecha("Fecha del gasto", fecha, { fecha = it }, Modifier.fillMaxWidth(), permitirQuitar = true)
            }
            error?.let { Text(it, color = EstadoRojo, fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = {
                    val m = if (monto.isBlank()) null else Dinero.parsear(monto)
                    val p = if (pagado.isBlank()) null else Dinero.parsear(pagado)
                    when {
                        nombre.isBlank() -> error = "Escribe ${tipo.etiquetaNombre.lowercase()}"
                        monto.isNotBlank() && m == null -> error = "El monto no es un número válido"
                        pagado.isNotBlank() && p == null -> error = "Lo pagado no es un número válido"
                        else -> {
                            val base = existente ?: ItemLista(lista = tipo, nombre = nombre, monto = null, orden = Int.MAX_VALUE / 2)
                            onGuardar(base.copy(nombre = nombre.trim(), monto = m, pagado = if (tipo == TipoLista.DEUDA) p else base.pagado, fecha = fecha))
                            onCerrar()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                Text(if (existente == null) "GUARDAR" else "ACTUALIZAR", fontWeight = FontWeight.Bold)
            }
            if (existente != null) {
                OutlinedButton(
                    onClick = { confirmar = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EstadoRojo)
                ) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("ELIMINAR")
                }
            }
        }
    }
    if (confirmar && existente != null) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Eliminar?") },
            text = { Text("Se eliminará \"${existente.nombre}\".") },
            confirmButton = { TextButton(onClick = { confirmar = false; onBorrar(existente); onCerrar() }) { Text("Eliminar", color = EstadoRojo) } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DialogoMonto(titulo: String, detalle: String, onAceptar: (Double) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Default.Payments, null) },
        title = { Text(titulo) },
        text = {
            Column {
                Text(detalle)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = texto, onValueChange = { texto = it; error = false },
                    label = { Text("Monto del abono") }, prefix = { Text("C$ ") }, isError = error, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = Dinero.parsear(texto)
                if (v == null || v <= 0) error = true else { onAceptar(v); onCerrar() }
            }) { Text("Abonar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoLimites(tipo: TipoLista, a: AjustesTraker, onGuardar: (AjustesTraker) -> Unit, onCerrar: () -> Unit) {
    val (titulo, actual) = when (tipo) {
        TipoLista.DEUDA -> "Límite de deuda pendiente" to a.limiteDeudas
        TipoLista.GASTO_EXTRA -> "Límite de gastos extras" to a.limiteExtras
        else -> "Meta de ahorro" to a.metaAhorro
    }
    var texto by remember { mutableStateOf(Dinero.editable(actual)) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Default.Tune, null) },
        title = { Text(titulo) },
        text = {
            Column {
                Text(
                    if (tipo == TipoLista.AHORRO) "Te avisaremos con una notificación cuando la alcances 🎉"
                    else "Te avisaremos con una notificación si lo superas ⚠️",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = texto, onValueChange = { texto = it; error = false },
                    label = { Text("Monto (vacío = sin límite)") }, prefix = { Text("C$ ") }, isError = error, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = if (texto.isBlank()) null else Dinero.parsear(texto)
                if (texto.isNotBlank() && v == null) error = true else {
                    onGuardar(
                        when (tipo) {
                            TipoLista.DEUDA -> a.copy(limiteDeudas = v)
                            TipoLista.GASTO_EXTRA -> a.copy(limiteExtras = v)
                            else -> a.copy(metaAhorro = v)
                        }
                    )
                    onCerrar()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
